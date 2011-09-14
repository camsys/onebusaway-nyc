package org.onebusaway.nyc.transit_data_manager.importers;

import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onebusaway.nyc.transit_data_manager.model.MtaUtsCrewAssignment;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.bean.CsvToBean;
import au.com.bytecode.opencsv.bean.HeaderColumnNameTranslateMappingStrategy;

public class CSVCrewAssignsInputConverter implements CrewAssignsInputConverter 
{
    private Reader inputReader = null;
    private CSVReader csvReader = null;
        
    public CSVCrewAssignsInputConverter (Reader csvInputReader) {
        inputReader = csvInputReader;
    }
    
    public List<MtaUtsCrewAssignment> getCrewAssignments() {
        setupCsvReader();
        
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
        List<MtaUtsCrewAssignment> crewAssignments = crewAssignsCsv.parse(mapStrat, inputReader);
        
        return crewAssignments;
    }
    
    private void setupCsvReader() {
        if (csvReader == null) {
            csvReader = new CSVReader(inputReader);
        }
    }
}
