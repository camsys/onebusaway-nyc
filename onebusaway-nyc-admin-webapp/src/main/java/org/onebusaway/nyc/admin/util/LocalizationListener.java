package org.onebusaway.nyc.admin.util;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opensymphony.xwork2.util.LocalizedTextUtil;

/**
 * Listen for context configuration for localization.  Default to 
 * original onebusaway-nyc configuration. 
 *
 */
public class LocalizationListener implements ServletContextListener {

  private static final String DEFAULT_RESOURCE = "onebusaway-nyc";
  private static Logger _log = LoggerFactory.getLogger(LocalizationListener.class);
  

  @Override
  public void contextInitialized(ServletContextEvent servletContextEvent) {
    _log.debug("context init");
    ServletContext servletContext = servletContextEvent.getServletContext();
    if (servletContext == null) return; // for testing support
    String resource = (String) servletContext.getInitParameter("obanyc.resource");
    if (resource != null) {
      _log.info("found resource override=" + resource);
      LocalizedTextUtil.addDefaultResourceBundle(resource);
    } else {
      _log.info("did not find resource override, using default localization of " + DEFAULT_RESOURCE);
      LocalizedTextUtil.addDefaultResourceBundle(DEFAULT_RESOURCE);
    }
    
  }


  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    // TODO Auto-generated method stub
    
  }

}
