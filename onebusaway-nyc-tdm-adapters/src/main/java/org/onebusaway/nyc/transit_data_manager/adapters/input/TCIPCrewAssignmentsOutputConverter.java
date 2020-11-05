/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onebusaway.nyc.transit_data_manager.adapters.input;

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaUtsCrewAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tcip_final_3_0_5_1.SCHOperatorAssignment;

public class TCIPCrewAssignmentsOutputConverter implements
    CrewAssignmentsOutputConverter {
  
  private static Logger _log = LoggerFactory.getLogger(TCIPCrewAssignmentsOutputConverter.class);

  private List<MtaUtsCrewAssignment> crewAssignInputData = null;

  private DepotIdTranslator depotIdTranslator;

  public TCIPCrewAssignmentsOutputConverter(List<MtaUtsCrewAssignment> data) {
    crewAssignInputData = data;
  }

  public List<SCHOperatorAssignment> convertAssignments() {
    MtaUtsToTcipAssignmentConverter dataConverter = new MtaUtsToTcipAssignmentConverter();
    dataConverter.setDepotIdTranslator(depotIdTranslator);
    
    List<SCHOperatorAssignment> opAssigns = new ArrayList<SCHOperatorAssignment>();

    _log.debug("About to convert " + crewAssignInputData.size() + " items from UTS input objects to TCIP SCHOperatorAssignment objects using MtaUtsToTcipAssignmentConverter.");
    
    for (MtaUtsCrewAssignment utsAssignment : crewAssignInputData) {
      SCHOperatorAssignment opAssign = dataConverter.ConvertToOutput(utsAssignment);
      if (opAssign != null) {
        opAssigns.add(opAssign);
      }
    }

    _log.debug("Done conversions, returning List<SCHOperatorAssignment>.");
    
    return opAssigns;
  }

  public void setDepotIdTranslator(DepotIdTranslator depotIdTranslator) {
    this.depotIdTranslator = depotIdTranslator;
    
  }
}
