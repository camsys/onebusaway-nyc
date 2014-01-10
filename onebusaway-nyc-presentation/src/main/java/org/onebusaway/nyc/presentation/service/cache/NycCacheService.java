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

  InetSocketAddress addr = new InetSocketAddress("sessions-memcache",11211);
  MemcachedClient memcache;
  boolean useMemcached = false;

  protected abstract void refreshCache();

  // proxy to the actual hashing algorithm 
  public abstract K hash(Object...factors);
  
  private boolean _disabled;

  public synchronized void setDisabled(boolean disable) {
	  this._disabled = true;
  }
  
  protected boolean useMemcached(){
    if (memcache==null)
    {
      try {
        memcache = new MemcachedClient(addr);
		useMemcached= true;
	  } 
      catch (IOException e1) {
		e1.printStackTrace();
		useMemcached= false;
	  }
	}
    return useMemcached;
  }
  
  public Cache<K, V> getCache(){
	int timeout = 60;
	return getCache(timeout, "GENERIC");
  }
  
  public Cache<K, V> getCache(int timeout, String type){
	useMemcached();
    if (_cache == null) {
      _log.info("creating initial " + type + " cache with timeout " + timeout + "...");
      _cache = CacheBuilder.newBuilder()
          .expireAfterWrite(timeout, TimeUnit.SECONDS)
          .build();
      _log.info("done");
    }
	if (_disabled) _cache.invalidateAll(); 
    return _cache;
  }

  @SuppressWarnings("unchecked")
  public V retrieve(K key){
    if (useMemcached){
      try{
        return (V) memcache.get(key.toString());
      }
      catch(Exception e){
        e.printStackTrace();
      }
    }
    useMemcached=false;
    return (getCache()!=null?getCache().getIfPresent(key):null);
  }

  public void store(K key, V value) {
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
	    return memcache.get(key.toString()) != null;
	  }
      catch(Exception e){
    	e.printStackTrace();
      }
    }
    return cache.asMap().containsKey(key);
  }

  public boolean hashContainsKey(Object...factors){
    return containsKey(hash(factors));
  }

  public void hashStore(V value, Object...factors){
    getCache().put(hash(factors), value);
  }
}