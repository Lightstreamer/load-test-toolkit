/*
 * Copyright (c) 2004-2015 Weswit s.r.l., Via Campanini, 6 - 20124 Milano, Italy.
 * All rights reserved.
 * www.lightstreamer.com
 *
 * This software is the confidential and proprietary information of
 * Weswit s.r.l.
 * You shall not disclose such Confidential Information and shall use it
 * only in accordance with the terms of the license agreement you entered
 * into with Weswit s.r.l.
 */
package com.lightstreamer.oneway_client.netty;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 */
public class CookieHelper {
    
//    private static final Logger log = LogManager.getLogger(Constants.UTILS_LOG);
    
//    private static void logCookies(String message, List<HttpCookie> cookies) {
//        // usato solo in caso di debug
//        for (HttpCookie cookie : cookies) {
//            message += ("\r\n    " + cookie.toString());
//            message += (" - domain " + cookie.getDomain());
//            message += (" - path " + cookie.getPath());
//            message += (" - expired " + cookie.hasExpired());
//            message += (" - ports " + cookie.getPortlist());
//            message += (" - secure " + cookie.getSecure());
//            message += (" - max-age " + cookie.getMaxAge());
//            message += (" - discard " + cookie.getDiscard());
//            // message += (" - http-only " + cookie.isHttpOnly());
//                // non presente in tutti gli Android, meglio non rischiare,
//                // tanto non sembra importante (accetta http e https, ma non file:)
//            message += (" - version " + cookie.getVersion());
//        }
//        log.debug(message);
//    }

    public static synchronized void addCookies(URI uri, List<HttpCookie> cookies) {
        CookieHandler handler = getCookieHandler();
        if (handler instanceof CookieManager) {
            CookieStore store = ((CookieManager) handler).getCookieStore();
//            if (log.isDebugEnabled()) {
//                logCookies("Before adding cookies for " + uri, store.getCookies());
//                logCookies("Cookies to be added for " + uri, cookies);
//            }
            for (HttpCookie cookie : cookies) {
                store.add(uri, cookie); 
            }
//            if (log.isDebugEnabled()) {
//                logCookies("After adding cookies for " + uri, store.getCookies());
//            }
        } else {
//            log.warn("Global CookieHandler not suitable for cookie storage");
        }
    }
    
    private static List<HttpCookie> emptyCookieList = Collections.unmodifiableList(new LinkedList<HttpCookie>());
    
    public static synchronized List<HttpCookie> getCookies(URI uri) {
        CookieHandler handler = getCookieHandler();
        if (handler instanceof CookieManager) {
            CookieStore store = ((CookieManager) handler).getCookieStore();
            if (uri == null) {
//                if (log.isDebugEnabled()) {
//                    logCookies("While extracting cookies", store.getCookies());
//                }
                return store.getCookies();
            } else {
//                if (log.isDebugEnabled()) {
//                    logCookies("While getting cookies for " + uri, store.getCookies());
//                    logCookies("Result of getting cookies for " + uri, store.get(uri));
//                }
                return store.get(uri);
            }
        } else {
//            log.warn("Global CookieHandler not suitable for cookie retrieval");
            return emptyCookieList;
        }
    }
    
  public static String getCookieHeader(URI target) {
      List<HttpCookie> cookieList = getCookies(target);
      if (!cookieList.isEmpty()) {
      
          StringBuffer headerValue = new StringBuffer();
          
          for (Iterator<HttpCookie> iter = cookieList.iterator(); iter.hasNext();) {
              if (headerValue.length() != 0) {
                  headerValue.append("; ");
              }
              HttpCookie cookie = iter.next();
              headerValue.append(cookie.toString()); //cookie toString generates the correct cookie value
          }
          
          String header = headerValue.toString(); 
//          log.info("Cookies to be inserted for " + target + ": " + header);
          return header;
          
      }
//      log.info("Cookies to be inserted for " + target + ": <none>");
      return null;
  }
  
  public static void saveCookies(URI uri, String cookieString) {
    if (cookieString == null) {
//      log.info("Cookies to be saved for " + uri + ": <none>");
      return;
    }
//    log.info("Cookies to be saved for " + uri + ": " + cookieString);
    List<HttpCookie> cookieList = HttpCookie.parse(cookieString);
    addCookies(uri, cookieList);
  }
  
  /**
   * Private cookie handler used when there is no global handler available (see {@link CookieHandler#setDefault(CookieHandler)}).
   */
  private static CookieManager cookieHandler;
  
  /**
   * True if the next call to {@link CookieHelper#getCookieHandler()} will be the first one. False otherwise.
   */
  private static boolean firstTime = true;
  
  /**
   * If the first time the method is called the user hasn't set a default cookie manager
   * (see {@link CookieHandler#setDefault(CookieHandler)}), the library creates a local manager. 
   * Every successive call uses the local manager regardless of whether the user installs
   * a default manager.
   * <br>
   * On the other hand if the user has installed a default cookie manager, 
   * the library uses the default manager. If the user changes the default manager,
   * the library uses the new manager. If the user removes the default manager,
   * the library doesn't manage cookies.
   */
  private static synchronized CookieHandler getCookieHandler() {
      if (firstTime) {
          firstTime = false;
          if (CookieHandler.getDefault() == null) {
              cookieHandler = new CookieManager(null, java.net.CookiePolicy.ACCEPT_ALL);
//              log.info("Setting up custom CookieHandler: " + cookieHandler);
              CookieStore defaultStore = cookieHandler.getCookieStore();
//              log.info("Default CookieStore type: " + defaultStore.getClass().getName());
              if (defaultStore.getClass().getName().equals("sun.net.www.protocol.http.InMemoryCookieStore")
                      || defaultStore.getClass().getName().equals("java.net.CookieStoreImpl")) {
                  // old cookie store; some of them are flawed; use a replacement
                  cookieHandler = new CookieManager(new Java7CookieStore(), java.net.CookiePolicy.ACCEPT_ALL);
//                  log.info("Improving the custom CookieHandler: " + cookieHandler);
              }
          } else {
//              log.info("Will use the default CookieHandler");
          }
      }
      if (cookieHandler != null) {
          return cookieHandler;
      } else {
          CookieHandler currentHandler = CookieHandler.getDefault();
//          if (log.isDebugEnabled()) {
//              log.debug("Using the current default CookieHandler: " + currentHandler);
//          }
          return currentHandler;
      }
  }
  
  /**
   * Returns true if the internal CookieManager, to be used
   * when a default cookie handler is not supplied, is set
   */
  public static synchronized boolean isCookieHandlerLocal() {
      getCookieHandler(); // to determine it as a side effect, if needed
      return cookieHandler != null;
  }
  
  /**
   * TEST ONLY
   * resets the state of the CookieHelper class
   */
  public static synchronized void reset() {
//      if (cookieHandler != null) {
//          log.info("Discarding the custom CookieHandler");
//      }
      cookieHandler = null;
      firstTime = true;
  }
  
}
