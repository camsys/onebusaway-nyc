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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class ExceptionInterceptor implements HandlerInterceptor {

  private static Logger _log = LoggerFactory.getLogger(ExceptionInterceptor.class);

  private static final long serialVersionUID = 1L;

  private static final int V1 = 1;


//
//  protected ResponseBean getExceptionAsResponseBean(String url, Exception ex) {
//    if (ex instanceof NoSuchStopServiceException
//            || ex instanceof NoSuchTripServiceException
//            || ex instanceof NoSuchRouteServiceException) {
//      return new ResponseBean(V1, ResponseCodes.RESPONSE_RESOURCE_NOT_FOUND,
//              ex.getMessage(), null);
//    }
//    else if( ex instanceof OutOfServiceAreaServiceException) {
//      return new ResponseBean(V1, ResponseCodes.RESPONSE_OUT_OF_SERVICE_AREA,
//              ex.getMessage(), null);
//    }
//    else {
//      _log.warn("exception for action: url=" + url, ex);
//      return new ResponseBean(V1, ResponseCodes.RESPONSE_SERVICE_EXCEPTION,
//              ex.getMessage(), null);
//    }
//  }
//
////  private String getActionAsUrl(ActionInvocation invocation) {
////
////    ActionProxy proxy = invocation.getProxy();
////    ActionContext context = invocation.getInvocationContext();
////
////    StringBuilder b = new StringBuilder();
////    b.append(proxy.getNamespace());
////    b.append("/");
////    b.append(proxy.getActionName());
////    b.append("!");
////    b.append(proxy.getMethod());
////
////    HttpParameters params = context.getParameters();
////
////    if (!params.isEmpty()) {
////      b.append("?");
////      boolean seenFirst = false;
////      for (Map.Entry<String, Parameter> entry : params.entrySet()) {
////
////        // Prune out any identifying information
////        if ("app_uid".equals(entry.getKey()))
////          continue;
////
////        if(entry.getValue() == null){
////          continue;
////        }
////        Object value = entry.getValue().getObject();
////        String[] values = (value instanceof String[]) ? (String[]) value
////            : new String[] {value.toString()};
////        for (String v : values) {
////          if (seenFirst)
////            b.append("&");
////          else
////            seenFirst = true;
////          b.append(entry.getKey());
////          b.append("=");
////          b.append(v);
////        }
////      }
////    }
////
////    return b.toString();
////  }
//
//
//
//
//  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
//    try{
//      chain.doFilter(request, response);
//    } catch (Exception ex) {
//      ResponseBean responseBean = getExceptionAsResponseBean(request.getLocalAddr(), ex);
//      ((HttpServletResponse) response).setStatus(responseBean.getCode());
////      ((HttpServletResponse) response).sendError(responseBean.getCode(), responseBean.getText());
//      return;
//    }
////    if(((HttpServletResponse) response).getStatus()!=200){
////      ((HttpServletResponse) response).setStatus(404);
////      ((HttpServletResponse) response).sendError(404, null);
////    }
//
//  }

  @PostConstruct
  public void sample(){
    int i = 11;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    int i = 3;
    // This is called before the controller method is invoked
    // Return true to continue the request, false to abort
    return true;
  }

  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    int i = 4;
    // This is called after the controller method is invoked, but before the view is rendered
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
    int i = 11;
    // This is called after the complete request has finished
    // It's called after rendering the view, hence useful for cleanup tasks
  }


}
