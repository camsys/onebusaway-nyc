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

package org.onebusaway.nyc.webapp.api;

import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.ServletActionContext;
import org.onebusaway.nyc.api.lib.services.ApiKeyThrottledService;
import org.onebusaway.nyc.api.lib.services.ApiKeyUsageMonitor;
import org.apache.struts2.dispatcher.HttpParameters;
import org.apache.struts2.dispatcher.Parameter;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.users.services.ApiKeyPermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Qualifier;
import uk.org.siri.siri.ErrorDescriptionStructure;
import uk.org.siri.siri.OtherErrorStructure;
import uk.org.siri.siri.ServiceDelivery;
import uk.org.siri.siri.ServiceDeliveryErrorConditionStructure;
import uk.org.siri.siri.Siri;
import uk.org.siri.siri.VehicleMonitoringDeliveryStructure;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;

public class ApiKeyInterceptor extends AbstractInterceptor {

  private static final long serialVersionUID = 1L;

  private static Logger _log = LoggerFactory.getLogger(ApiKeyInterceptor.class);

  private String _type = "xml";

  private Siri _response;

  @Autowired
  private ApiKeyPermissionService _keyService;

  @Autowired
  @Qualifier("NycRealtimeService")
  private RealtimeService _realtimeService;
  
  @Autowired
  private ApiKeyThrottledService _throttledKeyService;

  @Autowired
  private ApiKeyUsageMonitor _keyUsageMonitor;

  @Override
  public String intercept(ActionInvocation invocation) throws Exception {

    determineResponseType();

    int allowed = isAllowed(invocation);

    if (allowed != HttpServletResponse.SC_OK) {
      try {
    	String reason = "";
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
        _response = generateSiriResponse(new NotAuthorizedException(reason));
        HttpServletResponse servletResponse = ServletActionContext.getResponse();
        servletResponse.setStatus(allowed);
        servletResponse.getWriter().write(serializeResponse(servletResponse));
      } catch (Exception e) {
        _log.error(e.getMessage());
        e.printStackTrace();
      }
      return null;
    }

    return invocation.invoke();
  }

  private void determineResponseType() {
    HttpServletRequest request = ServletActionContext.getRequest();
    setType(request.getParameter("type"));
    // Not going to deal with Accept: header for now, although I should
    // But the API Action itself doesn't, either, so...
    // String acceptHeader = request.getHeader("Accept");
  }

  private int isAllowed(ActionInvocation invocation) {
    ActionContext context = invocation.getInvocationContext();
    HttpParameters parameters = context.getParameters();
    Parameter key = parameters.get("key");
    if(key == null || key.getMultipleValues() == null || key.getMultipleValues().length == 0){
      return HttpServletResponse.SC_UNAUTHORIZED;
    }

    String[] keys = key.getMultipleValues();
    _keyUsageMonitor.increment(keys[0]);
    boolean isPermitted = checkIsPermitted(_keyService.getPermission(keys[0], "api"));
    boolean notThrottled = _throttledKeyService.isAllowed(keys[0]);
    
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
  public void setKeyService(ApiKeyPermissionService _keyService) {
    this._keyService = _keyService;
  }

  public ApiKeyPermissionService getKeyService() {
    return _keyService;
  }

  private Siri generateSiriResponse(Exception error) {

    VehicleMonitoringDeliveryStructure vehicleMonitoringDelivery = new VehicleMonitoringDeliveryStructure();
    vehicleMonitoringDelivery.setResponseTimestamp(new Date());

    ServiceDelivery serviceDelivery = new ServiceDelivery();
    serviceDelivery.setResponseTimestamp(new Date());
    serviceDelivery.getVehicleMonitoringDelivery().add(
        vehicleMonitoringDelivery);

    ServiceDeliveryErrorConditionStructure errorConditionStructure = new ServiceDeliveryErrorConditionStructure();

    ErrorDescriptionStructure errorDescriptionStructure = new ErrorDescriptionStructure();
    errorDescriptionStructure.setValue(error.getMessage());

    OtherErrorStructure otherErrorStructure = new OtherErrorStructure();
    otherErrorStructure.setErrorText(error.getMessage());

    errorConditionStructure.setDescription(errorDescriptionStructure);
    errorConditionStructure.setOtherError(otherErrorStructure);

    vehicleMonitoringDelivery.setErrorCondition(errorConditionStructure);

    Siri siri = new Siri();
    siri.setServiceDelivery(serviceDelivery);

    return siri;
  }

  public String serializeResponse(HttpServletResponse servletResponse) {
    try {
      if (_type.equals("xml")) {
        servletResponse.setContentType("application/xml");
        return _realtimeService.getSiriXmlSerializer().getXml(_response);
      } else {
        String callback = ServletActionContext.getRequest().getParameter("callback");
        if(callback != null){
          servletResponse.setContentType("application/javascript");
        } else {
          servletResponse.setContentType("application/json");
        }
        return _realtimeService.getSiriJsonSerializer().getJson(_response, callback);
      }
    } catch (Exception e) {
      return e.getMessage();
    }

  }

  public void setType(String type) {
    _type = type;
  }

}
