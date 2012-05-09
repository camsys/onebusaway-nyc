package org.onebusaway.nyc.report_archive.impl;

import org.onebusaway.nyc.report_archive.model.CcLocationReportRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CcLocationCache {

  private static final float LOAD_FACTOR = 0.75f;
  private LRU<String, CcLocationReportRecord> _cache;
  private static Logger _log = LoggerFactory.getLogger(CcLocationCache.class);

  public CcLocationCache(int initialCapacity) {
    _cache = new LRU<String, CcLocationReportRecord>(initialCapacity,
        LOAD_FACTOR, true);
  }

  public synchronized CcLocationReportRecord get(String uuid) {
    CcLocationReportRecord record = _cache.remove(uuid);
    return record;
  }

  public synchronized void put(CcLocationReportRecord record) {
    if (record != null) {
      _cache.put(record.getUUID(), record);
    }

  }

  public synchronized void putAll(List<CcLocationReportRecord> records) {
    for (CcLocationReportRecord record : records) {
      _cache.put(record.getUUID(), record);
    }
  }

  public synchronized int size() {
    return _cache.size();
  }

  private static class LRU<K, V> extends LinkedHashMap<K, V> {

    private static final long serialVersionUID = 1L;
    private int _capacity;

    public LRU(int initialCapacity, float loadFactor, boolean accessOrder) {
      super(initialCapacity, loadFactor, accessOrder);
      _capacity = initialCapacity;
    }

    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
      return size() > _capacity;
    }
  }
}
