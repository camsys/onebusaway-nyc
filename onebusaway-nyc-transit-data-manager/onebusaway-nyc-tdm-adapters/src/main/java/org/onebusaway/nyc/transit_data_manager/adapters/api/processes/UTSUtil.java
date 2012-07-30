package org.onebusaway.nyc.transit_data_manager.adapters.api.processes;

import org.onebusaway.nyc.transit_data_manager.adapters.ModelCounterpartConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.OperatorAssignment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tcip_final_3_0_5_1.SCHOperatorAssignment;

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
      //outputAssigns.add(conv.convert(assignment));
      OperatorAssignment oa = conv.convert(assignment);
      String pass = oa.getPassId();
      if (passMap.containsKey(pass)) {
        // we have a collision, decide if this record is newer than the stored record
        // TODO
        OperatorAssignment existing = passMap.get(pass);
        if (oa.compareTo(existing) > 0) { // if newer (larger epoch delta)
          passMap.remove(existing);
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
}
