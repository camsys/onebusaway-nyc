package org.onebusaway.nyc.transit_data_manager.adapters.tools;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import au.com.bytecode.opencsv.CSVReader;

public class DepotIdTranslator {
  
  private Map<String, Map<String, String>> datasourceToIdMappingsMap = null;

  public DepotIdTranslator (File configFile) throws IOException {
    if (configFile.exists() && configFile.canRead()) {
      loadConfiguration(configFile);
    } else {
      throw new IOException("Config file " + configFile.getPath() + "does not exist or could not be read.");
    }
  }
  
  private String getMappedIdOrNull (String dataSourceId, String fromId) {
    return datasourceToIdMappingsMap.get(dataSourceId).get(fromId);
  }
  
  public boolean hasMappingForId (String dataSourceId, String fromId) {
    String mappedIdOrNull = getMappedIdOrNull(dataSourceId, fromId);
    
    boolean hasMappingForId = mappedIdOrNull != null ? true : false ;
    
    return hasMappingForId;
  }
  
  public String getMappedId (String dataSourceId, String fromId) {
    String mappedIdOrNull = getMappedIdOrNull(dataSourceId, fromId);
    
    String mappedId = mappedIdOrNull != null ? mappedIdOrNull : fromId ; 
    
    return mappedId;
  }
  
  private void loadConfiguration (File configFile) throws IOException {
    
    CSVReader reader = new CSVReader(new FileReader(configFile));
    
    // Read in the header line of the file.
    String[] nextLine = reader.readNext();
    
    if (nextLine == null)
      throw new IOException("Could not parse config file or file is empty.");
    // We expect the header to contain datasource_name, from and to
    
    int datasourceNameIdx = -1;
    int fromIdx = -1;
    int toIdx = -1;
    
    boolean isConfigFileValid = true; // In the following code, set this to false if soemthings not right. Then throw an exception later if it ends up false.
    
    // parse the header line to get index of each field.
    for (int i = 0; i < nextLine.length; i++) {
      String cellValue = nextLine[i];
      if ("datasource_name".equalsIgnoreCase(cellValue)) {
        if (datasourceNameIdx != -1)
          isConfigFileValid = false;
        else
          datasourceNameIdx = i;
      } else if ("from".equalsIgnoreCase(cellValue)) {
        if (fromIdx != -1)
          isConfigFileValid = false;
        else
          fromIdx = i;
      } else if ("to".equalsIgnoreCase(cellValue)) {
        if (toIdx != -1)
          isConfigFileValid = false;
        else
          toIdx = i;
      }
    }
    
    if ( datasourceNameIdx == -1 || fromIdx == -1 || toIdx == -1) {
      isConfigFileValid = false;
    }
    
    if (!isConfigFileValid) {
      throw new IOException("Config file does not have required columns, or has a duplicated column.");
    }
    
    datasourceToIdMappingsMap = new HashMap<String, Map<String, String>>();
    
    // okay, now parse the rest of the config file, building a map of maps.
    while ((nextLine = reader.readNext()) != null) {
      // Grab the values.
      String datasourceName = nextLine[datasourceNameIdx];
      String fromStr = nextLine[fromIdx];
      String toStr = nextLine[toIdx];
      
      if (datasourceToIdMappingsMap.containsKey(datasourceName)) {
        datasourceToIdMappingsMap.get(datasourceName).put(fromStr, toStr);
      } else {
        Map<String, String> idMap = new HashMap<String, String>();
        idMap.put(fromStr, toStr);
        datasourceToIdMappingsMap.put(datasourceName, idMap);
      }
    }
    
    reader.close();
  }
}
