/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.webapp.actions;

import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;

/**
 * Adapted from Google Analytics' ga.jsp code, which was: Copyright 2009 Google Inc. All Rights Reserved.
**/
@Results( {@Result(type = "stream", name = "pixel", params = {"contentType", "image/gif"})} )
public class GaAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;

  // Download stream to write 1x1 px. GIF back to user.
  private InputStream inputStream;
  
  // Tracker version.
  private static final String version = "4.4sj";

  private static final String COOKIE_NAME = "__utmmobile";

  // The path the cookie will be available to, edit this to use a different
  // cookie path.
  private static final String COOKIE_PATH = "/";

  // Two years in seconds.
  private static final int COOKIE_USER_PERSISTENCE = 63072000;

  // 1x1 transparent GIF
  private static final byte[] GIF_DATA = new byte[] {
      (byte)0x47, (byte)0x49, (byte)0x46, (byte)0x38, (byte)0x39, (byte)0x61,
      (byte)0x01, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x80, (byte)0xff,
      (byte)0x00, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0x00, (byte)0x00,
      (byte)0x00, (byte)0x2c, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
      (byte)0x01, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x02,
      (byte)0x02, (byte)0x44, (byte)0x01, (byte)0x00, (byte)0x3b
  };

  // this is the download streamed to the user
  public InputStream getInputStream() {
    return inputStream;
  }

  // A string is empty in our terms, if it is null, empty or a dash.
  private static boolean isEmpty(String in) {
    return in == null || "-".equals(in) || "".equals(in);
  }

  // The last octect of the IP address is removed to anonymize the user.
  private static String getIP(String remoteAddress) {
    if (isEmpty(remoteAddress)) {
      return "";
    }
    // Capture the first three octects of the IP address and replace the forth
    // with 0, e.g. 124.455.3.123 becomes 124.455.3.0
    String regex = "^([^.]+\\.[^.]+\\.[^.]+\\.).*";
    Pattern getFirstBitOfIPAddress = Pattern.compile(regex);
    Matcher m = getFirstBitOfIPAddress.matcher(remoteAddress);
    if (m.matches()) {
      return m.group(1) + "0";
    } else {
      return "";
    }
  }

  // Generate a visitor id for this hit.
  // If there is a visitor id in the cookie, use that, otherwise
  // use the guid if we have one, otherwise use a random number.
  private static String getVisitorId(
      String guid, String account, String userAgent, Cookie cookie)
      throws NoSuchAlgorithmException, UnsupportedEncodingException {

    // If there is a value in the cookie, don't change it.
    if (cookie != null && cookie.getValue() != null) {
      return cookie.getValue();
    }

    String message;
    if (!isEmpty(guid)) {
      // Create the visitor id using the guid.
      message = guid + account;
    } else {
      // otherwise this is a new user, create a new random id.
      message = userAgent + getRandomNumber() + UUID.randomUUID().toString();
    }

    MessageDigest m = MessageDigest.getInstance("MD5");
    m.update(message.getBytes("UTF-8"), 0, message.length());
    byte[] sum = m.digest();
    BigInteger messageAsNumber = new BigInteger(1, sum);
    String md5String = messageAsNumber.toString(16);

    // Pad to make sure id is 32 characters long.
    while (md5String.length() < 32) {
      md5String = "0" + md5String;
    }

    return "0x" + md5String.substring(0, 16);
  }

  // Get a random number string.
  private static String getRandomNumber() {
    return Integer.toString((int) (Math.random() * 0x7fffffff));
  }

  // Make a tracking request to Google Analytics from this server.
  // Copies the headers from the original request to the new one.
  // If request containg utmdebug parameter, exceptions encountered
  // communicating with Google Analytics are thown.
  private void sendRequestToGoogleAnalytics(
      String utmUrl, HttpServletRequest request) throws Exception {
    try {
      URL url = new URL(utmUrl);
      URLConnection connection = url.openConnection();
      connection.setUseCaches(false);

      connection.addRequestProperty("User-Agent",
          request.getHeader("User-Agent"));
      connection.addRequestProperty("Accepts-Language",
          request.getHeader("Accepts-Language"));

      connection.getContent();
    } catch (Exception e) {
      if (request.getParameter("utmdebug") != null) {
        throw new Exception(e);
      }
    }
  }

  // Track a page view, updates all the cookies and campaign tracker,
  // makes a server side request to Google Analytics and writes the transparent
  // gif byte data to the response.
  @Override
  public String execute() throws Exception {
    HttpServletRequest request = ServletActionContext.getRequest();      
    HttpServletResponse response = ServletActionContext.getResponse();
    
 	String domainName = request.getServerName();
    if (isEmpty(domainName)) {
      domainName = "";
    }

    // Get the referrer from the utmr parameter, this is the referrer to the
    // page that contains the tracking pixel, not the referrer for tracking
    // pixel.
    String documentReferer = request.getParameter("utmr");
    if (isEmpty(documentReferer)) {
      documentReferer = "-";
    } else {
      documentReferer = URLDecoder.decode(documentReferer, "UTF-8");
    }
    String documentPath = request.getParameter("utmp");
    if (isEmpty(documentPath)) {
      documentPath = "";
    } else {
      documentPath = URLDecoder.decode(documentPath, "UTF-8");
    }
    
    String account = request.getParameter("utmac");
    String userAgent = request.getHeader("User-Agent");
    if (isEmpty(userAgent)) {
      userAgent = "";
    }

    // Try and get visitor cookie from the request.
    Cookie[] cookies = request.getCookies();
    Cookie cookie = null;
    if (cookies != null) {
      for(int i = 0; i < cookies.length; i++) {
        if (cookies[i].getName().equals(COOKIE_NAME)) {
          cookie = cookies[i];
        }
      }
    }

    String visitorId = getVisitorId(
        request.getHeader("X-DCMGUID"), account, userAgent, cookie);

    // Always try and add the cookie to the response.
    Cookie newCookie = new Cookie(COOKIE_NAME, visitorId);
    newCookie.setMaxAge(COOKIE_USER_PERSISTENCE);
    newCookie.setPath(COOKIE_PATH);
    response.addCookie(newCookie);

    // Construct the gif hit url.
    String utmUrl = "utmwv=" + version +
        "&utmn=" + getRandomNumber() +
        "&utmhn=" + domainName +
        "&utmr=" + documentReferer +
        "&utmp=" + documentPath +
        "&utmac=" + account +
        "&utmcc=__utma%3D999.999.999.999.999.1%3B" +
        "&utmvid=" + visitorId +
        "&utmip=" + getIP(request.getRemoteAddr());

    // event tracking
    String type = request.getParameter("utmt");
    String event = request.getParameter("utme");
    if (!isEmpty(type) && !isEmpty(event)) {
    	utmUrl += "&utmt=" + URLDecoder.decode(type, "UTF-8");
    	utmUrl += "&utme=" + URLDecoder.decode(event, "UTF-8");
    }

    URI utfGifLocationUri = new URI("http", null, "www.google-analytics.com", 80, "/__utm.gif", utmUrl, null);
    
    sendRequestToGoogleAnalytics(utfGifLocationUri.toASCIIString(), request);

    // If the debug parameter is on, add a header to the response that contains
    // the url that was used to contact Google Analytics.
    if (request.getParameter("utmdebug") != null) {
      response.setHeader("X-GA-MOBILE-URL", utfGifLocationUri.toASCIIString());
    }
    
    // write 1x1 pixel tracking gif to output stream
    final PipedInputStream pipedInputStream = new PipedInputStream();
    final PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream);
    
    response.addHeader(
            "Cache-Control",
            "private, no-cache, no-cache=Set-Cookie, proxy-revalidate");
    response.addHeader("Pragma", "no-cache");
    response.addHeader("Expires", "Wed, 17 Sep 1975 21:32:10 GMT");

    pipedOutputStream.write(GIF_DATA);
    pipedOutputStream.flush();
    pipedOutputStream.close();
    
    // the input stream will get populated by the piped output stream
    inputStream = pipedInputStream;        
    return "pixel";
  }
}


