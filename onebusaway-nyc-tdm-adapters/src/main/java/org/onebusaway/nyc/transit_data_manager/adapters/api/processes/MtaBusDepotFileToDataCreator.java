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

package org.onebusaway.nyc.transit_data_manager.adapters.api.processes;

import java.io.File;
import java.io.FileNotFoundException;

import java.io.IOException;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.onebusaway.nyc.transit_data_manager.adapters.data.ImporterBusDepotData;
import org.onebusaway.nyc.transit_data_manager.adapters.data.VehicleDepotData;
import org.onebusaway.nyc.transit_data_manager.adapters.input.BusDepotAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.BusDepotAssignsToDepotMapTranslator;
import org.onebusaway.nyc.transit_data_manager.adapters.input.GroupByPropInListObjectTranslator;
import org.onebusaway.nyc.transit_data_manager.adapters.input.TCIPBusDepotAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaBusDepotAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.BusDepotAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.XMLBusDepotAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;

import tcip_final_3_0_5_1.CPTFleetSubsetGroup;

public class MtaBusDepotFileToDataCreator {

  private File inputFile;
  private DepotIdTranslator depotIdTranslator;
  
  public MtaBusDepotFileToDataCreator(File mtaBusDepotFile) throws FileNotFoundException {
    super();
    
    setInputFile(mtaBusDepotFile);
  }
  
  public VehicleDepotData generateVehicleDepotData() throws IOException {
    List<MtaBusDepotAssignment> assignments = loadDepotAssignments();

    // With Bus Depot Assignments we need to group MtaBusDepotAssignment s by
    // the depot field.
    // I'll add another class to do that.
    GroupByPropInListObjectTranslator<List<MtaBusDepotAssignment>, Map<String, List<MtaBusDepotAssignment>>> translator = new BusDepotAssignsToDepotMapTranslator();
    // Create a map to map depot codes to lists of MtaBusDepotAssignment s
    // corresponding to that depot.
    Map<String, List<MtaBusDepotAssignment>> depotBusesMap = translator.restructure(assignments);

    BusDepotAssignmentsOutputConverter converter = new TCIPBusDepotAssignmentsOutputConverter(
        depotBusesMap);
    converter.setDepotIdTranslator(depotIdTranslator);
    
    List<CPTFleetSubsetGroup> fleetSSGroups = converter.convertAssignments();

    // At this point I've got a list of CPTFleetSubsetGroup, so create the data
    // object to hold/manage it.
    VehicleDepotData data = new ImporterBusDepotData(fleetSSGroups);

    setLastModifiedDate(data, inputFile);
    return data;
  }

  private void setLastModifiedDate(VehicleDepotData data, File inputFile) {
    
    data.setLastUpdatedDate(new Date(inputFile.lastModified()));
    
  }

  public List<MtaBusDepotAssignment> loadDepotAssignments() throws IOException {
         
    BusDepotAssignsInputConverter inConv = new XMLBusDepotAssignsInputConverter(
        inputFile);

    List<MtaBusDepotAssignment> assignments = inConv.getBusDepotAssignments();

    return assignments;
  }
  
  private void setInputFile(File inputFile) throws FileNotFoundException {
    this.inputFile = inputFile;
  }

  public void setDepotIdTranslator(DepotIdTranslator depotIdTranslator) {
    this.depotIdTranslator = depotIdTranslator;
  }
}
