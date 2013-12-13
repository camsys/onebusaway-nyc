package org.onebusaway.nyc.presentation.service.cache;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.ConnectionFactoryBuilder.Protocol;
import net.spy.memcached.ConnectionObserver;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.auth.PlainCallbackHandler;

import org.onebusaway.nyc.queue.QueueListenerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import javax.security.auth.callback.CallbackHandler;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public abstract class NycCacheService<K, V> {

  protected static Logger _log = LoggerFactory.getLogger(QueueListenerTask.class);
  protected Cache<K, V> _cache;
  MemcachedClient memcache;
  boolean useMemcached;
  InetSocketAddress addr = new InetSocketAddress("localhost",8080);

  protected abstract void refreshCache();

  // proxy to the actual hashing algorithm 
  public abstract K hash(Object...factors);

  public Cache<K, V> getCache(){
	  AuthDescriptor ad = new AuthDescriptor(new String[]{"PLAIN"},
	          new PlainCallbackHandler("username", "password"));
	  try {
	      
		if (memcache == null || memcache.getAvailableServers().isEmpty()) {
	          memcache = new MemcachedClient(
	              new ConnectionFactoryBuilder().setProtocol(Protocol.BINARY)
	              .setAuthDescriptor(ad)
	              .build(),
	              AddrUtil.getAddresses(addr.toString()));
	          useMemcached=true;
	          return null;
	      	}
	  } catch (Exception e) {
	      _log.info("Couldn't create a connection: \nIOException " + e.getMessage()+". Caching to local instance.");
	  }
      
    if (_cache == null) {
      int timeout = 15;
      _log.info("creating initial GENERIC cache with timeout " + timeout + "...");
      _cache = CacheBuilder.newBuilder()
          .expireAfterWrite(timeout, TimeUnit.SECONDS)
          .build();
      _log.info("done");
    }
    useMemcached=false;
    return _cache;
  }

  public V retrieve(K key){
	  if (useMemcached){
		  return (V) memcache.get(key.toString());
	  }
	  else{
		  return getCache().getIfPresent(key);
	  }
  }

  public void store(K key, V value) {
	  if (useMemcached){
		  memcache.add(key.toString(), 0, value);
		  return;
	  }
	  else{
		  getCache().put(key, value);
	  }
  }

  public boolean containsKey(K key){
	if (useMemcached){
		return memcache.get(key.toString()) != null;
	}
	else{
		return getCache().asMap().containsKey(key);
	}
  }

  public boolean hashContainsKey(Object...factors){
    return containsKey(hash(factors));
  }

  public void hashStore(V value, Object...factors){
    getCache().put(hash(factors), value);
  }
}