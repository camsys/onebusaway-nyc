package org.onebusaway.nyc.transit_data_manager.importers;

import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onebusaway.nyc.transit_data_manager.model.MtaUtsVehiclePullInPullOut;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.bean.CsvToBean;
import au.com.bytecode.opencsv.bean.HeaderColumnNameTranslateMappingStrategy;

public class CSVVehicleAssignsInputConverter implements VehicleAssignsInputConverter {
	
	private Reader inputReader = null;
    private CSVReader csvReader = null;
	
	public CSVVehicleAssignsInputConverter (Reader csvInputReader) {
		inputReader = csvInputReader;
	}
	
	public List<MtaUtsVehiclePullInPullOut> getVehicleAssignments() {
		setupCsvReader();
		
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
        List<MtaUtsVehiclePullInPullOut> vehicleAssignments = crewAssignsCsv.parse(mapStrat, inputReader);
        
        return vehicleAssignments;
	}
	
	private void setupCsvReader() {
        if (csvReader == null) {
            csvReader = new CSVReader(inputReader);
        }
    }
}
