/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
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
package org.onebusaway.nyc.sms.impl;

import org.onebusaway.nyc.sms.services.SessionManager;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PersistentSessionManagerImpl implements SessionManager {

  private ConcurrentHashMap<String, ContextEntry> _contextEntriesByKey = new ConcurrentHashMap<String, ContextEntry>();

  /****
   * {@link SessionManager} Interface
   ****/

  @Override
  public Map<String, Object> getContext(String key) {
    ContextEntry entry = getOrCreateContextEntry(key);
    return entry.getContext();
  }

  @Override
  public boolean contextExistsFor(String sessionId) {
    return _contextEntriesByKey.containsKey(sessionId);
  }
  
  /****
   * Private Method
   ****/

  private ContextEntry getOrCreateContextEntry(String key) {
    while (true) {
      ContextEntry entry = new ContextEntry();
      ContextEntry existingEntry = _contextEntriesByKey.putIfAbsent(key, entry);
      return (existingEntry == null) ? entry : existingEntry;
    }
  }

  private static class ContextEntry {
    
    private Map<String, Object> _context = new HashMap<String, Object>();

    public Map<String, Object> getContext() {
      return _context;
    }
  }
}
