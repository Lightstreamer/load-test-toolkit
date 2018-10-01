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
