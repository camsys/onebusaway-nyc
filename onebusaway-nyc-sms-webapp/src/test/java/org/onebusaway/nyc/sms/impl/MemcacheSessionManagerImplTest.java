/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.sms.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Matchers.*;

import net.spy.memcached.CASValue;
import net.spy.memcached.MemcachedClientIF;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.onebusaway.nyc.sms.impl.SessionManagerImpl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class MemcacheSessionManagerImplTest {

  @Mock
  MemcacheClientFactory memcacheClientFactory;
  
  @InjectMocks
  private MemcacheSessionManagerImpl _sessionManager;

  private MemcachedClientIF mockMemcacheClient;

  @Before
  public void setup() throws IOException {
    mockMemcacheClient = mock(MemcachedClientIF.class);
    when(memcacheClientFactory.getCacheClient()).thenReturn(mockMemcacheClient);
//    _sessionManager = new MemcacheSessionManagerImpl();
    ((SessionManagerImpl) _sessionManager).setSessionTimeout(10);
    ((SessionManagerImpl) _sessionManager).setSessionReapearFrequency(2);
    ((SessionManagerImpl) _sessionManager).start();
  }

  @After
  public void teardown() {
    ((SessionManagerImpl) _sessionManager).stop();
  }

  @Test
  public void testSessionManager() {

    assertFalse(_sessionManager.contextExistsFor("A"));

    CASValue<Object> mockCasValue = mock(CASValue.class);
    Map<String, Object> mockSession = new HashMap<String, Object>();
    when(mockCasValue.getValue()).thenReturn(mockSession);
    when(mockMemcacheClient.getAndTouch(eq("A"), anyInt())).thenReturn(mockCasValue);

    Map<String, Object> session = _sessionManager.getContext("A");
    assertTrue(session.isEmpty());
    
    session.put("hello", "world");

    sleep(5 * 1000);

    when(mockMemcacheClient.getAndTouch(eq("A"), anyInt())).thenReturn(mockCasValue);
    
    session = _sessionManager.getContext("A");
    assertEquals("world", session.get("hello"));

    sleep(15 * 1000);

    when(mockMemcacheClient.getAndTouch(eq("A"), anyInt())).thenReturn(null);
    
    session = _sessionManager.getContext("A");
    assertFalse(session.containsKey("hello"));
    
    verify(mockMemcacheClient, times(3)).getAndTouch(eq("A"), anyInt());
  }

  private static final void sleep(long time) {
    try {
      Thread.sleep(time);
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }
}