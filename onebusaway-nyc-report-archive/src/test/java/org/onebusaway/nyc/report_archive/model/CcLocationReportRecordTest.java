package org.onebusaway.nyc.report_archive.model;

import static org.junit.Assert.*;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import org.junit.Test;

import java.util.Date;

public class CcLocationReportRecordTest {


  @Test
  public void testConvertTime() {
    // time reported may be invalid from bus on startup, test logic that 
    // corrects it
    String badTimeStr = "2011-10-15T03:26:19.000"; // missing timezone!
    String offset = "-04:00";
    CcLocationReportRecord cc = new CcLocationReportRecord();
    DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
    formatter.withZone(DateTimeZone.UTC);

    assertEquals(new Date(formatter.parseDateTime("2011-10-15T03:26:19.000-04:00").getMillis()), cc.convertTime(badTimeStr, offset));

    offset = "-05:00";
    assertEquals(new Date(formatter.parseDateTime("2011-10-15T03:26:19.000-05:00").getMillis()), cc.convertTime(badTimeStr, offset));
  }    

}