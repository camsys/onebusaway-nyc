/**
 * Copyright (C) 2026 Metropolitan Transportation Authority
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.apache.struts2.dispatcher.HttpParameters;
import org.junit.Test;
import org.mockito.Mockito;
import org.onebusaway.nyc.sms.services.SessionManager;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Checks that {@link MobileCommonsSessionInterceptor} swaps in the texter's saved SMS session
 * while the action runs and puts the original session back afterwards.
 */
public class MobileCommonsSessionInterceptorTest {

  @Test
  public void swapsInSavedSessionAndRestoresOriginal() throws Exception {
    Map<String, Object> originalSession = new HashMap<>();
    Map<String, Object> savedSession = new HashMap<>();
    savedSession.put("lastQuery", "M15");

    SessionManager sessionManager = Mockito.mock(SessionManager.class);
    Mockito.when(sessionManager.contextExistsFor("abc")).thenReturn(true);
    Mockito.when(sessionManager.getContext("abc")).thenReturn(savedSession);

    Map<String, Object> params = new HashMap<>();
    params.put("profile_id", "abc");
    ActionContext context = ActionContext.of(new HashMap<>())
        .withParameters(HttpParameters.create(params).build())
        .withSession(originalSession);

    AtomicReference<Map<String, Object>> sessionDuringAction = new AtomicReference<>();
    ActionInvocation invocation = Mockito.mock(ActionInvocation.class);
    Mockito.when(invocation.getInvocationContext()).thenReturn(context);
    Mockito.when(invocation.getAction()).thenReturn(new Object());
    Mockito.when(invocation.invoke()).thenAnswer(i -> {
      sessionDuringAction.set(context.getSession());
      return "success";
    });

    MobileCommonsSessionInterceptor interceptor = new MobileCommonsSessionInterceptor();
    interceptor.setSessionManager(sessionManager);

    assertEquals("success", interceptor.intercept(invocation));
    assertSame("action sees the saved SMS session", savedSession, sessionDuringAction.get());
    assertSame("original session is put back", originalSession, context.getSession());
    Mockito.verify(sessionManager).saveContext("abc");
    Mockito.verify(sessionManager).close();
  }
}
