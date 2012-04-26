package org.onebusaway.nyc.sms.impl;

import java.io.IOException;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.MemcachedClientIF;

public class MemcacheClientFactory {

  public MemcachedClientIF getCacheClient() throws IOException {
    // TODO Have to configure addresses
    return new MemcachedClient(
        AddrUtil.getAddresses("server1:11211 server2:11211"));
  }

}
