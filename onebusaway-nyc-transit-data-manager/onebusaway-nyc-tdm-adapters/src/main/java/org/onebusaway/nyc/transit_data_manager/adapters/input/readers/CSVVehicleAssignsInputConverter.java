package org.onebusaway.nyc.transit_data_manager.adapters.input.readers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaUtsVehiclePullInPullOut;

import au.com.bytecode.opencsv.bean.CsvToBean;
import au.com.bytecode.opencsv.bean.HeaderColumnNameTranslateMappingStrategy;

public class CSVVehicleAssignsInputConverter implements
    VehicleAssignsInputConverter {

  private File csvFile;
  
  public CSVVehicleAssignsInputConverter(File csvInputFile) {
    csvFile = csvInputFile;
  }

  public List<MtaUtsVehiclePullInPullOut> getVehicleAssignments() throws FileNotFoundException {
    
    // Would this mapping possibly be better implemented somewhere else?
    // Inside of MtaUtsVehiclePullInPullOut perhaps? Just a thought
    Map<String, String> colMap = new HashMap<String, String>();
    colMap.put("ROUTE", "routeField");
    colMap.put("DEPOT", "depotField");
    colMap.put("RUN NUMBER", "runNumberField");
    colMap.put("DATE", "dateField");
    colMap.put("SCHED PO", "schedPOField");
    colMap.put("ACTUAL PO", "actualPOField");
    colMap.put("SCHED PI", "schedPIField");
    colMap.put("ACTUAL PI", "actualPIField");
    colMap.put("BUS NUMBER", "busNumberField");
    colMap.put("BUS MILEAGE", "busMileageField");
    colMap.put("PASS", "passNumberField");
    colMap.put("AUTH ID", "authIdField");

    HeaderColumnNameTranslateMappingStrategy<MtaUtsVehiclePullInPullOut> mapStrat = new HeaderColumnNameTranslateMappingStrategy<MtaUtsVehiclePullInPullOut>();
    mapStrat.setType(MtaUtsVehiclePullInPullOut.class);
    mapStrat.setColumnMapping(colMap);

    CsvToBean<MtaUtsVehiclePullInPullOut> crewAssignsCsv = new CsvToBean<MtaUtsVehiclePullInPullOut>();
    
    List<MtaUtsVehiclePullInPullOut> vehicleAssignments = null;
    
    Reader inputReader = null;
    try {
      inputReader = new FileReader(csvFile);
      vehicleAssignments = crewAssignsCsv.parse(
          mapStrat, inputReader);
    } finally {
      if (inputReader != null)
        try {
          inputReader.close();
        } catch (IOException e) {}
    }
    

    return vehicleAssignments;
  }
  
}
