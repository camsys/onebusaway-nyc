/**
 * Copyright (C) 2026 Cambridge Systematics, Inc.
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
package org.onebusaway.api.actions.api;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.ActionProxy;
import com.opensymphony.xwork2.config.entities.ActionConfig;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import org.apache.struts2.dispatcher.HttpParameters;
import org.apache.struts2.rest.ContentTypeHandlerManager;
import org.apache.struts2.rest.DefaultHttpHeaders;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.api.ResponseCodes;
import org.onebusaway.nyc.api.lib.impl.ApiKeyUsageMonitorImpl;
import org.onebusaway.users.services.ApiKeyPermissionService;

/**
 * Pins the API key check on every OBA API call (OBANYC-4240): a good key runs the API action and
 * returns its data; a bad or missing key never runs the action, so no data goes out.
 *
 * <p>The check doesn't decide the HTTP status of a refusal by itself: it hands a 401 (or 429 for
 * rate limits) to the Struts REST content handler. Live, dev and prod both answer a bad key with
 * HTTP 200 and the body {@code null}, so this test checks what the interceptor controls (whether
 * the action runs, and the status it asks for), not the final HTTP response.</p>
 */
public class ApiKeyInterceptorTest {

  private static final String ACTION_RESULT = "action-ran";
  private static final String REFUSED_RESULT = "refused";

  private ApiKeyInterceptor interceptor;
  private ApiKeyPermissionService keyService;
  private ContentTypeHandlerManager handlers;
  private ActionInvocation invocation;
  private ActionConfig actionConfig;

  @Before
  public void setUp() throws Exception {
    keyService = mock(ApiKeyPermissionService.class);
    handlers = mock(ContentTypeHandlerManager.class);

    interceptor = new ApiKeyInterceptor();
    interceptor.setKeyService(keyService);
    interceptor.setMimeTypeHandlerSelector(handlers);
    // _keyUsageMonitor is field-injected by Spring and has no setter.
    Field monitor = ApiKeyInterceptor.class.getDeclaredField("_keyUsageMonitor");
    monitor.setAccessible(true);
    monitor.set(interceptor, mock(ApiKeyUsageMonitorImpl.class));

    actionConfig = new ActionConfig.Builder("api", "current-time", "CurrentTimeAction").build();
    ActionProxy proxy = mock(ActionProxy.class);
    when(proxy.getConfig()).thenReturn(actionConfig);

    invocation = mock(ActionInvocation.class);
    when(invocation.getAction()).thenReturn(new Object()); // no @ApiKeyAuthorization: key required
    when(invocation.getProxy()).thenReturn(proxy);
    when(invocation.invoke()).thenReturn(ACTION_RESULT);
    when(handlers.handleResult(eq(actionConfig), any(), any())).thenReturn(REFUSED_RESULT);
  }

  @Test
  public void goodKeyRunsTheAction() throws Exception {
    withKey("OBANYCUI");
    when(keyService.getPermission("OBANYCUI", "api"))
        .thenReturn(ApiKeyPermissionService.Status.AUTHORIZED);

    assertEquals(ACTION_RESULT, interceptor.intercept(invocation));
    verify(invocation).invoke();
  }

  @Test
  public void badKeyNeverRunsTheAction() throws Exception {
    withKey("BADKEY123");
    when(keyService.getPermission("BADKEY123", "api"))
        .thenReturn(ApiKeyPermissionService.Status.UNAUTHORIZED);

    assertEquals(REFUSED_RESULT, interceptor.intercept(invocation));
    verify(invocation, never()).invoke();
    verifyRefusedWith(ResponseCodes.RESPONSE_UNAUTHORIZED);
  }

  @Test
  public void missingKeyNeverRunsTheAction() throws Exception {
    withParameters(new HashMap<>());

    assertEquals(REFUSED_RESULT, interceptor.intercept(invocation));
    verify(invocation, never()).invoke();
    verify(keyService, never()).getPermission(anyString(), anyString());
    verifyRefusedWith(ResponseCodes.RESPONSE_UNAUTHORIZED);
  }

  @Test
  public void keyOverItsRateLimitNeverRunsTheAction() throws Exception {
    withKey("BUSYKEY");
    when(keyService.getPermission("BUSYKEY", "api"))
        .thenReturn(ApiKeyPermissionService.Status.RATE_EXCEEDED);

    assertEquals(REFUSED_RESULT, interceptor.intercept(invocation));
    verify(invocation, never()).invoke();
    verifyRefusedWith(ResponseCodes.RESPONSE_TOO_MANY_REQUESTS);
  }

  private void withKey(String key) {
    Map<String, Object> params = new HashMap<>();
    params.put("key", new String[] {key});
    withParameters(params);
  }

  private void withParameters(Map<String, Object> params) {
    ActionContext context = ActionContext.of(new HashMap<>())
        .withParameters(HttpParameters.create(params).build());
    when(invocation.getInvocationContext()).thenReturn(context);
  }

  private void verifyRefusedWith(int status) throws Exception {
    verify(handlers).handleResult(eq(actionConfig),
        argThat(headers -> ((DefaultHttpHeaders) headers).getStatus() == status), any());
  }
}
