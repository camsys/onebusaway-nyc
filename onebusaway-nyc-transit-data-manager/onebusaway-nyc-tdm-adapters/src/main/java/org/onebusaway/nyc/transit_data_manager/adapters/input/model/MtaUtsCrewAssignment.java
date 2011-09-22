package org.onebusaway.nyc.transit_data_manager.adapters.input.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.UtsMappingTool;

public class MtaUtsCrewAssignment {
  private UtsMappingTool mappingTool = null;

  public MtaUtsCrewAssignment() {
    mappingTool = new UtsMappingTool();
  }

  private String depotField;
  private String authIdField;
  private String passNumberField;
  private String routeField;
  private String runNumberField;
  private String servIdField;
  private String dateField;
  private String timestampField;

  public void setDepotField(String depotField) {
    this.depotField = depotField;
  }

  public void setAuthIdField(String authIdField) {
    this.authIdField = authIdField;
  }

  public void setPassNumberField(String passNumberField) {
    this.passNumberField = passNumberField;
    setPassNumber(this.passNumberField);
  }

  public void setRouteField(String routeField) {
    this.routeField = routeField;
  }

  public void setRunNumberField(String runNumberField) {
    this.runNumberField = runNumberField;
  }

  public void setServIdField(String servIdField) {
    this.servIdField = servIdField;
  }

  public void setDateField(String dateField) {
    this.dateField = dateField;
    setDate(this.dateField);
  }

  public void setTimestampField(String timestampField) {
    this.timestampField = timestampField;
    setTimestamp(this.timestampField);
  }

  private String passNumberLeadingLetters = ""; // Operator Pass #
  private Long passNumberNumericPortion; // Operator Pass #
  // private Boolean runNumberContainsLetters;
  // /* servId:
  // * 1 Weekday/School Open (DOB)
  // * 2 Weekday/School-Closed (DOB only)
  // * 3 Saturday
  // * 4 Sunday
  // * 5 Holiday (DOB Only)
  // * 0 Weekday/both School Open & School-Closed (DOB)
  // * 6 Other Holiday Service
  // * 7 Other Holiday Service
  // * 8 Other Holiday Service
  // * 9 Other Holiday Service
  // * A Other Holiday Service
  // * B Other Holiday Service
  // */
  private DateTime date; // Service Date
  private DateTime timestamp; // Assignment Timestamp

  public void setTimestamp(String value) {
    DateTime parsedDate = null;

    DateTimeFormatter dtf = DateTimeFormat.forPattern(UtsMappingTool.UTS_TIMESTAMP_FIELD_DATEFORMAT);

    parsedDate = dtf.parseDateTime(value);

    timestamp = parsedDate;
  }

  public void setDate(String value) {
    DateTime parsedDate = null;

    DateTimeFormatter dtf = DateTimeFormat.forPattern(UtsMappingTool.UTS_DATE_FIELD_DATEFORMAT);

    parsedDate = dtf.parseDateTime(value);

    DateMidnight dm = new DateMidnight(parsedDate);

    date = new DateTime(dm);
  }

  public void setPassNumber(String value) {
    try {
      passNumberNumericPortion = Long.parseLong(value);
    } catch (NumberFormatException nfea) {
      Pattern lettersNumbersPattern = Pattern.compile("^(\\D*)(\\d+)$");
      Matcher matcher = lettersNumbersPattern.matcher(value);
      if (matcher.find()) {
        passNumberLeadingLetters = matcher.group(1); // The leading letter(s)
        String passNumberNumStr = matcher.group(2); // the Number

        try {
          passNumberNumericPortion = Long.parseLong(passNumberNumStr);
        } catch (NumberFormatException nfeb) {
          System.out.println("Exception trying to parse " + passNumberNumStr);
          nfeb.printStackTrace();
        }
      } else {
        passNumberNumericPortion = new Long(-1);
      }
    }
  }

  public String getAuthId() {
    return authIdField;
  }

  public DateTime getDate() {
    return date;
  }

  public String getDepot() {
    return depotField;
  }

  public String getRunNumber() {
    return runNumberField;
  }

  public String getRoute() {
    return routeField;
  }

  public Long getPassNumberNumericPortion() {
    return passNumberNumericPortion;
  }

  public DateTime getTimestamp() {
    return timestamp;
  }

  public String getOperatorDesignator() {
    return authIdField + passNumberField;
  }

  public String getRunDesignator() {
    return routeField + "-" + runNumberField;
  }

}
