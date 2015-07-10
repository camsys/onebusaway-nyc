package org.onebusaway.nyc.webapp.actions.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.struts2.ServletActionContext;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.transit_data.model.AgencyWithCoverageBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class PingAction extends OneBusAwayNYCActionSupport {
  
  private static Logger _log = LoggerFactory.getLogger(PingAction.class);
  private TransitDataService _tds;
  
  @Autowired
  public void setTransitDataService(TransitDataService tds) {
    _tds = tds;
  }
  
  @Autowired
  private ConfigurationService _config;
  
  @Override
  public String execute() throws Exception {
    String pingStatus = null;
    try {
      pingStatus = getPing();
      ServletActionContext.getResponse().setContentType("application/json");
      ServletActionContext.getResponse().getWriter().write(pingStatus);
    } catch (Throwable t) {
      ServletActionContext.getResponse().setStatus(500);
    }
    return null;
  }
  
  public String getPing() throws RuntimeException {
    try {
      List<AgencyWithCoverageBean> count = _tds.getAgenciesWithCoverage();
      String smsUrl = _config.getConfigurationValueAsString("sms.pingUrl", "http://localhost:8080/onebusaway-nyc-sms-webapp/index.action");
      
      if (count == null || count.isEmpty()) {
        _log.error("Ping action found agencies = " + count);
        throw new ServletException("No agencies supported in current bundle");
      }
      if(!isResponsive(smsUrl)){
    	throw new ServletException("SMS Url " + smsUrl + " is not responding");
      }
      
      return "" + count.size();      
      
    } catch (Throwable t) {
      _log.error("Ping action failed with ", t);
      throw new RuntimeException(t);
    }
  }
  
  private boolean isResponsive(String url) throws IOException{
	  
	  boolean responsive = false;
	  
	  try {
          URL siteURL = new URL(url);
          HttpURLConnection connection = (HttpURLConnection) siteURL
                  .openConnection();
          connection.setRequestMethod("GET");
          connection.connect();

          int code = connection.getResponseCode();
          if (code == 200) {
              responsive = true;
          }
      } catch (Exception e) {
    	  responsive = false;
      }

	  return responsive;
	  
  }
  
}
