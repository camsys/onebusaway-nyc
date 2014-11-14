package org.onebusaway.nyc.transit_data_manager.adapters.api.processes;

import org.onebusaway.nyc.transit_data_manager.adapters.ModelCounterpartConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.OperatorAssignment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tcip_final_4_0_0_0.SCHOperatorAssignment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class UTSUtil {

  private static Logger _log = LoggerFactory.getLogger(UTSUtil.class);
  
  public List<OperatorAssignment> listConvertOpAssignTcipToJson(
    ModelCounterpartConverter<SCHOperatorAssignment, OperatorAssignment> conv,
    List<SCHOperatorAssignment> inputAssigns) {
    LinkedHashMap<String, OperatorAssignment> passMap = new LinkedHashMap<String, OperatorAssignment>(); 
    
    _log.debug("About to convert " + inputAssigns.size() + " SCHOperatorAssignments to OperatorAssignment using " + conv.getClass().getName());
    List<OperatorAssignment> outputAssigns = new ArrayList<OperatorAssignment>();

    for (SCHOperatorAssignment assignment : inputAssigns) {

      OperatorAssignment oa = conv.convert(assignment);
      String pass = oa.getPassId();
      // ignore collisions if the ROUTE == DEPOT (obanyc-1440)
      if (passMap.containsKey(pass)) {
        // we have a collision, decide if this record is newer than the stored record
        OperatorAssignment existing = passMap.get(pass);
        if (oa.compareTo(existing) == 0) {
          // for ties, filter out the ROUTE == DEPOT
          if (existing.getRunRoute().equals(existing.getDepot())) {
            passMap.put(pass, oa);
          } else if (!oa.getRunRoute().equals(oa.getDepot())) { // filter out ROUTE == DEPOT
            // if ROUTE != DEPOT, we accept the later entry of the date match
            passMap.put(pass, oa);
          }
        } else if (oa.compareTo(existing) > 0) { // if newer (larger epoch delta)
            passMap.put(pass, oa);
        }
      } else {
        passMap.put(pass, oa);
      }
    }

    for (OperatorAssignment oa : passMap.values()) {
      outputAssigns.add(oa);
    }

    _log.debug("Done converting operatorassignments to tcip.");
    return outputAssigns;
    
  }

  /**
   * remove leading non-numeric characters from pass (operator) id.
   */
  public String stripLeadingCharacters(String passId) {
    if (passId == null) return null;
    if (passId.length() == 0) return passId;
    //passId.matches("^([A-Z]+).*")
    while (passId.charAt(0) >= 'A' && passId.charAt(0) <= 'Z' ) {
      if (passId.length() == 1) return "";
      passId = passId.substring(1, passId.length());
    }
    return passId;
  }
}
