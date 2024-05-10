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


import org.onebusaway.api.ResponseCodes;
import org.onebusaway.api.model.ResponseBean;
import org.onebusaway.exceptions.NoSuchRouteServiceException;
import org.onebusaway.exceptions.NoSuchStopServiceException;
import org.onebusaway.exceptions.NoSuchTripServiceException;
import org.onebusaway.exceptions.OutOfServiceAreaServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class ExceptionInterceptor extends OncePerRequestFilter {

  private static Logger _log = LoggerFactory.getLogger(ExceptionInterceptor.class);

  private static final long serialVersionUID = 1L;

  private static final int V1 = 1;





  private String getActionAsUrl(HttpServletRequest request) {
    String uri = request.getRequestURI();
    String queryString = request.getQueryString();
    StringBuilder b = new StringBuilder();
    b.append(uri);

    if(queryString!=null) {
      String argToRemove = "app_uid";
      int startPointer = queryString.indexOf(argToRemove);
      if (startPointer == -1) {
        b.append(queryString);
      } else {
        b.append("?");
        int endPointer = queryString.indexOf("&", startPointer);
        b.append(queryString.substring(0,startPointer));
        if (endPointer != -1) {
          b.append(queryString.substring(endPointer));
        }
      }
    }
    return b.toString();
  }

  protected ResponseBean getExceptionAsResponseBean(String url, Exception ex) {
    Throwable throwable = ex.getCause();
    if (throwable instanceof NoSuchStopServiceException
            || throwable instanceof NoSuchTripServiceException
            || throwable instanceof NoSuchRouteServiceException) {
      return new ResponseBean(V1, ResponseCodes.RESPONSE_RESOURCE_NOT_FOUND,
              throwable.getMessage(), null);
    }
    else if( throwable instanceof OutOfServiceAreaServiceException) {
      return new ResponseBean(V1, ResponseCodes.RESPONSE_OUT_OF_SERVICE_AREA,
              throwable.getMessage(), null);
    }
    else {
      _log.warn("exception for action: url=" + url, ex);
      return new ResponseBean(V1, ResponseCodes.RESPONSE_SERVICE_EXCEPTION,
              throwable.getMessage(), null);
    }
  }


  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    try {
      filterChain.doFilter(request, response);
    } catch (Exception e) {

      ResponseBean responseBean = getExceptionAsResponseBean(getActionAsUrl(request), e);
      response.setStatus(responseBean.getCode());
      response.setContentType("text/html;charset=utf-8");
      response.getWriter().write(responseBean.getText());
    }
  }
}
