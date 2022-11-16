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

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class EncodingUtils {
    
    public static final String UNCHANGED = new String("UNCHANGED");
    
    public static ArrayList<String> processUpdate(byte[] buf, int start, int count) {
        /* parse fields */
        ArrayList<String> values = new ArrayList<String>();
        int fieldStart = start; // index of the separator introducing the next field
//        assert buf[fieldStart] == ','; // tested above
        while (fieldStart < count) {
            
            int fieldEnd = findPipe(buf, fieldStart + 1, count);
            if (fieldEnd == -1) {
                fieldEnd = count;
            }
            /*
              Decoding algorithm:
                  1) Set a pointer to the first field of the schema.
                  2) Look for the next pipe “|” from left to right and take the substring to it, or to the end of the line if no pipe is there.
                  3) Evaluate the substring:
                         A) If its value is empty, the pointed field should be left unchanged and the pointer moved to the next field.
                         B) Otherwise, if its value corresponds to a single “#” (UTF-8 code 0x23), the pointed field should be set to a null value and the pointer moved to the next field.
                         C) Otherwise, If its value corresponds to a single “$” (UTF-8 code 0x24), the pointed field should be set to an empty value (“”) and the pointer moved to the next field.
                         D) Otherwise, if its value begins with a caret “^” (UTF-8 code 0x5E):
                                 - take the substring following the caret and convert it to an integer number;
                                 - for the corresponding count, leave the fields unchanged and move the pointer forward;
                                 - e.g. if the value is “^3”, leave unchanged the pointed field and the following two fields, and move the pointer 3 fields forward;
                         E) Otherwise, the value is an actual content: decode any percent-encoding and set the pointed field to the decoded value, then move the pointer to the next field.
                            Note: “#”, “$” and “^” characters are percent-encoded if occurring at the beginning of an actual content.
                  4) Return to the second step, unless there are no more fields in the schema.
             */
//              String value = message.substring(fieldStart + 1, fieldEnd);
              int firstChar = fieldStart + 1;
              if (firstChar == fieldEnd) { // step A
                  values.add(UNCHANGED);

              } else if (buf[firstChar] == '#') { // step B
//                  if (value.length() != 1) {
//                      onIllegalMessage("Wrong field quoting in message: " + message);
//                  } // a # followed by other text should have been quoted
                  values.add(null);

              } else if (buf[firstChar] == '$') { // step C
//                  if (value.length() != 1) {
//                      onIllegalMessage("Wrong field quoting in message: " + message);
//                  } // a $ followed by other text should have been quoted
                  values.add("");

              } else if (buf[firstChar] == '^') { // step D
//                  int count = myParseInt(value.substring(1), "compression", message);
                  int n = b2int(buf, firstChar + 1, fieldEnd);
                  while (n-- > 0) {
                      values.add(UNCHANGED);
                  }

              } else { // step E
//                  String value = b2str(buf, fieldStart + 1, fieldEnd);
                  String unquoted = EncodingUtils.unquote(buf, firstChar, fieldEnd);
                  values.add(unquoted);
              }
              fieldStart = fieldEnd;
        }
        return values;
    }
    
    public static int findPipe(byte[] buf, int start, int end) {
        for (int i = start; i < end; i++) {
            if (buf[i] == '|') {
                return i;
            }
        }
        return -1;
    }
    
    public static String b2str(byte[] buf, int start, int end) {
        return new String(buf, start, end - start, StandardCharsets.UTF_8);
    }
    
    public static int b2int(byte[] buf, int start, int end) {
        return Integer.parseInt(b2str(buf, start, end));
    }
    
    public static long b2long(byte[] buf, int start, int end) {
        return Long.parseLong(b2str(buf, start, end));
    }
    
    /**
     * Converts a string containing sequences as {@code %<hex digit><hex digit>} into a new string 
     * where such sequences are transformed in UTF-8 encoded characters. <br> 
     * For example the string "a%C3%A8" is converted to "aè" because the sequence 'C3 A8' is 
     * the UTF-8 encoding of the character 'è'.
     */
    public static String unquote(byte[] bb, int start, int end) {
            // to save space and time the input byte sequence is also used to store the converted byte sequence.
            // this is possible because the length of the converted sequence is equal to or shorter than the original one.
            bb = Arrays.copyOfRange(bb, start, end);
            int i = 0, j = 0;
            while (i < bb.length) {
                assert i >= j;
                if (bb[i] == '%') {
                    int firstHexDigit  = hexToNum(bb[i + 1]);
                    int secondHexDigit = hexToNum(bb[i + 2]);
                    bb[j++] = (byte) ((firstHexDigit << 4) + secondHexDigit); // i.e (firstHexDigit * 16) + secondHexDigit
                    i += 3;
                    
                } else {
                    bb[j++] = bb[i++];
                }
            }
            // j contains the length of the converted string
            String ss = null;
            try {
                ss = new String(bb, 0, j, "UTF-8");
                
            } catch (UnsupportedEncodingException e) {
                Logger.logError(e);
            }
            return ss;
    }
    
    /**
     * Converts an ASCII-encoded hex digit in its numeric value.
     */
    private static int hexToNum(int ascii) {
        assert "0123456789abcdefABCDEF".indexOf(ascii) != -1; // ascii is a hex digit
        int hex;
        // NB ascii characters '0', 'A', 'a' have codes 30, 41 and 61
        if ((hex = ascii - 'a' + 10) > 9) {
            // NB (ascii - 'a' + 10 > 9) <=> (ascii >= 'a')
            // and thus ascii is in the range 'a'..'f' because
            // '0' and 'A' have codes smaller than 'a'
            assert 'a' <= ascii && ascii <= 'f';
            assert 10 <= hex && hex <= 15;
            
        } else if ((hex = ascii - 'A' + 10) > 9) {
            // NB (ascii - 'A' + 10 > 9) <=> (ascii >= 'A')
            // and thus ascii is in the range 'A'..'F' because
            // '0' has a code smaller than 'A' 
            // and the range 'a'..'f' is excluded
            assert 'A' <= ascii && ascii <= 'F';
            assert 10 <= hex && hex <= 15;
            
        } else {
            // NB ascii is in the range '0'..'9'
            // because the ranges 'a'..'f' and 'A'..'F' are excluded
            hex =  ascii - '0';
            assert '0' <= ascii && ascii <= '9';
            assert 0 <= hex && hex <= 9;
        }
        assert 0 <= hex && hex <= 15;
        return hex;
    }
}
