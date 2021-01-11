/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onebusaway.nyc.report.impl;

import org.onebusaway.nyc.report.model.CcLocationReportRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CcLocationCache {

  private static final float LOAD_FACTOR = 0.75f;
  private int intialCapacity;
  private LRU<String, CcLocationReportRecord> _cache;
  private static Logger _log = LoggerFactory.getLogger(CcLocationCache.class);

  public CcLocationCache(int initialCapacity) {
	this.setIntialCapacity(initialCapacity);
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

  public int getIntialCapacity() {
	return intialCapacity;
}

public void setIntialCapacity(int intialCapacity) {
	this.intialCapacity = intialCapacity;
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
