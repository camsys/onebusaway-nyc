package org.onebusaway.nyc.transit_data_manager.adapters.tools;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;

public class DepotIdTranslator {

  private static Logger _log = LoggerFactory.getLogger(DepotIdTranslator.class);
  
  private Map<String, Map<String, String>> datasourceToIdMappingsMap = null;

  public DepotIdTranslator(File configFile) throws IOException {
    _log.debug("Constructing DepotIdTranslator using config file: " + configFile.getPath());
    
    try {
      if (configFile.exists() && configFile.canRead()) {
        loadConfiguration(configFile);
      } else {
        throw new IOException("Config file " + configFile.getPath()
            + "does not exist or could not be read.");
      }
    } catch (IOException e) {
      _log.info("Failed to construct DepotIdTranslator with config file " + configFile.getPath() + " because " + e.getMessage() + ". Throwing exception.");
      throw e;
    }

  }

  private String getMappedIdOrNull(String dataSourceId, String fromId) {
    return datasourceToIdMappingsMap.get(dataSourceId).get(fromId);
  }

  public boolean hasMappingForId(String dataSourceId, String fromId) {
    String mappedIdOrNull = getMappedIdOrNull(dataSourceId, fromId);

    boolean hasMappingForId = mappedIdOrNull != null ? true : false;

    return hasMappingForId;
  }

  public String getMappedId(String dataSourceId, String fromId) {
    String mappedIdOrNull = getMappedIdOrNull(dataSourceId, fromId);

    String mappedId = mappedIdOrNull != null ? mappedIdOrNull : fromId;

    return mappedId;
  }

  private void loadConfiguration(File configFile) throws IOException {

    CSVReader reader = null;

    try {
      reader = new CSVReader(new FileReader(configFile));

      // Read in the header line of the file.
      String[] nextLine = reader.readNext();

      if (nextLine == null)
        throw new IOException("Could not parse config file or file is empty.");
      // We expect the header to contain datasource_name, from and to

      int datasourceNameIdx = -1;
      int fromIdx = -1;
      int toIdx = -1;

      boolean isConfigFileValid = true; // In the following code, set this to
                                        // false if somethings not right. Then
                                        // throw an exception later if it ends
                                        // up false.

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

      if (datasourceNameIdx == -1 || fromIdx == -1 || toIdx == -1) {
        isConfigFileValid = false;
      }

      if (!isConfigFileValid) {
        throw new IOException(
            "Config file does not have required columns, or has a duplicated column.");
      }
      
      // Add this to skip over empty lines and avoid potential index outoutbounds exception later.
      int maxColIdx = datasourceNameIdx > fromIdx ? datasourceNameIdx : fromIdx ;
      maxColIdx = maxColIdx > toIdx ? maxColIdx : toIdx ;

      datasourceToIdMappingsMap = new HashMap<String, Map<String, String>>();

      // okay, now parse the rest of the config file, building a map of maps.
      while ((nextLine = reader.readNext()) != null) {
        if (nextLine.length >= (maxColIdx + 1)) {  // Check that we don't have an empty line or a line that doesn't have enough values.
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
        } else { // Skip this line, Since we have an empty line or a line doesn't have enough values.
          continue;
        }
      }
    } finally {
      if (reader != null)
        try {
          reader.close();
        } catch (IOException e) {} // Not much we can do here...
    }

  }
}
