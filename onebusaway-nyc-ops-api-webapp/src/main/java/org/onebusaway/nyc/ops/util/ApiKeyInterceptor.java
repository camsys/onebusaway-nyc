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

package org.onebusaway.nyc.ops.util;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.dispatcher.HttpParameters;
import org.apache.struts2.dispatcher.Parameter;
import org.onebusaway.nyc.api.lib.services.ApiKeyThrottledService;
import org.onebusaway.nyc.api.lib.services.ApiKeyUsageMonitor;
import org.onebusaway.nyc.api.lib.services.ApiKeyWithRolesPermissionService;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.users.services.ApiKeyPermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.WebApplicationContextUtils;
import uk.org.siri.siri.*;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Date;


public class ApiKeyInterceptor implements Filter {

    private static Logger _log = LoggerFactory.getLogger(ApiKeyInterceptor.class);

    private static final long serialVersionUID = 1L;

    private String _type = "xml";

    private Siri _response;


    private ApiKeyWithRolesPermissionService _keyService;


    private ApiKeyThrottledService _throttledKeyService;


    private ApiKeyUsageMonitor _keyUsageMonitor;


    @Override
    public void init(FilterConfig filterConfig) {
        ApplicationContext ctx = WebApplicationContextUtils
                .getRequiredWebApplicationContext(filterConfig.getServletContext());
        _keyUsageMonitor = ctx.getBean(org.onebusaway.nyc.api.lib.services.ApiKeyUsageMonitor.class);
        _throttledKeyService =  ctx.getBean(org.onebusaway.nyc.api.lib.services.ApiKeyThrottledService.class);
        _keyService = ctx.getBean(org.onebusaway.nyc.api.lib.services.ApiKeyWithRolesPermissionService.class);
        int a = 23;
    }



    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;

        determineResponseType(req);

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

        _log.info(
                "Starting a transaction for req : {}",
                req.getRequestURI());

        chain.doFilter(request, response);
        _log.info(
                "Committing a transaction for req : {}",
                req.getRequestURI());
    }

    private void determineResponseType(HttpServletRequest request) {
        setType(request.getParameter("type"));
        // Not going to deal with Accept: header for now, although I should
        // But the API Action itself doesn't, either, so...
        // String acceptHeader = request.getHeader("Accept");
    }

    private int isAllowed(ServletRequest request) {
        String key = request.getParameter("key");
//        HttpParameters parameters = context.getParameters();
//        Parameter key = parameters.get("key");
        if(key == null  || key.length() == 0){
            return HttpServletResponse.SC_UNAUTHORIZED;
        }

        _keyUsageMonitor.increment(key);
        boolean isPermitted = checkIsPermitted(_keyService.getOpsApiOnlyPermission(key, "api"));
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

    public void setType(String type) {
        _type = type;
    }

    @Override
    public void destroy() {

    }
}
