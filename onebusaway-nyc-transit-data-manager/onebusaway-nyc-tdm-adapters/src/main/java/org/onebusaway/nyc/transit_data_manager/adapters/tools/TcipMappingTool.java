package org.onebusaway.nyc.transit_data_manager.adapters.tools;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class TcipMappingTool {

  public static String MTA_NYCT_BUS_DESIGNATOR = "MTA NYCT";
  public static String MTA_BUS_CO_BUS_DESIGNATOR = "MTA BUS CO";

  protected static long MTA_NYCT_AGENCY_ID = new Long(2008);
  protected static long MTA_BUS_CO_AGENCY_ID = new Long(2188);
  
  public static DateTimeFormatter TCIP_DATEONLY_FORMATTER = ISODateTimeFormat.date();
  public static DateTimeFormatter TCIP_TIMEONLY_FORMATTER = DateTimeFormat.forPattern("HH:mm:ss");
}
