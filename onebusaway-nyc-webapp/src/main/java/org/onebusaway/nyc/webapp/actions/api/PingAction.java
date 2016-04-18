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
  
  /**
	 * 
	 */
  private static final long serialVersionUID = -8210535045633625964L;
  private static Logger _log = LoggerFactory.getLogger(PingAction.class);
  private TransitDataService _tds;
  
  public static final String TRUE = "true";
  public static final String NEW_LINE = System.getProperty("line.separator");
  
  public static final String WEBAPP_IS_ONLINE = "Webapp is Online";
  public static final String SMS_IS_ONLINE = "SMS Webapp is Online";
  public static final String API_IS_ONLINE = "API Webapp is Online";
  
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
      StringBuilder sb = new StringBuilder(5);
      
      sb.append(getWebappStatus());
      sb.append(NEW_LINE);
      sb.append(getSmsWebappStatus());
      sb.append(NEW_LINE);
      sb.append(getApiWebappStatus());
      
      return sb.toString();      
      
    } catch (Throwable t) {
      _log.error("Ping action failed with ", t);
      throw new RuntimeException(t);
    }
  }
  
  private String getWebappStatus() throws ServletException{
	  List<AgencyWithCoverageBean> count = _tds.getAgenciesWithCoverage();
	  if (count == null || count.isEmpty()){
		  _log.error("Ping action found agencies = " + count);
		  throw new ServletException("No agencies supported in current bundle");
	  }
	  return WEBAPP_IS_ONLINE;
  }
  
  private String getSmsWebappStatus() throws ServletException{
	  String smsUrl = _config.getConfigurationValueAsString("sms.pingUrl", "http://localhost:8080/onebusaway-nyc-sms-webapp/index.action");
	  if(!isSmsWebappQuerySucessful(smsUrl)){
    	throw new ServletException("SMS Url " + smsUrl + " is not responding");
      }
	  return SMS_IS_ONLINE;
  }
  
  private boolean isSmsWebappQuerySucessful(String url){
	  return isAlive(url, SUCCESS, TRUE);
  }
  
  private String getApiWebappStatus() throws ServletException{
	  String apiUrl = _config.getConfigurationValueAsString("apiWebapp.pingUrl", "http://localhost:8080/onebusaway-nyc-api-webapp/api/ping");
	  if(!isApiWebappQuerySucessful(apiUrl)){
    	throw new ServletException("Api Webapp Url " + apiUrl + " is not responding");
      }
	  return API_IS_ONLINE;
  }
  
  private boolean isApiWebappQuerySucessful(String url){
	  return isAlive(url, SUCCESS, TRUE);
  }
  
  private boolean isAlive(String url, String jsonSuccessKey, String jsonSuccessValue){
	  try{
		  JSONObject json = new JSONObject(IOUtils.toString(new URL(url)));
		  String success = json.get(jsonSuccessKey).toString();
		  return success.equals(jsonSuccessValue);
	  } catch(Exception e){
		  return false;
	  }  
  }
  
}
