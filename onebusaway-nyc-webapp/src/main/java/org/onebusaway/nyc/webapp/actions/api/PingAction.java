package org.onebusaway.nyc.webapp.actions.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.io.IOUtils;
import org.apache.struts2.ServletActionContext;
import org.json.JSONException;
import org.json.JSONObject;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.transit_data.model.AgencyWithCoverageBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class PingAction extends OneBusAwayNYCActionSupport {
  
  private static Logger _log = LoggerFactory.getLogger(PingAction.class);
  private static final String TRUE_STRING = "true";
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
      if(!isSucessful(smsUrl)){
    	throw new ServletException("SMS Url " + smsUrl + " is not responding");
      }
      
      return "" + count.size();      
      
    } catch (Throwable t) {
      _log.error("Ping action failed with ", t);
      throw new RuntimeException(t);
    }
  }
  
  private boolean isSucessful(String url){
	  try{
		  JSONObject json = new JSONObject(IOUtils.toString(new URL(url)));
		  String success = json.get(SUCCESS).toString();
		  return success.equals(TRUE_STRING);
	  } catch(Exception e){
		  return false;
	  }
  } 
}
