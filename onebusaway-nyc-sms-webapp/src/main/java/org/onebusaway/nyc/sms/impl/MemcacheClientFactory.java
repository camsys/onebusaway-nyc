package org.onebusaway.nyc.sms.impl;

import java.io.IOException;

import org.springframework.stereotype.Component;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.MemcachedClientIF;

@Component
public class MemcacheClientFactory {

  private String _addresses;

  public void setAddresses(String addresses) {
//  _addresses = "localhost:11211";
    _addresses = addresses;
  }
  
  public MemcachedClientIF getCacheClient() throws IOException {
    return new MemcachedClient(
        new BinaryConnectionFactory(),
        AddrUtil.getAddresses(_addresses));
  }

}
