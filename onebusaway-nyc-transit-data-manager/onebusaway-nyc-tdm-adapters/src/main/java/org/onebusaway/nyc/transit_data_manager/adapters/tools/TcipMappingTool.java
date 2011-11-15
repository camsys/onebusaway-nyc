package org.onebusaway.nyc.transit_data_manager.adapters.tools;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class TcipMappingTool {
  
  public TcipMappingTool() {
    super();
  }

  public static String MTA_NYCT_BUS_DESIGNATOR = "MTA NYCT";
  public static String MTA_BUS_CO_BUS_DESIGNATOR = "MTA BUS";
  public static String MTA_LI_BUS_BUS_DESIGNATOR = "MTA LI BUS";

  protected static long MTA_NYCT_AGENCY_ID = new Long(2008);
  protected static long MTA_BUS_CO_AGENCY_ID = new Long(2188);
  protected static long MTA_LI_BUS_AGENCY_ID = new Long(2007);
  
  public static DateTimeFormatter TCIP_DATEONLY_FORMATTER = ISODateTimeFormat.date();
  public static DateTimeFormatter TCIP_DATETIME_FORMATTER = ISODateTimeFormat.dateTimeNoMillis();
  public static DateTimeFormatter TCIP_TIMEONLY_FORMATTER = DateTimeFormat.forPattern("HH:mm:ss");
  
  // TODO: This method is the same as getVehicleDesignatorFromAgencyId in UtsMappingTool. 
  public String getJsonModelAgencyIdByTcipId(Long tcipId) {
    
    String vehDesignator;
    
    if (tcipId == MTA_NYCT_AGENCY_ID) {
      vehDesignator = MTA_NYCT_BUS_DESIGNATOR;
    } else if (tcipId == MTA_BUS_CO_AGENCY_ID) {
      vehDesignator = MTA_BUS_CO_BUS_DESIGNATOR;
    } else if (tcipId == MTA_LI_BUS_AGENCY_ID) {
      vehDesignator = MTA_LI_BUS_BUS_DESIGNATOR;
    } else {
      vehDesignator = null;
    }
    
    return vehDesignator;
    
  }
  
  public String cutRunNumberFromTcipRunDesignator(String input) {
    int dashIdx = input.indexOf("-") + 1;

    return input.substring(dashIdx);
  }
}
