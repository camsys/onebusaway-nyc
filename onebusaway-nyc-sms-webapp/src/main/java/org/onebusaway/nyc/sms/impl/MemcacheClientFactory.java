package org.onebusaway.nyc.sms.impl;

import java.io.IOException;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.MemcachedClient;

import org.springframework.stereotype.Component;

@Component
public class MemcacheClientFactory {

  private String _addresses;

  public void setAddresses(String addresses) {
    _addresses = addresses;
  }
  
  public MemcachedClient getCacheClient() throws IOException {
    return new MemcachedClient(
        new BinaryConnectionFactory(),
        AddrUtil.getAddresses(_addresses));
  }

}
