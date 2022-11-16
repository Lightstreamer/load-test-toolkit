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


package com.lightstreamer.load_test.commons;

/**
 * Utility class used to convert a time, in milliseconds, to a byte array of hexadecimal digits (and vice versa).
 */
public class TimeConversion {

    private static final byte[] DEC_DIGITS = {'0','1','2','3','4','5','6','7','8','9'};
    
    private static byte FIRST;
    private static byte SECOND;
    //private static byte THIRD;
    private static int MAX_FIXED_INDEX;
    static {
        String date = String.valueOf(System.currentTimeMillis());
        FIRST = date.getBytes()[0];
        //the second digit changes any ~3 years; I doubt we'll have a so long test :)
        SECOND = date.getBytes()[1];
        MAX_FIXED_INDEX=1;
        
        //the third digit changes any ~115 days...we may fix it too
        //THIRD = date.getBytes()[2];
        //MAX_FIXED_INDEX=2;
    }    

    public static long getTimeMillis() {
        return System.currentTimeMillis();
    }

    public static byte[] getTimeMillisInByte(int padding) {
        long currTime = getTimeMillis();
        return convertTimeMillisInByte(currTime, padding);
    }
        
    public static byte[] convertTimeMillisInByte(long currTime, int padding) {
        byte[] timeByteArray = new byte[Constants.SIZE_OF_TIMESTAMP_IN_BYTES+padding];
        long currentValue = 0;
        
        timeByteArray[0] = FIRST;
        timeByteArray[1] = SECOND;
        //timeByteArray[2] = THIRD;
        
        for (int y=Constants.SIZE_OF_TIMESTAMP_IN_BYTES-1; y>MAX_FIXED_INDEX; y--) {
            currentValue = currTime % 10;
            timeByteArray[y] = DEC_DIGITS[(int)currentValue];
            currTime /= 10;
        }
        for (int i = Constants.SIZE_OF_TIMESTAMP_IN_BYTES; i<timeByteArray.length; i++) {
            timeByteArray[i] = 77;
        }
        
        return timeByteArray;
    }
    
}
