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
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.onebusaway.nyc.transit_data_manager.adapters.data.VehicleDepotData;
import org.onebusaway.nyc.transit_data_manager.adapters.tcip.FleetSubsetsGenerator;

import tcip_final_3_0_5_1.CPTFleetSubsetGroup;
import tcip_final_3_0_5_1.CptFleetSubsets;
import tcip_final_3_0_5_1.ObjectFactory;

public class MtaBusDepotsToTcipXmlProcess extends FileToFileConverterProcess {

  public MtaBusDepotsToTcipXmlProcess(File inputFile, File outputFile) {
    super(inputFile, outputFile);
  }

  public void executeProcess() throws IOException {

    MtaBusDepotFileToDataCreator dataCreator = new MtaBusDepotFileToDataCreator(
        inputFile);
    
    dataCreator.setDepotIdTranslator(depotIdTranslator);
    
    VehicleDepotData data = dataCreator.generateDataObject();
    
    List<CPTFleetSubsetGroup> allDefinedGroups = new ArrayList<CPTFleetSubsetGroup>();

    Iterator<String> depNamesIt = data.getAllDepotNames().iterator();
    while(depNamesIt.hasNext()) {
      allDefinedGroups.addAll(data.getGroupsWithDepotNameStr(depNamesIt.next()));
    }
    
    FleetSubsetsGenerator fleetSSGen = new FleetSubsetsGenerator();
    CptFleetSubsets fleetSubsets = fleetSSGen.generateFromSubsetGroups(allDefinedGroups);

    try {
      output = generateXml(fleetSubsets);
    } catch (JAXBException e) {
      e.printStackTrace();
    }

  }

  private String generateXml(CptFleetSubsets inputElement) throws JAXBException {
    String outputStr = null;

    ObjectFactory tcipFinalObjectFactory = new ObjectFactory();
    JAXBElement<CptFleetSubsets> inputWrappedAsJaxbElement = tcipFinalObjectFactory.createCptFleetSubsets(inputElement);

    StringWriter wrtr = new StringWriter();

    JAXBContext jc = JAXBContext.newInstance(CptFleetSubsets.class);
    Marshaller m = jc.createMarshaller();
    m.setProperty("jaxb.formatted.output", new Boolean(true));
    m.marshal(inputWrappedAsJaxbElement, wrtr);
    outputStr = wrtr.toString();

    return outputStr;
  }

}
