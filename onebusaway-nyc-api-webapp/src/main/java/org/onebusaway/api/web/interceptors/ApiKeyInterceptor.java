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
package org.onebusaway.api.web.interceptors;


import org.onebusaway.nyc.api.lib.impl.ApiKeyUsageMonitorImpl;
import org.onebusaway.nyc.api.lib.services.ApiKeyThrottledService;
import org.onebusaway.nyc.api.lib.services.ApiKeyWithRolesPermissionService;
import org.onebusaway.users.services.ApiKeyPermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


// todo: struts to spring: merge this and duplicate in Ops-API, probably should go to nyc-api-lib and then both should pull from it
// todo: struts to spring: also, should do the same thing with the usagemonitor
public class ApiKeyInterceptor implements Filter {

  private static Logger _log = LoggerFactory.getLogger(ApiKeyInterceptor.class);

  private static final long serialVersionUID = 1L;


  @Autowired
  private ApiKeyPermissionService _keyService;

  @Autowired
  private ApiKeyUsageMonitorImpl _keyUsageMonitor;

  @Autowired
  private ApiKeyThrottledService _throttledKeyService;


  @Override
  public void init(FilterConfig filterConfig) {
    ApplicationContext ctx = WebApplicationContextUtils
            .getRequiredWebApplicationContext(filterConfig.getServletContext());
    _keyUsageMonitor = ctx.getBean(org.onebusaway.nyc.api.lib.impl.ApiKeyUsageMonitorImpl.class);
    _keyService = ctx.getBean(org.onebusaway.nyc.api.lib.impl.ApiKeyWithRolesPermissionServiceImpl.class);
    _throttledKeyService = ctx.getBean(org.onebusaway.nyc.api.lib.impl.ApiKeyThrottledServiceImpl.class);
  }



  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

    HttpServletRequest req = (HttpServletRequest) request;

    int allowed = isAllowed(request);

    if (allowed != HttpServletResponse.SC_OK) {
      String reason = "";
      try {
        switch(allowed){
          case HttpServletResponse.SC_UNAUTHORIZED:
            reason = "API key required.";
            break;
          case HttpServletResponse.SC_EXPECTATION_FAILED:
            reason = "API key request rate has been exceeded.";
            break;
          case HttpServletResponse.SC_FORBIDDEN:
            reason = "API key is not authorized.";
            break;
          default:
            reason = "A server error occurred.";
        }
      } catch (Exception e) {
        _log.error(e.getMessage());
        e.printStackTrace();
      }
      ((HttpServletResponse) response).setStatus(allowed);
      ((HttpServletResponse) response).sendError(allowed,reason);
      return;
    }

    _log.debug("Starting a transaction for req : {}", req.getRequestURI());

    chain.doFilter(request, response);
    _log.debug("Committing a transaction for req : {}",req.getRequestURI());
  }

  private int isAllowed(ServletRequest request) {
    String key = request.getParameter("key");
    if(key == null  || key.length() == 0){
      return HttpServletResponse.SC_UNAUTHORIZED;
    }

    _keyUsageMonitor.increment(key);
    boolean isPermitted = checkIsPermitted(_keyService.getPermission(key, "api"));
    boolean notThrottled = _throttledKeyService.isAllowed(key);

    if (isPermitted && notThrottled)
      return HttpServletResponse.SC_OK;
    else if(!notThrottled){
      //we are throttled
      return HttpServletResponse.SC_EXPECTATION_FAILED;
    }else
      return HttpServletResponse.SC_FORBIDDEN;
  }


  private boolean checkIsPermitted(ApiKeyPermissionService.Status api) {
    if(api != null && api.equals(ApiKeyPermissionService.Status.AUTHORIZED)){
      return Boolean.TRUE;
    }
    return Boolean.FALSE;
  }

  @Autowired
  public void setKeyService(ApiKeyWithRolesPermissionService _keyService) {
    this._keyService = _keyService;
  }

  public ApiKeyPermissionService getKeyService() {
    return _keyService;
  }

  @Override
  public void destroy() {

  }

}
