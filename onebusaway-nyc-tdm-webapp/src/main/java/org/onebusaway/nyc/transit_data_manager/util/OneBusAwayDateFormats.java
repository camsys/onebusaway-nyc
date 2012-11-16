package org.onebusaway.nyc.transit_data_manager.util;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class OneBusAwayDateFormats {

  public static DateTimeFormatter DATETIMEPATTERN_JSON_DATE_TIME = ISODateTimeFormat.dateTimeNoMillis();
  
  public static String DATETIMEPATTERN_DATE = "yyyy-MM-dd";
}
