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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaBusDepotAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;

import tcip_final_3_0_5_1.CPTFleetSubsetGroup;

public class TCIPBusDepotAssignmentsOutputConverter implements
    BusDepotAssignmentsOutputConverter {

  private Map<String, List<MtaBusDepotAssignment>> mtaDepotToBusesAtDepotMap = null;
  private DepotIdTranslator depotIdTranslator;

  public TCIPBusDepotAssignmentsOutputConverter(
      Map<String, List<MtaBusDepotAssignment>> data) {
    mtaDepotToBusesAtDepotMap = data;
  }

  public List<CPTFleetSubsetGroup> convertAssignments() {

    MtaDepotMapToTcipAssignmentConverter dataConverter = new MtaDepotMapToTcipAssignmentConverter();
    dataConverter.setDepotIdTranslator(depotIdTranslator);

    List<CPTFleetSubsetGroup> assigns = new ArrayList<CPTFleetSubsetGroup>();

    CPTFleetSubsetGroup opAssign = null;
    String mtaDepotStr = null;
    
    Iterator<String> mtaDepotItr = mtaDepotToBusesAtDepotMap.keySet().iterator();

    while (mtaDepotItr.hasNext()) {
      mtaDepotStr = mtaDepotItr.next();
      // Note that in dataConverter, the depot ids will get translated. I will stick with the source IDs here
      // So that all the translation happens in the same class.
      opAssign = dataConverter.ConvertToOutput(mtaDepotStr, mtaDepotToBusesAtDepotMap.get(mtaDepotStr));
      assigns.add(opAssign);
    }
    
    
    return assigns;
  }

  public void setDepotIdTranslator(DepotIdTranslator depotIdTranslator) {
    this.depotIdTranslator = depotIdTranslator;
  }
}
