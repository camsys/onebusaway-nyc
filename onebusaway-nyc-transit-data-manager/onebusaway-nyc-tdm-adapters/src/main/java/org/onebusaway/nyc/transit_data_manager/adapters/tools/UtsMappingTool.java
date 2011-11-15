package org.onebusaway.nyc.transit_data_manager.adapters.tools;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.MutableDateTime;
import org.joda.time.ReadableDateTime;
import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class UtsMappingTool {
  public static String UTS_DATE_FIELD_DATEFORMAT = "yyyy-MM-dd";
  public static String UTS_TIMESTAMP_FIELD_DATEFORMAT = "yyyy-MM-dd HH:mm:ss";

  public static String TIMEFORMAT_HHMMSS = "HH:mm:ss";

  public static String MTA_NYCT_BUS_DESIGNATOR = "MTA NYCT";
  public static String MTA_BUS_CO_BUS_DESIGNATOR = "MTA BUS CO";

  private static long MTA_NYCT_AGENCY_ID = new Long(2008);
  private static long MTA_BUS_CO_AGENCY_ID = new Long(2188);

  public UtsMappingTool() {
    super();
  }

  public Long getAgencyIdFromDepotAssignAgency(int value) {
    return MTA_BUS_CO_AGENCY_ID;
  }

  public Long getAgencyIdFromUtsAuthId(String authId) {
    Long agencyId = new Long(-1);

    if ("TA".equals(authId) || "OA".equals(authId)) { // TA and OA indicate MTA NYCT (2008)
      agencyId = MTA_NYCT_AGENCY_ID;
    } else if ("RB".equals(authId)) { // RB indicates MTA Bus Company (2188)
      agencyId = MTA_BUS_CO_AGENCY_ID;
    }

    return agencyId;
  }
  
  public String getVehicleDesignatorFromAgencyId(Long agencyId) {
    String vehDesignator;
    
    if (agencyId == MTA_NYCT_AGENCY_ID) {
      vehDesignator = MTA_NYCT_BUS_DESIGNATOR;
    } else if (agencyId == MTA_BUS_CO_AGENCY_ID) {
      vehDesignator = MTA_BUS_CO_BUS_DESIGNATOR;
    } else {
      vehDesignator = null;
    }
    
    return vehDesignator;
  }

  // These methods use the Joda DateTime class
  public String dateDateTimeToDateString(DateTime dt) {
    DateTimeFormatter dtf = DateTimeFormat.forPattern(UTS_DATE_FIELD_DATEFORMAT);
    return dtf.print(dt);
  }

  public String timestampDateTimeToDateString(DateTime dt) {
    DateTimeFormatter dtf = DateTimeFormat.forPattern(UTS_TIMESTAMP_FIELD_DATEFORMAT);
    return dtf.print(dt);
  }

  public ReadableDateTime parseUtsDateToDateTime(String dateStr,
      String formatStr) {
    ReadableDateTime result = null;

    DateTimeFormatter dtf = DateTimeFormat.forPattern(formatStr);
    DateTime date = dtf.parseDateTime(dateStr);

    if (UTS_DATE_FIELD_DATEFORMAT.equals(formatStr)) {
      DateMidnight dm = new DateMidnight(date);
      date = new DateTime(dm);
    } else if (UTS_TIMESTAMP_FIELD_DATEFORMAT.equals(formatStr)) {
      result = date;
    }

    return result;
  }

  /***
   * This function is designed to calculate the actual date of a pull in or out
   * based on both the input time string (with a suffix of A,P,B,X) and a base
   * date (likely the service date). The base date is in 24 hour format, so I
   * think we can safely assume that the AM/PM aspect is uneccessary, only that
   * the time is the same day, the day before or the day after is needed.
   * 
   * The possible suffixes are as follows: A - The time is AM, of the same date
   * as the baseDate P - The time is PM, of the same date as the baseDate B -
   * The time is PM, the date before the baseDate X - The time is AM, the date
   * after the baseDate
   * 
   * @param baseDate The reference date that the time suffix refers to.
   * @param suffixedTime A time in the format 02:53Z, where Z is a suffix
   *          representing one of four possible half days.
   * @return A DateTime with the date incorporating the suffix, or null if the
   *         suffixedTime string didn't match the input format.
   */
  public DateTime calculatePullInOutDateFromDateUtsSuffixedTime(
      DateTime baseDate, String suffixedTime) {
    DateTime result = null;

    Pattern timeSuffixPattern = Pattern.compile("(.{2}):(.{2})([APBX])");
    Matcher timeSuffixMatcher = timeSuffixPattern.matcher(suffixedTime);

    int hours, minutes;
    String suffix = null;

    if (timeSuffixMatcher.find()) {
      MutableDateTime modDate = baseDate.toMutableDateTime();

      hours = Integer.parseInt(timeSuffixMatcher.group(1));
      minutes = Integer.parseInt(timeSuffixMatcher.group(2));
      suffix = timeSuffixMatcher.group(3);

      modDate.setHourOfDay(hours);
      modDate.setMinuteOfHour(minutes);

      if ("B".equals(suffix)) { // suffixedTime is the day before the baseDate
        modDate.addDays(-1);
      } else if ("X".equals(suffix)) { // suffixedTime is the day after the
                                       // basedate
        modDate.addDays(1);
      } // if AM or PM, do nothing.

      result = modDate.toDateTime();
    } else {
      // What do we do when the time parsing fails - should throw an exception
      // here.
    }

    return result;
  }

  public String getJsonModelAgencyIdByTcipId(Long tcipId) {
    return "MTA NYCT"; // Hard coded for now.
  }

  public String cutRunNumberFromTcipRunDesignator(String input) {
    int dashIdx = input.indexOf("-") + 1;

    return input.substring(dashIdx);
  }

  public String dateTimeToXmlDatetimeFormat(ReadableInstant input) {
    DateTimeFormatter xmlDTF = ISODateTimeFormat.dateTimeNoMillis();
    return xmlDTF.print(input);
  }

  public String parseRunNumFromBlockDesignator(String blockDes) {
    // Example Block designator (blockDes)
    // -> CSLT_S56_001_06012011_0459
    // where the values are depot, run route, run number, service date,
    // scheduled pull-out time

    int firstBar = blockDes.indexOf("_");

    int secondBar = blockDes.indexOf("_", firstBar + 1);

    return blockDes.substring(firstBar, secondBar);
  }

}
