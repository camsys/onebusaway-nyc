package org.onebusaway.nyc.util.time;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.util.impl.tdm.TransitDataManagerApiLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Single point of time manipulation for OBANYC.  Intended for development
 * only -- not for use in production environments.  Hence the defaults
 * are OFF!
 */
public class SystemTime {

  public static final String TIME_ENABLED_KEY = "tdm.systemTime.enabled";
  public static final String TIME_ADJUSTMENT_KEY = "tdm.systemTime.adjustment";
  public static final String TDM_HOST_KEY = "tdm.host";
  public static final String TDM_PORT_KEY = "tdm.port";
 

  private static final int POLL_INTERVAL = 60; // seconds
  // construct a single instance per JVM
  private static SystemTime INSTANCE = new SystemTime();

  private TransitDataManagerApiLibrary _transitDataManagerApiLibrary = null;
  private String _host = null; // no path configured for TDM by default
  private Integer _port = null; // no port configured for TDM by default
  private String _enabled = "false"; // turned off by default
  private long adjustment = 0; // by default there is no adjustment
  
  private static Logger _log = LoggerFactory.getLogger(SystemTime.class);

  private SystemTime() {
    UpdateWorker uw = new UpdateWorker();
    new Thread(uw).start();
  }
  public static long currentTimeMillis() {
    return SystemTime.currentTimeMillis() + INSTANCE.adjustment;
  }

  public static void setHost(String url) {
    INSTANCE._host = url;
  }
  
  public static void setPort(Integer port) {
    INSTANCE._port = port;
  }

  public static void setEnabled(String enabledFlag) {
    INSTANCE._enabled = enabledFlag;
  }

  public static void setAdjustment(long adjustmentFromNowInMillis) {
    INSTANCE.adjustment = adjustmentFromNowInMillis;
    _log.info("updated adjustment to " + adjustmentFromNowInMillis);
  }

  public static long getAdjustment() {
    return INSTANCE.adjustment;
  }

  private long refreshAdjustment() throws Exception {
    
	refreshTDMConfigValues();
	  
    if (!isEnabled()) {
      return 0;
    }
    if (getHostUrl() == null) {
      return 0;
    }
    
    return getAdjustment();
  }
  
  private void refreshTDMConfigValues() throws Exception{
	  if(_transitDataManagerApiLibrary == null){
	    	_transitDataManagerApiLibrary = new TransitDataManagerApiLibrary(getHostUrl(), getPort(), null);
	  }
	    
	  List<JsonObject> configurationItems = 
			  _transitDataManagerApiLibrary.getItemsForRequest("config", "list");
	    
	    
	  for(JsonObject configItem : configurationItems) {
		
		String configKey = configItem.get("key").getAsString();
		
		if(configKey.equalsIgnoreCase(TIME_ENABLED_KEY))
			setEnabled(configItem.get("value").getAsString());
		
		if(configKey.equalsIgnoreCase(TIME_ADJUSTMENT_KEY))
			setAdjustment(getConfigItemAsLong(configItem));
		
		if(configKey.equalsIgnoreCase(TDM_HOST_KEY))
			setHost(configItem.get("value").getAsString());
		
		if(configKey.equalsIgnoreCase(TDM_PORT_KEY))
			setPort(getConfigItemAsInteger(configItem));
	  } 
  }
  
  private Long getConfigItemAsLong(JsonElement configItem){
	  try{
		  Long configItemAsLong = Long.parseLong(configItem.getAsString());
		  return configItemAsLong;
	  }
	  catch(NumberFormatException nfe){
		  _log.error("failed to convert config value into Long", nfe);
		  return 0L;
	  }
  }
  
  private Integer getConfigItemAsInteger(JsonElement configItem){
	  try{
		  Integer configItemAsInteger = Integer.valueOf(configItem.getAsString());
		  return configItemAsInteger;
	  }
	  catch(NumberFormatException nfe){
		  _log.error("failed to convert config value into Integer", nfe);
		  return null;  
	  }	  
  }

  private String getHostUrl() {
    String host = null;
    // option 1:  read tdm.host from system env
    host = getHostUrlFromSystemProperty();
    if (StringUtils.isNotBlank(host))
      return host;
    // option 2:  spring injection
    if (StringUtils.isNotBlank(_host)) {
      return _host;
    }

    // option 3:  let if fail -- the default is not to configure this
    return null;
  }

  private String getHostUrlFromSystemProperty() {
    return System.getProperty(TDM_HOST_KEY);
  }
  
  private Integer getPort() {
    Integer port = null;
    // option 1:  read tdm.port from system env
    port = getPortFromSystemProperty();
    if (port != null && port > 0)
      return port;
    // option 2:  spring injection
    if (_port != null && port > 0) {
      return _port;
    }

    // option 3:  let if fail -- the default is not to configure this
    return null;
  }
  
  private Integer getPortFromSystemProperty() {
	  try{
		  Integer port =  Integer.valueOf(System.getProperty(TDM_PORT_KEY));
		  return port;
	  }
	  catch(Exception e){
		  return null;
	  } 
  }


  /**
   * determine if time skew adjustment is enabled.  In production
   * environments IT SHOULD NOT BE.  FOR DEVELOPMENT ONLY!
   * @return
   */
  public static boolean isEnabled() {
    String systemProperty = System.getProperty(TIME_ENABLED_KEY);
    if (systemProperty != null) {
      return "true".equals(systemProperty);
    }

    // if endpoint was injected, assume we are turned on
    return "true".equalsIgnoreCase(INSTANCE._enabled);
  }

  public class UpdateWorker implements Runnable {

    @Override
    public void run() {

      if (INSTANCE.isEnabled()) {
        if (StringUtils.isNotBlank(getHostUrl())) {
          _log.warn("SystemTime Adjustment enabled.  Polling "
                  + getHostUrl() + " every " + POLL_INTERVAL + " seconds");
        } else {
          _log.error("SystemTime Adjustment enabled but no endpoint ULR configured!"
          + "  Please set -Dtdm.host=tdm -Dtdm.port=80 or equivalent.  Exiting.");
          return;
        }
      } else {
        _log.info("SystemTime Adjustment disabled.  Exiting.");
        return;
      }


      while (!Thread.interrupted()) {
        try {
          int rc = sleep(POLL_INTERVAL);
          if (rc < 0) {
            _log.info("caught SIGHUP, exiting");
            return;
          }
          _log.debug("check for instance=" + INSTANCE.toString());
          long adj = refreshAdjustment();
          if (adj != adjustment) {
            _log.info("updated adjustment to " + adj);
            adjustment = adj;
          }
        } catch (Exception e) {
          _log.error("refresh failed:" + e);
        }
      }
    }

    private int sleep(int i) {
      try {
        Thread.sleep(i * 1000);
      } catch (InterruptedException e) {
        return -1;
      }
      return 0;
    }
    
  }

}