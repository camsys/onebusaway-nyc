package org.onebusaway.nyc.geocoder.impl;

import org.apache.commons.codec.binary.Base64;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

// adapted from: http://gmaps-samples.googlecode.com/svn/trunk/urlsigning/UrlSigner.java
public class GoogleUrlAuthentication {

  private byte[] key;
  
  public GoogleUrlAuthentication(String keyString) throws IOException {
    keyString = keyString.replace('-', '+');
    keyString = keyString.replace('_', '/');

    this.key = Base64.decodeBase64(keyString.getBytes());
  }

  public String signRequest(String resource) throws NoSuchAlgorithmException,
    InvalidKeyException, UnsupportedEncodingException, URISyntaxException {
    
    // Get an HMAC-SHA1 signing key from the raw key bytes
    SecretKeySpec sha1Key = new SecretKeySpec(key, "HmacSHA1");

    // Get an HMAC-SHA1 Mac instance and initialize it with the HMAC-SHA1 key
    Mac mac = Mac.getInstance("HmacSHA1");
    mac.init(sha1Key);

    // compute the binary signature for the request
    byte[] sigBytes = mac.doFinal(resource.getBytes());

    // base 64 encode the binary signature
    String signature = new String(Base64.encodeBase64(sigBytes));
    
    // convert the signature to 'web safe' base 64
    signature = signature.replace('+', '-');
    signature = signature.replace('/', '_');
    
    return resource + "&signature=" + signature;
  }
}