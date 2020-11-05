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

package org.onebusaway.nyc.transit_data_manager.adapters.input.readers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaUtsCrewAssignment;

import au.com.bytecode.opencsv.bean.CsvToBean;
import au.com.bytecode.opencsv.bean.HeaderColumnNameTranslateMappingStrategy;

public class CSVCrewAssignsInputConverter implements CrewAssignsInputConverter {
  private File csvFile = null;
  
  public CSVCrewAssignsInputConverter(File csvInputFile) {
    csvFile = csvInputFile;
  }

  public List<MtaUtsCrewAssignment> getCrewAssignments() throws FileNotFoundException {
    
    Map<String, String> colMap = new HashMap<String, String>();
    colMap.put("DEPOT", "depotField");
    colMap.put("AUTH_ID", "authIdField");
    colMap.put("PASS_NUMBER", "passNumberField");
    colMap.put("ROUTE", "routeField");
    colMap.put("RUN_NUMBER", "runNumberField");
    colMap.put("SERV_ID", "servIdField");
    colMap.put("DATE", "dateField");
    colMap.put("TIMESTAMP", "timestampField");

    HeaderColumnNameTranslateMappingStrategy<MtaUtsCrewAssignment> mapStrat = new HeaderColumnNameTranslateMappingStrategy<MtaUtsCrewAssignment>();
    mapStrat.setType(MtaUtsCrewAssignment.class);
    mapStrat.setColumnMapping(colMap);

    CsvToBean<MtaUtsCrewAssignment> crewAssignsCsv = new CsvToBean<MtaUtsCrewAssignment>();
    
    List<MtaUtsCrewAssignment> crewAssignments = null;
    
    Reader inputReader = null;
    
    try {
      inputReader = new FileReader(csvFile);
      crewAssignments = crewAssignsCsv.parse(mapStrat,
          inputReader);
    } finally {
      if (inputReader != null)
        try {
          inputReader.close();
        } catch (IOException e) {}
    }
    
    return crewAssignments;
  }

}
