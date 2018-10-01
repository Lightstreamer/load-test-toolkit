package com.lightstreamer.oneway_client.netty;

import java.io.IOException;

import io.netty.buffer.ByteBuf;

/**
 * Extracts the lines from a byte buffer.
 * 
 * @author Alessandro Carioni
 * @since January 2017
 */
public abstract class LineAssembler {
    
    private final PeekableByteArrayOutputStream linePart;
    
    private static final byte LF = '\n';
    private static final byte CR = '\r';

    public LineAssembler() {
        linePart = new PeekableByteArrayOutputStream();
    }
    
    abstract protected void message(byte buf[], int count);
    
    /**
     * Reads the available bytes and extracts the contained lines. 
     * For each line found the method {@link RequestListener#onMessage(String)} is notified.
     */
    public void readBytes(ByteBuf buf) {
        /*
         * A frame has the following structure:
         * <frame> ::= <head><body><tail>
         * 
         * The head of a frame (if present) is the rest of a line started in a previous frame.
         * <head> ::= <rest-previous-line>?
         * <rest-previous-line> ::= <line-part><LF> 
         * NB line-part can be empty. In that case the char CR is in the previous frame.
         * 
         * The body consists of a sequence of whole lines.
         * <body> ::= <line>*
         * <line> ::= <line-body><EOL>
         * 
         * The tail of a frame (if present) is a line lacking the EOL terminator (NB it can span more than one frame).
         * <tail> ::= <line-part>?
         * 
         * EOL is the sequence \r\n.
         * <EOL> ::= <CR><LF>
         * 
         */
        /*
         * NB 
         * startIndex and eolIndex are the most important variables (and the only non-final)
         * and they must be updated together since they represents the next part of frame to elaborate. 
         */
        final int endIndex = buf.readerIndex() + buf.readableBytes(); // ending index of the byte buffer (exclusive)
        int startIndex = buf.readerIndex(); // starting index of the current line/part of line (inclusive)
        int eolIndex; // ending index of the current line/part of line (inclusive) (it points to EOL)
        if (startIndex >= endIndex) {
            return; // byte buffer is empty: nothing to do 
        }
        /* head */
        final boolean hasHead;
        final boolean prevLineIsIncomplete = linePart.size() != 0;
        if (prevLineIsIncomplete) {
            /* 
             * Since the previous line is incomplete (it lacks the line terminator), 
             * is the rest of the line in this frame?
             * We have three cases:
             * A) the char CR is in the previous frame and the char LF is in this one;
             * B) the chars CR and LF are in this frame;
             * C) the sequence CR LF is not in this frame (maybe there is CR but not LF).
             * 
             * If case A) or B) holds, the next part to compute is <head> (see grammar above).
             * In case C) we must compute <tail>.
             */
            if (linePart.peekAtLastByte() == CR && buf.getByte(startIndex) == LF) {
                // case A) EOL is across the previous and the current frame
                hasHead = true;
                eolIndex = startIndex;
            } else {
                eolIndex = findEol(buf, startIndex, endIndex);
                if (eolIndex != -1) {
                    // case B)
                    hasHead = true;
                } else {
                    // case C)
                    hasHead = false;
                }
            }
            
        } else {
            /* 
             * The previous line is complete.
             * We must consider two cases:
             * D) the sequence CR LF is in this frame;
             * E) the sequence CR LF is not in this frame (maybe there is CR but not LF).
             * 
             * If case D) holds, the next part to compute is <body>.
             * If case E) holds, the next part is <tail>.
             */
            hasHead = false;
            eolIndex = findEol(buf, startIndex, endIndex);
        }
        if (hasHead) {
            copyLinePart(buf, startIndex, eolIndex + 1);
            message(linePart.buf, linePart.count - 2); // exclude CR LF chars
            linePart.reset();
            
            startIndex = eolIndex + 1;
            eolIndex = findEol(buf, startIndex, endIndex);
        }
        /* body */
        while (eolIndex != -1) {
            copyLinePart(buf, startIndex, eolIndex + 1);
            message(linePart.buf, linePart.count - 2); // exclude CR LF chars
            linePart.reset();
            
            startIndex = eolIndex + 1;
            eolIndex = findEol(buf, startIndex, endIndex);
        }
        /* tail */
        final boolean hasTail = startIndex != endIndex;
        if (hasTail) {
            copyLinePart(buf, startIndex, endIndex);
        }
    }
    
    /**
     * Finds the index of a CR LF sequence (EOL). The index points to LF.
     * Returns -1 if there is no EOL.
     * @param startIndex starting index (inclusive)
     * @param endIndex ending index (exclusive)
     */
    private int findEol(ByteBuf buf, int startIndex, int endIndex) {
        int eolIndex = -1;
        if (startIndex >= endIndex) {
            return eolIndex;
        }
        int crIndex = buf.indexOf(startIndex, endIndex, CR);
        if (crIndex != -1 
                && crIndex != endIndex - 1 // CR it is not the last byte
                && buf.getByte(crIndex + 1) == LF) {
            eolIndex = crIndex + 1;
        }
        return eolIndex;
    }
    
    /**
     * Copies a slice of a frame representing a part of a bigger string in a temporary buffer to be reassembled.
     * @param startIndex starting index (inclusive)
     * @param endIndex ending index (exclusive)
     */
    private void copyLinePart(ByteBuf buf, int startIndex, int endIndex) {
        try {
            buf.getBytes(startIndex, linePart, endIndex - startIndex);
        } catch (IOException e) {
            Logger.logError(e);
        }
    }
    
}