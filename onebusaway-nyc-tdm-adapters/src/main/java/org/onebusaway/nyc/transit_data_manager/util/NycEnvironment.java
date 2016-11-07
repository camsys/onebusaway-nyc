package org.onebusaway.nyc.transit_data_manager.util;

import java.io.File;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.org.siri.siri.ParticipantRefStructure;

public class NycEnvironment {

  private static final String ENVIRONMENT_UNKNOWN = "unknown";
  static final Logger _log = LoggerFactory.getLogger(NycEnvironment.class);
  private String _environment = null;
  
  @SuppressWarnings("unchecked")
  public String getEnvironment() {
    if (_environment != null)
      return _environment;
    
    try {
      String config = FileUtils.readFileToString(new File(
          "/var/lib/obanyc/config.json"));
      HashMap<String, Object> o = new ObjectMapper(new JsonFactory()).readValue(
          config, new TypeReference<HashMap<String, Object>>() {
          });
      _environment = (String) ((HashMap<String, Object>) o.get("oba")).get("env");
    } catch (Exception e) {
      _log.info("Failed to get an environment out of /var/lib/obanyc/config.json, continuing without it.");
      _environment = ENVIRONMENT_UNKNOWN;
    }
    return _environment;
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
