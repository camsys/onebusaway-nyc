package org.onebusaway.nyc.presentation.service.cache;

import net.spy.memcached.MemcachedClient;

import org.onebusaway.nyc.queue.QueueListenerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public abstract class NycCacheService<K, V> {

  protected static Logger _log = LoggerFactory.getLogger(QueueListenerTask.class);
  protected Cache<K, V> _cache;

  InetSocketAddress addr = new InetSocketAddress("127.0.0.1",11211);
  MemcachedClient memcache;
  boolean useMemcached = false;

  protected abstract void refreshCache();

  // proxy to the actual hashing algorithm 
  public abstract K hash(Object...factors);

  protected boolean useMemcached(){
	  if (memcache==null)
	  {
		try {
			memcache = new MemcachedClient(addr);
			useMemcached= true;
			System.out.println("useMemcached has been set to TRUE");
		} catch (IOException e1) {
			e1.printStackTrace();
			useMemcached= false;
			System.out.println("useMemcached has been set to FALSE");
		}
	  }
	  System.out.println("all available servers: "+memcache.getAvailableServers());
      return useMemcached;
  }
  public Cache<K, V> getCache(){
    if (!useMemcached() && _cache == null) {
      int timeout = 15;
      _log.info("creating initial GENERIC cache with timeout " + timeout + "...");
      _cache = CacheBuilder.newBuilder()
          .expireAfterWrite(timeout, TimeUnit.SECONDS)
          .build();
      _log.info("done");
    }
    return _cache;
  }

  public V retrieve(K key){
	  System.out.println("Retrieving! K:"+key);
          if (useMemcached){
                  return (V) memcache.get(key.toString());
          }
          else{
        	  return getCache().getIfPresent(key);
          }
  }

  public void store(K key, V value) {
	  System.out.println("Storing! K:"+key+" V:"+value);
      if (useMemcached){
    	  try{
        	  memcache.set(key.toString(), 0, value);
              return;
    	  }
    	  catch(Exception e){
    		  e.printStackTrace();
    	  }
      }
      getCache().put(key, value);
  }

  public boolean containsKey(K key){
  	Cache<K, V> cache = getCache();
    if (useMemcached){
    	try{
    		System.out.println("key: "+key);
	    	System.out.println("Memcached contains key "+key+"?: "+memcache.get(key.toString()) != null);
	        return memcache.get(key.toString()) != null;
	    }
    	catch(Exception e){
    		e.printStackTrace();
    	}
    }
	System.out.println("Local cache contains key "+key+"?: "+cache.asMap().containsKey(key));
    return cache.asMap().containsKey(key);
  }

  public boolean hashContainsKey(Object...factors){
    return containsKey(hash(factors));
  }

  public void hashStore(V value, Object...factors){
    getCache().put(hash(factors), value);
  }
}