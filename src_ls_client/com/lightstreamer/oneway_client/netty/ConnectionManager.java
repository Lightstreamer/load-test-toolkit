package com.lightstreamer.oneway_client.netty;

import static com.lightstreamer.oneway_client.netty.Logger.log;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.lightstreamer.oneway_client.Subscription;
import com.lightstreamer.oneway_client.netty.TlcpParser.TlcpHandler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.AbstractChannelPoolMap;
import io.netty.channel.pool.ChannelHealthChecker;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.SimpleChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

/**
 * Manager using Netty to make connections.
 * 
 * @author Alessandro Carioni
 * @since August 2018
 */
public class ConnectionManager {
    
    protected static final String TLCP_VER = "TLCP-2.0.0";
    
    private final AtomicInteger nextReqId = new AtomicInteger();
    private final NioEventLoopGroup group;
    private final AbstractChannelPoolMap<MyInetSocketAddress, SimpleChannelPool> httpPoolMap;
    private final boolean ignoreData;

    /**
     * Creates a connection manager.
     * 
     * @param ignoreData when true, the manager ignores data received on the stream connection
     * @param nThreads number of threads allocated to Netty
     * @param {@code true} sockets selection will be LIFO, if {@code false} FIFO
     */
    public ConnectionManager(boolean ignoreData, int nThreads, boolean lastRecentUsed) {
        this.ignoreData = ignoreData;
        if (nThreads > 0) {
            this.group = new NioEventLoopGroup(nThreads);
        } else {            
            this.group = new NioEventLoopGroup();
        }
        
        final Bootstrap httpBs = new Bootstrap();
        httpBs.group(group)
        .channel(NioSocketChannel.class);
        
        this.httpPoolMap = new AbstractChannelPoolMap<MyInetSocketAddress, SimpleChannelPool>() {
            @Override
            protected SimpleChannelPool newPool(final MyInetSocketAddress key) {
                return new SimpleChannelPool(
                        httpBs.remoteAddress(key.address), 
                        getChannelPoolHandler(key),
                        ChannelHealthChecker.ACTIVE,
                        true /*check channel health before offering back*/,
                        lastRecentUsed);
            }
        };
    }
    
    /**
     * Sends create_session requests.
     */
    public void connect(final Session session) {
        try {
            doConnect(session);
            
        } catch (Throwable e) {
            session.onConnectionError(e);
        }
    }
    
    private void doConnect(Session session) {
        final String path = "/lightstreamer/create_session.txt?LS_protocol=" +  TLCP_VER;
        final String postMsg = "LS_polling=true"
                + "&LS_cause=new.api"
                + "&LS_polling_millis=0"
                + "&LS_idle_millis=0"
                + "&LS_cid=mgQkwtwdysogQz2BJ4Ji%20kOj2Bg"
                + "&LS_adapter_set=" + session.adapterSet
                + (session.client.connectionDetails.user != null ? "&LS_user=" + session.client.connectionDetails.user : "")
                + (session.client.connectionDetails.password != null ? "&LS_password=" + session.client.connectionDetails.password : "")
                + "\r\n";
        sendHttpRequest(session, session.host, session.port, path, postMsg, CreateHandler::new);
    }
    
    /**
     * Sends bind_session request.
     */
    public void bind(boolean http, Session session) {
        try {
            if (http) {
                doBindHttp(session);
            } else {
                doBindWs(session);
            }
        } catch (Throwable e) {
            session.onConnectionError(e);
        }
    }
    
    /**
     * Sends bind_session request using HTTP.
     */
    protected void doBindHttp(Session session) {
        final String path = "/lightstreamer/bind_session.txt?LS_protocol=" + TLCP_VER;
        final String postMsg = "LS_cause=loop1&LS_session=" + session.id + "&\r\n";
        
        String host = session.clinkHost;
        int port = session.clinkPort;
        
        String protocol = (session.ssl ? "https://" : "http://");
        final URI uri = LsUtils.uri(protocol + host + ":" + port + path);
        
        final FullHttpRequest httpRequest = buildHttpRequest(host, port, path, postMsg, session, uri);
        
        MyInetSocketAddress key = new MyInetSocketAddress(host, port, session.ssl, session.instanceId);
        final SimpleChannelPool chPool = httpPoolMap.get(key);
        Future<Channel> fc1 = chPool.acquire();
        fc1.addListener(new FutureListener<Channel>() {
            @Override
            public void operationComplete(Future<Channel> f1) throws Exception {
                if (f1.isSuccess()) {
                    final Channel ch1 = f1.getNow();
                    ChannelPipeline pipeline = ch1.pipeline();
                    
                    ChannelHandler r = pipeline.get("reader");
                    if (r != null) {
                        pipeline.remove("reader");
                    }
                    if (ignoreData) {
                        pipeline.addLast("reader", new ByteCountHandler());
                        
                    } else {                        
                        pipeline.addLast("reader", new BindHttpHandler(session, ch1, chPool, uri));
                    }
                    
                    if (Logger.isDebug()) {
                        Logger.log("Sending HTTP: " + ch1 + " " + uri + " " + postMsg);
                    }
                    ChannelFuture fc2 = ch1.writeAndFlush(httpRequest);
                    fc2.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture f2) throws Exception {
                            if (f2.isSuccess()) {
                                if (ignoreData) {
                                    
                                    ChannelHandler http = pipeline.get("http");
                                    if (http != null) {
                                        pipeline.remove(http);
                                    }
                                    
                                    ChannelHandler ssl = pipeline.get("ssl");
                                    if (ssl != null) {
                                        pipeline.remove(ssl);
                                    }
                                    
                                    session.onBound(new HttpConnection(ch1, session, chPool), "CONNECTED:HTTP-STREAMING");
                                }
                                
                            } else {
                                // write error
                                closeAndRelease(ch1, chPool);
                                session.onConnectionError(f2.cause());
                            }
                        }
                    });
                    
                } else {
                    // socket creation error
                    session.onConnectionError(f1.cause());
                }
            }
        });
    }
    
    /**
     * Sends bind_session request using HTTP.
     */
    protected void doBindWs(Session session) {
        String clink = session.clinkHost;
        int port = session.clinkPort;
        String protocol = (session.ssl ? "wss://" : "ws://");
        final String subprotocol = TLCP_VER + ".lightstreamer.com";
        final URI uri = LsUtils.uri(protocol + clink + ":" + port + "/lightstreamer");

        MyInetSocketAddress inetAddress = new MyInetSocketAddress(clink, port, session.ssl, session.instanceId);
        final SimpleChannelPool chPool = httpPoolMap.get(inetAddress);
        Future<Channel> fc1 = chPool.acquire();
        fc1.addListener(new FutureListener<Channel>() {
            @Override
            public void operationComplete(Future<Channel> f1) throws Exception {
                if (f1.isSuccess()) {
                    final Channel ch1 = f1.getNow();
                    ChannelPipeline pipeline = ch1.pipeline();
                    
                    ChannelPromise handshakerPromise = ch1.newPromise();
                    DefaultHttpHeaders customHeaders = new DefaultHttpHeaders();
                    /* send cookies */
                    String cookies = session.getLocalCookieHelper().getCookieHeader(uri);
                    if (cookies != null && cookies.length() > 0) {
                        customHeaders.set(HttpHeaderNames.COOKIE,cookies);
                    }
                    WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13, subprotocol, true, customHeaders);
                    
                    ChannelHandler handler = pipeline.get("reader");
                    if (handler != null) {
                        pipeline.remove("reader"); // remove http handler
                    }
                    pipeline.addLast(new HttpObjectAggregator(8192));
                    pipeline.addLast(WebSocketClientCompressionHandler.INSTANCE);
                    pipeline.addLast("reader", new BindWsHandler(handshaker, handshakerPromise, session, ch1, chPool));
                    
                    handshakerPromise.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture f2) throws Exception {
                            if (f2.isSuccess()) {
                                
                                if (ignoreData) {
                                    /* Don't remove http handler or Netty will raise an exception.
                                     * The handler will be removed later by WebSocketClientHandshaker.
                                    pipeline.remove("http");
                                    */
                                    pipeline.remove("ws-decoder");
                                    // don't parse TLCP commands but only count the bytes received
                                    pipeline.replace("reader", "reader", new ByteCountHandler());
                                }
                                
                                final Channel ch2 = f2.channel();
                                String bindMsg = "bind_session\r\nLS_cause=loop1&LS_session=" + session.id;
                                if (Logger.isDebug()) {
                                    Logger.log("Sending WS: " + ch2 + " " + bindMsg);
                                }
                                ChannelFuture fc3 = ch2.writeAndFlush(new TextWebSocketFrame(bindMsg));
                                fc3.addListener(new ChannelFutureListener() {
                                    @Override
                                    public void operationComplete(ChannelFuture f3) throws Exception {
                                        if (f3.isSuccess()) {
                                            if (ignoreData) {
                                                session.onBound(new WsConnection(ch2, session, chPool), "CONNECTED:WS-STREAMING");
                                            }
                                            
                                        } else {                                            
                                            closeAndRelease(ch2, chPool);
                                            session.onConnectionError(f3.cause());
                                        }
                                    }
                                });
                                
                            } else {
                                // ws handshake error
                                closeAndRelease(ch1, chPool);
                                session.onConnectionError(f2.cause());
                            }
                        }
                    });
                    
                } else {
                    // socket creation error
                    session.onConnectionError(f1.cause());
                }
            }
        });
    }
    
    /**
     * Sends subscription request.
     */
    public void subscribe(Connection ch, Subscription sub, String subId, Session session) {
        String params = "LS_reqId=" + nextReqId.incrementAndGet()
            + "&LS_op=add"
            + "&LS_subId=" + subId
            + "&LS_mode=" + sub.getMode()
            + "&LS_group=" + sub.getItemDesc().names
            + "&LS_schema=" + sub.getFieldDesc().names
            + (sub.getDataAdapter() != null ? "&LS_data_adapter=" + sub.getDataAdapter() : "")
            + (sub.getRequestedSnapshot() != null ? "&LS_snapshot=" + (sub.getRequestedSnapshot().equals("yes") ? "true" : "false" ) : "")
            + "&LS_session=" + session.id;
        ch.sendSubscription(params);
    }
    
    /**
     * Sends message request.
     */
    public void sendMessage(Connection ch, String message, Session session) {
        String params = "LS_reqId=" + nextReqId.incrementAndGet()
                + "&LS_message=" + message
                + "&LS_outcome=false&LS_ack=false"
                + "&LS_session=" + session.id;
        ch.sendMessage(params);
    }
    
    private void sendHttpRequest(Session session, String host, int port, String path, String postMsg, ReadHandlerFactory factory) {
        String protocol = (session.ssl ? "https://" : "http://");
        final URI uri = LsUtils.uri(protocol + host + ":" + port + path);
        
        final FullHttpRequest httpRequest = buildHttpRequest(host, port, path, postMsg, session, uri);
        
        MyInetSocketAddress key = new MyInetSocketAddress(host, port, session.ssl, session.instanceId);
        final SimpleChannelPool chPool = httpPoolMap.get(key);
        Future<Channel> fc1 = chPool.acquire();
        fc1.addListener(new FutureListener<Channel>() {
            @Override
            public void operationComplete(Future<Channel> f1) throws Exception {
                if (f1.isSuccess()) {
                    final Channel ch1 = f1.getNow();
                    ChannelPipeline pipeline = ch1.pipeline();
                    
                    ChannelHandler r = pipeline.get("reader");
                    if (r != null) {
                        pipeline.remove("reader");
                    }
                    pipeline.addLast("reader", factory.newInstance(session, ch1, chPool, uri));
                    
                    if (Logger.isDebug()) {
                        Logger.log("Sending HTTP: " + ch1 + " " + uri + " " + postMsg);
                    }
                    ChannelFuture fc2 = ch1.writeAndFlush(httpRequest);
                    fc2.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture f2) throws Exception {
                            if (! f2.isSuccess()) {
                                // write error
                                closeAndRelease(ch1, chPool);
                                session.onConnectionError(f2.cause());
                            }
                        }
                    });
                    
                } else {
                    // socket creation error
                    session.onConnectionError(f1.cause());
                }
            }
        });
    }

    private FullHttpRequest buildHttpRequest(String host, int port, String path, String postMsg, Session session, final URI uri) {
        final FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, path);
        httpRequest.headers().set("Host", host  + ":" + port);
        httpRequest.headers().set(HttpHeaderNames.USER_AGENT, "Lightstreamer-client-simulator");
        httpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        /* send cookies */
        String cookies = session.getLocalCookieHelper().getCookieHeader(uri);
        if (cookies != null && cookies.length() > 0) {
          httpRequest.headers().set(HttpHeaderNames.COOKIE,cookies);
        }
        ByteBuf bbuf = Unpooled.copiedBuffer(postMsg, StandardCharsets.UTF_8);
        httpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, bbuf.readableBytes());
        httpRequest.content().clear().writeBytes(bbuf);
        return httpRequest;
    }
    
    protected ChannelPoolHandler getChannelPoolHandler(final MyInetSocketAddress key) {
        return new ChannelPoolHandler() {
            
            @Override
            public void channelReleased(Channel ch) throws Exception {
                if (Logger.isDebug()) {
                    log("channelReleased " + ch);
                }
            }
            
            @Override
            public void channelCreated(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                if (key.ssl) {
                    SslContextBuilder builder = SslContextBuilder.forClient();
                    builder.sslProvider(SslProvider.JDK);
                    SslContext sslCtx = builder.build();
                    ch.pipeline().addLast("ssl", 
                            sslCtx.newHandler(ch.alloc(), key.address.getHostString(), key.address.getPort()));
                }
                pipeline.addLast("http", new HttpClientCodec());

                if (Logger.isDebug()) {
                    log("channelCreated " + ch);
                }
            }
            
            @Override
            public void channelAcquired(Channel ch) throws Exception {
                if (Logger.isDebug()) {                            
                    log("channelAcquired " + ch);
                }
            }
        };
    }
    
    private void closeAndRelease(Channel ch, SimpleChannelPool chPool) {
        ch.close();
        chPool.release(ch);
        if (Logger.isDebug()) {                            
            log("channelClosed " + ch);
        }
    }

    /**
     * Factory returning TLCP message handler.
     */
    @FunctionalInterface
    interface ReadHandlerFactory {
        ReadHandler newInstance(Session session, Channel ch, SimpleChannelPool chPool, URI uri);
    }
    
    /**
     * Generic TLCP message handler.
     */
    private abstract class ReadHandler extends SimpleChannelInboundHandler<Object> implements TlcpHandler {
        
        final TlcpParser parser = new TlcpParser(this);
        final Session session;
        final Channel ch;
        final SimpleChannelPool chPool;
        final URI uri;
        
        public ReadHandler(Session session, Channel ch, SimpleChannelPool chPool, URI uri) {
            this.session = session;
            this.ch = ch;
            this.chPool = chPool;
            this.uri = uri;
        }

        @Override
        public void onCONOK(String sessionId, long reqLimit, long keepalive, String clink) {
            fail();
        }

        @Override
        public void onCONERR(int code, String error) {
            fail();
        }

        @Override
        public void onLOOP() {
            fail();
        }
        
        @Override
        public void onSUBOK(String subId, int totalItems, int totalFields) {
            fail();
        }
        
        @Override
        public void onUpdate(String subId, int item, List<String> values) {
            fail();
        }
        
        @Override
        public void onREQERR(String reqId, int code, String error) {
            closeAndRelease(ch, chPool);
            session.onSessionError(code, error);
        }
        
        @Override
        public void onParseError(Exception e) {
            closeAndRelease(ch, chPool);
            session.onSessionError(e);
        }
        
        void fail() {
            closeAndRelease(ch, chPool);
            session.onSessionError(-1, "Message not expected");
        }
    }
    
    /**
     * Generic TLCP handler for HTTP connection.
     */
    private class ReadHttpHandler extends ReadHandler {

        public ReadHttpHandler(Session session, Channel ch, SimpleChannelPool chPool, URI uri) {
            super(session, ch, chPool, uri);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            Channel ch = ctx.channel();
            if (msg instanceof HttpResponse) {
                HttpResponse response = (HttpResponse) msg;
                int respCode = response.status().code();
                if (respCode != 200) {
                    closeAndRelease(ch, chPool);
                    session.onConnectionError(new RuntimeException("HTTP status: " + respCode));
                    return;
                }
                /* save cookies */
                for (String cookie : response.headers().getAll(HttpHeaderNames.SET_COOKIE)) {
                    session.getLocalCookieHelper().saveCookies(uri, cookie);
                }
                
            } else if (msg instanceof HttpContent) {
                HttpContent chunk = (HttpContent) msg;
                ByteBuf buf = chunk.content();
                try {
                    if (Logger.isDebug()) {
                        Logger.log("Receiving: " + ch + " " + buf.toString(CharsetUtil.US_ASCII));
                    }
                    parser.readBytes(buf);
                    if (chunk instanceof LastHttpContent) {
                        chPool.release(ch);
                    }
                    
                } catch (Throwable e) {
                    closeAndRelease(ch, chPool);
                    session.onConnectionError(e);
                }
            }
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            closeAndRelease(ctx.channel(), chPool);
            session.onConnectionError(cause);
        }
    }
    
    /**
     * TLCP handler for HTTP create_session responses.
     */
    private class CreateHandler extends ReadHttpHandler {
        
        public CreateHandler(Session session, Channel ch, SimpleChannelPool chPool, URI uri) {
            super(session, ch, chPool, uri);
        }

        @Override
        public void onCONOK(String sessionId, long reqLimit, long keepalive, String clink) {
            session.onConnected(sessionId, clink);
        }
        
        @Override
        public void onCONERR(int code, String error) {
            session.onSessionError(code, error);
        }
        
        @Override
        public void onLOOP() {
            session.bind();
        }
    }
    
    /**
     * TLCP handler for HTTP bind_session responses.
     */
    private class BindHttpHandler extends ReadHttpHandler {
        
        public BindHttpHandler(Session session, Channel ch, SimpleChannelPool chPool, URI uri) {
            super(session, ch, chPool, uri);
        }

        @Override
        public void onCONOK(String sessionId, long reqLimit, long keepalive, String clink) {
            session.onBound(new HttpConnection(ch, session, chPool), "CONNECTED:HTTP-STREAMING");
        }
        
        @Override
        public void onCONERR(int code, String error) {
            closeAndRelease(ch, chPool);
            session.onSessionError(code, error);
        }
        
        @Override
        public void onSUBOK(String subId, int totalItems, int totalFields) {
            session.onSubscription(subId, totalItems, totalFields);
        }
        
        @Override
        public void onUpdate(String subId, int item, List<String> values) {
            session.onUpdate(subId, item, values);
        }
    }
    
    /**
     * TLCP handler for Websockets bindd_session responses.
     */
    private class BindWsHandler extends ReadHandler {
        
        private final WebSocketClientHandshaker handshaker;
        private final ChannelPromise handshakerPromise;
        
        public BindWsHandler(WebSocketClientHandshaker handshaker, ChannelPromise handshakerPromise, Session session, Channel ch, SimpleChannelPool chPool) {
            super(session, ch, chPool, handshaker.uri());
            this.handshaker = handshaker;
            this.handshakerPromise = handshakerPromise;
        }
        
        @Override
        public void onCONOK(String sessionId, long reqLimit, long keepalive, String clink) {
            session.onBound(new WsConnection(ch, session, chPool), "CONNECTED:WS-STREAMING");
        }
        
        @Override
        public void onCONERR(int code, String error) {
            closeAndRelease(ch, chPool);
            session.onSessionError(code, error);
        }
        
        @Override
        public void onREQERR(String reqId, int code, String error) {
            closeAndRelease(ch, chPool);
            session.onSessionError(code, error);
        }
        
        @Override
        public void onSUBOK(String subId, int totalItems, int totalFields) {
            session.onSubscription(subId, totalItems, totalFields);
        }
        
        @Override
        public void onUpdate(String subId, int item, List<String> values) {
            session.onUpdate(subId, item, values);
        }
        
        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            /* channel is already active */
            handshaker.handshake(ctx.channel());
        }

//        @Override
//        public void channelActive(ChannelHandlerContext ctx) {
//            handshaker.handshake(ctx.channel());
//        }
        
//        @Override
//        public void channelInactive(ChannelHandlerContext ctx) {
//            ctx.fireChannelInactive();
//        }
        
        @Override
        public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            Channel ch = ctx.channel();
            if (! handshaker.isHandshakeComplete()) {
                try {
                    FullHttpResponse resp = (FullHttpResponse) msg;
                    handshaker.finishHandshake(ch, resp);
                    handshakerPromise.setSuccess();
                    /* save cookies */
                    for (String cookie : resp.headers().getAll(HttpHeaderNames.SET_COOKIE)) {
                        session.getLocalCookieHelper().saveCookies(handshaker.uri(), cookie);
                    }
                    
                } catch (Throwable e) {
                    handshakerPromise.setFailure(e);
                }
                return;
            }

            WebSocketFrame frame = (WebSocketFrame) msg;
            if (frame instanceof TextWebSocketFrame) {
                try {
                    if (Logger.isDebug()) {
                        TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
                        Logger.log("Receiving: " + ch + " " + textFrame.text());
                    }
                    parser.readBytes(frame.content());
                    
                } catch (Throwable e) {
                    closeAndRelease(ch, chPool);
                    session.onConnectionError(e);
                }
                
            } else if (frame instanceof CloseWebSocketFrame) {
                closeAndRelease(ch, chPool);
            }
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (! handshakerPromise.isDone()) {
                handshakerPromise.setFailure(cause);
                
            } else {
                closeAndRelease(ctx.channel(), chPool);
                session.onConnectionError(cause);
            }
        }
    }
    
    /**
     * Simple handler which only counts incoming bytes.
     */
    private static class ByteCountHandler extends SimpleChannelInboundHandler<Object> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof ByteBuf) {
                ByteBuf buf = (ByteBuf) msg;
                Stats.bytesRead.add(buf.readableBytes());
            }
        }
    }
    
    abstract class BaseConnection implements Connection {
        
        final Channel ch;
        final Session session;
        final SimpleChannelPool chPool;
        
        public BaseConnection(Channel ch, Session session, SimpleChannelPool chPool) {
            this.ch = ch;
            this.session = session;
            this.chPool = chPool;
        }

        @Override
        public void close() {
            closeAndRelease(ch, chPool);
        }
    }
    
    class WsConnection extends BaseConnection {

        public WsConnection(Channel ch, Session session, SimpleChannelPool chPool) {
            super(ch, session, chPool);
        }
        
        @Override
        public void sendMessage(String params) {
            String msg = "msg\r\n" + params;
            if (Logger.isDebug()) {
                Logger.log("Sending WS: " + ch + " " + msg);
            }
            ChannelFuture fc = ch.writeAndFlush(new TextWebSocketFrame(msg));
            fc.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture f) throws Exception {
                    if (! f.isSuccess()) {
                        closeAndRelease(ch, chPool);
                        session.onConnectionError(f.cause());
                    }
                }
            });
        }
        
        @Override
        public void sendSubscription(String params) {
            String msg = "control\r\n" + params;
            if (Logger.isDebug()) {
                Logger.log("Sending WS: " + ch + " " + msg);
            }
            ChannelFuture fc = ch.writeAndFlush(new TextWebSocketFrame(msg));
            fc.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture f) throws Exception {
                    if (f.isSuccess()) {
                        if (ignoreData) {                            
                            session.onSubscription("1", -1, -1);
                        }
                        
                    } else {
                        closeAndRelease(ch, chPool);
                        session.onConnectionError(f.cause());                        
                    }
                }
            });
        }
        
        @Override
        public void speedUpReading() {
            if (ignoreData) {
                // NB in WebSockets ssl codec can be removed only when we are sure that the client doesn't have any more requests to send 
                ChannelPipeline pipeline = ch.pipeline();
                ChannelHandler ssl = pipeline.get("ssl");
                if (ssl != null) {
                    pipeline.remove(ssl);
                }
            }
        }
    }
    
    class HttpConnection extends BaseConnection {

        public HttpConnection(Channel ch, Session session, SimpleChannelPool chPool) {
            super(ch, session, chPool);
        }
        
        @Override
        public void sendMessage(String params) {
            String path = "/lightstreamer/msg.txt?LS_protocol=" + TLCP_VER;
            sendHttpRequest(session, session.clinkHost, session.clinkPort, path, params, ReadHttpHandler::new);
        }
        
        @Override
        public void sendSubscription(String params) {
            String path = "/lightstreamer/control.txt?LS_protocol=" +  TLCP_VER;
            
            String host = session.clinkHost;
            int port = session.clinkPort;
            String postMsg = params;

            String protocol = (session.ssl ? "https://" : "http://");
            final URI uri = LsUtils.uri(protocol + host + ":" + port + path);
            
            final FullHttpRequest httpRequest = buildHttpRequest(host, port, path, postMsg, session, uri);
            
            MyInetSocketAddress key = new MyInetSocketAddress(host, port, session.ssl, session.instanceId);
            final SimpleChannelPool chPool = httpPoolMap.get(key);
            Future<Channel> fc1 = chPool.acquire();
            fc1.addListener(new FutureListener<Channel>() {
                @Override
                public void operationComplete(Future<Channel> f1) throws Exception {
                    if (f1.isSuccess()) {
                        final Channel ch1 = f1.getNow();
                        ChannelPipeline pipeline = ch1.pipeline();
                        
                        ChannelHandler r = pipeline.get("reader");
                        if (r != null) {
                            pipeline.remove("reader");
                        }
                        pipeline.addLast("reader", new ReadHttpHandler(session, ch1, chPool, uri));
                        
                        if (Logger.isDebug()) {
                            Logger.log("Sending HTTP: " + ch1 + " " + uri + " " + postMsg);
                        }
                        ChannelFuture fc2 = ch1.writeAndFlush(httpRequest);
                        fc2.addListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture f2) throws Exception {
                                if (f2.isSuccess()) {
                                    if (ignoreData) {                            
                                        session.onSubscription("1", -1, -1);
                                    }
                                    
                                } else {
                                    // write error
                                    closeAndRelease(ch1, chPool);
                                    session.onConnectionError(f2.cause());                                    
                                }
                            }
                        });
                        
                    } else {
                        // socket creation error
                        session.onConnectionError(f1.cause());
                    }
                }
            });
        }
        
        @Override
        public void speedUpReading() {
            // nothing to do: http and ssl codecs have been removed after the bind_session
        }
    }

}
