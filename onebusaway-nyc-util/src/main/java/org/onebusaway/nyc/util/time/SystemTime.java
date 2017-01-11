package org.onebusaway.nyc.util.time;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.util.impl.tdm.ManualConfigurationServiceImpl;
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

  private static final int POLL_INTERVAL = 60; // seconds
  // construct a single instance per JVM
  private static SystemTime INSTANCE = new SystemTime();
  
  private ConfigurationService _configurationService = null;
  private String _enabled = "false"; // turned off by default
  private long adjustment = 0; // by default there is no adjustment
  
  private static Logger _log = LoggerFactory.getLogger(SystemTime.class);

  private SystemTime() {
    UpdateWorker uw = new UpdateWorker();
    new Thread(uw).start();
  }
  public static long currentTimeMillis() {
    return System.currentTimeMillis() + INSTANCE.adjustment;
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
    
	refreshConfigValues();
	  
    if (!isEnabled()) {
      return 0;
    }
    
    return getAdjustment();
  }
  
  private void refreshConfigValues() throws Exception{
	  
	  if(_configurationService == null){
		  _configurationService = new ManualConfigurationServiceImpl();
	  }
	  
	  _configurationService.refreshConfiguration();
	  
	  setEnabled(_configurationService.getConfigurationValueAsString(TIME_ENABLED_KEY, "false"));
	  
	  String adjustment = _configurationService.getConfigurationValueAsString(TIME_ADJUSTMENT_KEY, "0");
	  setAdjustment(getConfigItemAsLong(adjustment));
  }
  
  private Long getConfigItemAsLong(String value){
	  try{
		  Long configItemAsLong = Long.parseLong(value);
		  return configItemAsLong;
	  }
	  catch(NumberFormatException nfe){
		  _log.error("failed to convert config value into Long", nfe);
		  return 0L;
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
      
      try {
    	  refreshConfigValues();
      } catch (Exception e) {
    	  _log.error("Unable to retrieve TDM configuration values",e);
	  }
      
      if (INSTANCE.isEnabled()) {
        _log.warn("SystemTime Adjustment enabled.  Polling configuration service every " + POLL_INTERVAL + " seconds");
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