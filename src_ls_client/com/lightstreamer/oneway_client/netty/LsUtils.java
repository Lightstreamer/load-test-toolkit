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

import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.Nonnull;

public class LsUtils {
    
    /**
     * Returns URI from string.
     */
    public static @Nonnull URI uri(String uri) {
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    /**
     * Returns whether the URI is secured.
     */
    public static boolean isSSL(URI uri) {
        String scheme = uri.getScheme();
        return "https".equalsIgnoreCase(scheme) || "wss".equalsIgnoreCase(scheme);
    }
    
    /**
     * Returns the port of an URI.
     */
    public static int port(URI uri) {
        int port = uri.getPort();
        if (port == -1) {
            return isSSL(uri) ? 443 : 80;
        } else {
            return port;
        }
    }
}
