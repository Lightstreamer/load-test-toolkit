package com.lightstreamer.oneway_client.netty;

import static com.lightstreamer.oneway_client.netty.EncodingUtils.b2int;
import static com.lightstreamer.oneway_client.netty.EncodingUtils.b2long;
import static com.lightstreamer.oneway_client.netty.EncodingUtils.b2str;

import java.util.ArrayList;
import java.util.List;

/**
 * TLCP parser.
 * 
 * @author Alessandro Carioni
 * @since August 2018
 */
class TlcpParser extends LineAssembler {
    
    /**
     * TLCP command handler.
     */
    interface TlcpHandler {
        void onCONOK(String sessionId, long reqLimit, long keepalive, String clink);
        void onCONERR(int code, String error);
        void onLOOP();
        void onSUBOK(String subId, int totalItems, int totalFields);
        void onUpdate(String subId, int item, List<String> values);
        void onREQERR(String reqId, int code, String error);
        void onParseError(Exception e);
    }

    private final TlcpHandler handler;
    
    TlcpParser(TlcpHandler handler) {
        this.handler = handler;
    }
    
    @Override
    protected void message(byte[] buf, int count) {
        try {
            if (count > 0 && buf[0] == 'U') {
                // U,<table>,<item>,<field1>|...|<fieldN>
                int firstComma = 1;
                int secondComma = findComma(buf, firstComma + 1);
                int thirdComma = findComma(buf, secondComma + 1);
                String subId = b2str(buf, firstComma + 1, secondComma);
                int item = b2int(buf, secondComma + 1, thirdComma);
                ArrayList<String> values = EncodingUtils.processUpdate(buf, thirdComma, count);
                handler.onUpdate(subId, item, values);
                
            } else if (count > 4 && buf[0] == 'S' && buf[1] == 'U' && buf[2] == 'B' && buf[3] == 'O' && buf[4] == 'K') {
                // SUBOK,<table>,<total items>,<total fields>
                int firstComma = 5;
                int secondComma = findComma(buf, firstComma + 1);
                int thirdComma = findComma(buf, secondComma + 1);
                String subId = b2str(buf, firstComma + 1, secondComma);
                int totalItems = b2int(buf, secondComma + 1, thirdComma);
                int totalFields = b2int(buf, thirdComma + 1, count);
                handler.onSUBOK(subId, totalItems, totalFields);
                
            } else if (count > 2 && buf[0] == 'C' && buf[1] == 'O' && buf[2] == 'N') {
                if (count > 4 && buf[3] == 'O' && buf[4] == 'K') {
                    // CONOK,<session id>,<request limit>,<keep alive>,<control link>
                    int firstComma = 5;
                    int secondComma = findComma(buf, firstComma + 1);
                    int thirdComma = findComma(buf, secondComma + 1);
                    int fourthComma = findComma(buf, thirdComma + 1);
                    String sessionId = b2str(buf, firstComma + 1, secondComma);
                    long reqLimit = b2long(buf, secondComma + 1, thirdComma);
                    long keepalive = b2long(buf, thirdComma + 1, fourthComma);
                    String clink = (buf[fourthComma + 1] == '*' ? null : b2str(buf, fourthComma + 1, count));
                    handler.onCONOK(sessionId, reqLimit, keepalive, clink);

                } else if (count > 5 && buf[3] == 'E' && buf[4] == 'R' && buf[5] == 'R') {
                    // CONERR,<code>,<error>
                    int secondComma = findComma(buf, 7);
                    int code = b2int(buf, 7, secondComma);
                    String error = b2str(buf, secondComma + 1, count);
                    handler.onCONERR(code, error);
                }
                
            } else if (count > 5 && buf[0] == 'R' && buf[1] == 'E' && buf[2] == 'Q' && buf[3] == 'E' && buf[4] == 'R' && buf[5] == 'R') {
                // REQERR,<reqId>,<code>,<msg>
                int firstComma = 6;
                int secondComma = findComma(buf, firstComma + 1);
                int thirdComma = findComma(buf, secondComma + 1);
                String reqId = b2str(buf, firstComma + 1, secondComma);
                int code = b2int(buf, secondComma + 1, thirdComma);
                String msg = b2str(buf, thirdComma + 1, count);
                handler.onREQERR(reqId, code, msg);
                
            } else if (count > 3 && buf[0] == 'L' && buf[1] == 'O' && buf[2] == 'O' && buf[3] == 'P') {
                // LOOP,<millis>
                handler.onLOOP();
            }
            
        } catch (Exception e) {
            handler.onParseError(e);
        }
    }
    
    int findComma(byte[] buf, int start) {
        for (int i = start, len = buf.length; i < len; i++) {
            if (buf[i] == ',') {
                return i;
            }
        }
        throw new IllegalArgumentException();
    }
}