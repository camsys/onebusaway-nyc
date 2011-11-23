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
import org.onebusaway.presentation.impl.users.XWorkRequestAttributes;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;

import org.apache.struts2.interceptor.SessionAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Map;

public class MobileCommonsSessionInterceptor extends AbstractInterceptor {

  private static final long serialVersionUID = 1L;

  private SessionManager _sessionManager;

  private String _sessionIdParameterName = "profile_id";

  @Autowired
  public void setSessionManager(SessionManager sessionManager) {
    _sessionManager = sessionManager;
  }

  public void setSessionIdParameterName(String sessionIdParameterName) {
    _sessionIdParameterName = sessionIdParameterName;
  }

  @Override
  public String intercept(ActionInvocation invocation) throws Exception {

    ActionContext context = invocation.getInvocationContext();
    Map<String, Object> parameters = context.getParameters();

    Object rawSessionId = parameters.get(_sessionIdParameterName);

    if (rawSessionId == null)
      return invocation.invoke();

    if (rawSessionId instanceof String[]) {
      String[] values = (String[]) rawSessionId;
      if (values.length == 0)
        return invocation.invoke();
      rawSessionId = values[0];
    }

    String sessionId = rawSessionId.toString();
    Map<String, Object> persistentSession = _sessionManager.getContext(sessionId);

    Map<String, Object> originalSession = context.getSession();
    context.setSession(persistentSession);

    XWorkRequestAttributes attributes = new XWorkRequestAttributes(context, sessionId);
    RequestAttributes originalAttributes = RequestContextHolder.getRequestAttributes();
    RequestContextHolder.setRequestAttributes(attributes);

    Object action = invocation.getAction();
    if (action instanceof SessionAware) {
      if(persistentSession != null) {
        persistentSession.put("sessionId", sessionId);
      }
      
      ((SessionAware)action).setSession(persistentSession);
    }
    
    try {
      return invocation.invoke();
    } finally {
      RequestContextHolder.setRequestAttributes(originalAttributes);
      context.setSession(originalSession);
    }
  }
}
