/*
 *  Copyright (c) Lightstreamer Srl
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


package com.lightstreamer.oneway_client.netty;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;

/**
 * Manager using NIO selector to make connections.
 * Should be used only if transport is WebSockets.
 * Should be used only if configuration "ignoreData" is set to true.
 */
public class NioConnectionManager extends ConnectionManager {
    
    static final String handshakeRequest =  "GET /lightstreamer HTTP/1.1\r\n" +
            "Sec-WebSocket-Key: cmi68iokhXbZMOVpoz9gjQ==\r\n" +
            "Connection: Upgrade\r\n" +
            "Upgrade: websocket\r\n" +
            "Sec-WebSocket-Version: 13\r\n" +
            "Sec-WebSocket-Protocol: " + TLCP_VER + ".lightstreamer.com\r\n" +
            "Host: NOT_IMPORTANT\r\n" +
            "\r\n" +
            "\r\n";
    
    final ScheduledThreadPoolExecutor executor;
    final ConcurrentLinkedQueue<SocketChannel>[] pendingChannels;
    final Selector[] selectors;
    final Thread[] workers;
    final ModularCounter selectorCnt;
    
    public NioConnectionManager(int nThreads, int selectorThreads) {
        super(false, -1, true);
        if (selectorThreads <= 0) {
            selectorThreads = Runtime.getRuntime().availableProcessors();
        }
        selectorCnt = new ModularCounter(selectorThreads);
        pendingChannels = new ConcurrentLinkedQueue[selectorThreads];
        selectors = new Selector[selectorThreads];
        workers = new Thread[selectorThreads];
        try {
            for (int i = 0; i < selectorThreads; i++) {
                int selectorId = i;
                ConcurrentLinkedQueue<SocketChannel> pendings = new ConcurrentLinkedQueue<SocketChannel>();
                pendingChannels[i] = pendings;
                Selector selector = Selector.open();
                selectors[i] = selector;
                Thread worker = new Thread() {
                    @Override
                    public void run() {
                        selectorLoop(selectorId, selector, pendings);
                    }
                };
                workers[i] = worker;
                worker.setDaemon(true);
                worker.start();
            }
        } catch (IOException e) {
            Logger.logError(e);
        }
        executor = new ScheduledThreadPoolExecutor(nThreads);
        executor.setKeepAliveTime(1, TimeUnit.MINUTES);
    }
    
    @Override
    protected void doBindWs(Session session) {
        int selectorId = selectorCnt.next();
        executor.schedule(() -> {
            try {
                InetSocketAddress addr = new InetSocketAddress(session.clinkHost, session.clinkPort);
                SocketChannel channel = SocketChannel.open(addr);
                Socket socket = null;
                if (session.ssl) {
                    SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                    socket = factory.createSocket(channel.socket(), session.clinkHost, session.clinkPort, true);
                } else {
                    socket = channel.socket();
                }
                OutputStream writeStream = socket.getOutputStream();
                writeStream.write(handshakeRequest.getBytes());

                executor.schedule(() -> {
                    try {
                        String bind = "bind_session\r\nLS_cause=loop1&LS_session=" + session.id;
                        if (Logger.isDebug()) {
                            Logger.log("Sending NIO(" + selectorId + "): " + addr + " " + bind);
                        }
                        byte[] bindHeader = getHeaderForSingleFrame(bind);
                        writeStream.write(bindHeader);
                        writeStream.write(bind.getBytes());
                        
                        executor.schedule(() -> {
                            try {
                                session.onBound(new NioConnection(selectorId, writeStream, session), "CONNECTED:WS-STREAMING");
                                
                                executor.schedule(() -> {
                                    try {
                                        channel.configureBlocking(false);
                                        pendingChannels[selectorId].add(channel);
                                        selectors[selectorId].wakeup();
                                        
                                    } catch (Exception e) {
                                        session.onConnectionError(e);
                                    }
                                }, 1, TimeUnit.SECONDS);
                                
                            } catch (Exception e) {
                                session.onConnectionError(e);
                            }
                        }, 1, TimeUnit.SECONDS);

                    } catch (Exception e) {
                        session.onConnectionError(e);
                    }
                }, 1, TimeUnit.SECONDS);

            } catch (Exception e) {
                session.onConnectionError(e);
            }
        }, 0, TimeUnit.SECONDS);
    }
    
    public long getPendingTaskCount() {
        return executor.getQueue().size();
    }
    
    byte[] getHeaderForSingleFrame(String payload) {
        int len = payload.length(); 
        if (len <= 125) {
            byte[] header = new byte[6];
            header[0] = (byte) 0x81;
            header[1] = (byte) (0x80 + len);
            // gli altri (mask) sono 0
            return header;
        } else {
            byte[] header = new byte[8];
            header[0] = (byte) 0x81;
            header[1] = (byte) 0xFE;
            header[2] = (byte) (len / 256);
            header[3] = (byte) (len % 256);
            // gli altri (mask) sono 0
            return header;
        }
    }
    
    void selectorLoop(int selectorId, Selector selector, ConcurrentLinkedQueue<SocketChannel> pendingChannels) {
        ByteBuffer buf = ByteBuffer.allocate(10000);
        byte[] bytes = new byte[10000];
        
        while (true) {
            while (! pendingChannels.isEmpty()) {
                SocketChannel newChannel = pendingChannels.poll();
                try {
                    newChannel.register(selector, SelectionKey.OP_READ);
                } catch (ClosedChannelException e) {
                    Logger.logError(e);
                }
            }
            
            try {
                selector.select();
            } catch (IOException e1) {
                Logger.logError(e1);
                break;
            }
            
            Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator(); 
            while (selectedKeys.hasNext()) {
                SelectionKey key = (SelectionKey) selectedKeys.next();
                selectedKeys.remove();
                if (! key.isValid()) {
                    continue;
                }
                if (key.isReadable()) {
                    try {
                        SocketChannel involvedChannel = (SocketChannel) key.channel(); 
                        int outcome = involvedChannel.read(buf);
                        if (outcome == -1) {
                            Stats.socketErrors.increment();
                            key.cancel();
                            synchronized (involvedChannel) {
                                involvedChannel.notify();
                            }
                        }
                    } catch (IOException e) {
                        Logger.logError(e);
                        buf.clear();
                        try {
                            Stats.socketErrors.increment();
                            key.cancel();
                        } catch (Exception e1) {
                            Logger.logError(e1);
                        }
                        continue;
                    }
                    buf.flip();
                    int remaining = buf.remaining();
                    if (remaining > 0) {
                        Stats.bytesRead.add(remaining);
                        if (Logger.isDebug()) {
                            buf.get(bytes, 0, remaining);
                            Logger.log("Receiving NIO(" + selectorId + "): " + new String(bytes, 0, remaining));
                        }
                    }
                    buf.clear();
                }
            }
        }
    }
    
    class NioConnection implements Connection {
        
        final OutputStream writeStream;
        final Session session;
        final int selectorId;

        public NioConnection(int selectorId, OutputStream writeStream, Session session) {
            this.selectorId = selectorId;
            this.writeStream = writeStream;
            this.session = session;
        }

        @Override
        public void sendMessage(String msg) {
            sendFrame(msg);
        }
        
        @Override
        public void sendSubscription(String sub) {
            sendFrame(sub);
            session.onSubscription("1", -1, -1);
        }
        
        void sendFrame(String msg) {
            try {
                if (Logger.isDebug()) {
                    Logger.log("Sending NIO(" + selectorId + "): " + msg);
                }
                byte[] msgHeader = getHeaderForSingleFrame(msg);
                writeStream.write(msgHeader);
                writeStream.write(msg.getBytes());
                
            } catch (IOException e) {
                session.onConnectionError(e);
            }
        }

        @Override
        public void close() {
            try {
                writeStream.close();
            } catch (IOException e) {
                session.onConnectionError(e);
            }
        }

        @Override
        public void speedUpReading() {
            // nothing to do
        }
    }

}
