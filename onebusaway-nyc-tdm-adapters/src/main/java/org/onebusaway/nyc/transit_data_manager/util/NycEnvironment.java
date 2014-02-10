package org.onebusaway.nyc.transit_data_manager.util;

import java.io.File;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.org.siri.siri.ParticipantRefStructure;

public class NycEnvironment {

  private static final String ENVIRONMENT_UNKNOWN = "unknown";
  static final Logger _log = LoggerFactory.getLogger(NycEnvironment.class);
  private HashMap<String, Object> _config = null;
  private String _environment = null;
  private String _configServiceClientClassName = null;
  
  private HashMap<String, Object> getConfig() {
	  if (_config != null) {
		  return _config;
	  }
	  
	    try {
	        String config = FileUtils.readFileToString(new File(
	            "/var/lib/obanyc/config.json"));
	        HashMap<String, Object> o = new ObjectMapper(new JsonFactory()).readValue(
	            config, new TypeReference<HashMap<String, Object>>() {
	            });
	        _config = o;
	      } catch (Exception e) {
	        _log.info("Failed to get an environment out of /var/lib/obanyc/config.json, continuing without it.");
	      }
	      return _config;

  }
  
  @SuppressWarnings("unchecked")
  public String getEnvironment() {
    if (_environment != null)
      return _environment;
    
    if (_config == null) {
    	_environment = ENVIRONMENT_UNKNOWN;
    } else {
    	_environment = (String) ((HashMap<String, Object>) _config.get("oba")).get("env");
    }
    return _environment;
  }
  
  public String getConfigServiceClientClassName() {
	  if (_configServiceClientClassName != null) {
		  return _configServiceClientClassName;
	  }
	  if (_config == null) {
		  return null;
	  }
	  _configServiceClientClassName = (String) ((HashMap<String, Object>) _config.get("oba")).get("configServiceClient");
	  return _configServiceClientClassName;
  }
  
  public ParticipantRefStructure getParticipant() {
    ParticipantRefStructure participant = new ParticipantRefStructure();
    participant.setValue(getEnvironment());
    return participant;
  }

  public boolean isUnknown() {
    return StringUtils.equals(getEnvironment(), ENVIRONMENT_UNKNOWN);
  }

}
