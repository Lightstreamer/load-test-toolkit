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

    private synchronized void addCookies(URI uri, List<HttpCookie> cookies) {
        if (! cookies.isEmpty()) {
            if (cookieManager == null) {
                cookieManager = createCookieManager(); // we are already synchronized
            }
            CookieStore store = cookieManager.getCookieStore();
//          if (log.isDebugEnabled()) {
//              logCookies("Before adding cookies for " + uri, store.getCookies());
//              logCookies("Cookies to be added for " + uri, cookies);
//          }
            for (HttpCookie cookie : cookies) {
                store.add(uri, cookie); 
            }
//          if (log.isDebugEnabled()) {
//              logCookies("After adding cookies for " + uri, store.getCookies());
//          }
        } else {
            // we save some processing by using null for an empty store
        }
    }
    
    private static List<HttpCookie> emptyCookieList = Collections.unmodifiableList(new LinkedList<HttpCookie>());
    
    private synchronized List<HttpCookie> getCookies(URI uri) {
        if (cookieManager != null) {
            CookieStore store = cookieManager.getCookieStore();
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
            return emptyCookieList;
        }
    }
    
  public String getCookieHeader(URI target) {
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
  
  public void saveCookies(URI uri, String cookieString) {
    if (cookieString == null) {
//      log.info("Cookies to be saved for " + uri + ": <none>");
      return;
    }
//    log.info("Cookies to be saved for " + uri + ": " + cookieString);
    List<HttpCookie> cookieList = HttpCookie.parse(cookieString);
    addCookies(uri, cookieList);
  }
  
  private CookieManager cookieManager = null;
  
  private static final boolean avoidOldStore;
  
  static {
      CookieManager manager = new CookieManager(null, java.net.CookiePolicy.ACCEPT_ALL);
      CookieStore defaultStore = manager.getCookieStore();
//      log.info("Default CookieStore type: " + defaultStore.getClass().getName());
      if (defaultStore.getClass().getName().equals("sun.net.www.protocol.http.InMemoryCookieStore")
              || defaultStore.getClass().getName().equals("java.net.CookieStoreImpl"))
      {
          // old cookie store; some of them are flawed; use a replacement
          avoidOldStore = true;
      } else {
          avoidOldStore = false;
      }
  }
  
  private static CookieManager createCookieManager() {
      if (! avoidOldStore) {
          CookieManager manager = new CookieManager(null, java.net.CookiePolicy.ACCEPT_ALL);
//          log.info("Setting up custom CookieManager: " + newManager);
          return manager;
      } else {
          CookieManager manager = new CookieManager(new Java7CookieStore(), java.net.CookiePolicy.ACCEPT_ALL);
//          log.info("Improving the custom CookieHandler: " + manager);
          return manager;
      }
  }

}
