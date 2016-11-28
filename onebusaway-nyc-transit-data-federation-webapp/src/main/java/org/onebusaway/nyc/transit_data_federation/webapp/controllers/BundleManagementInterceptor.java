package org.onebusaway.nyc.transit_data_federation.webapp.controllers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.onebusaway.util.services.configuration.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

public class BundleManagementInterceptor extends HandlerInterceptorAdapter {

  @Autowired
  private ConfigurationService _config;
  
  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
      Object handler) throws Exception {
    if (isEnabled())
      return true;
    response.sendError(403);
    return false;
  }

  private boolean isEnabled() {
    return Boolean.parseBoolean(_config.getConfigurationValueAsString("tds.enableBundleSwitchWeb", "false"));
  }

}
