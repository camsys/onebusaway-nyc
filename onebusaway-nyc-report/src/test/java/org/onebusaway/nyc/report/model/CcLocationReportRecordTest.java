package org.onebusaway.nyc.report.model;

import static org.junit.Assert.*;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;
import org.onebusaway.nyc.queue.model.RealtimeEnvelope;
import org.onebusaway.nyc.report.model.CcLocationReportRecord;

import java.math.BigDecimal;
import java.util.Date;

import lrms_final_09_07.Angle;
import tcip_final_3_0_5_1.CcLocationReport;
import tcip_final_3_0_5_1.CPTOperatorIden;
import tcip_final_3_0_5_1.CPTVehicleIden;
import tcip_final_3_0_5_1.SCHRouteIden;
import tcip_final_3_0_5_1.SCHRunIden;
import tcip_final_3_0_5_1.SPDataQuality;
import tcip_3_0_5_local.NMEA;

public class CcLocationReportRecordTest {


  @Test
  public void testConstructor() {
    RealtimeEnvelope r = new RealtimeEnvelope();
    CcLocationReport c = new CcLocationReport();
    r.setCcLocationReport(c);
    String rmc = "$GPRMC,195108.00,A,4039.604859,N,07400.109146,W,043.033,219.87,230911,,,A*72";
    String gga = "$GPGGA,195108.000,4039.60486,N,07400.10915,W,1,09,00.9,+00023.0,M,,M,,*43";
    c.setDestSignCode(4631l);
    c.setDirection(new Angle());
    c.getDirection().setDeg(new BigDecimal(128.77));
    c.setOperatorID(new CPTOperatorIden());
    
    c.setRouteID(new SCHRouteIden());
    c.setRunID(new SCHRunIden());
    c.setVehicle(new CPTVehicleIden());
    c.getVehicle().setAgencydesignator("MTA NYCT");
    c.getVehicle().setAgencyId(2008l);
    c.getVehicle().setVehicleId(2560);

    c.setLocalCcLocationReport(new tcip_3_0_5_local.CcLocationReport());
    c.getLocalCcLocationReport().setNMEA(new NMEA());
    c.getLocalCcLocationReport().getNMEA().getSentence().add(rmc);
    c.getLocalCcLocationReport().getNMEA().getSentence().add(gga);
    CcLocationReportRecord ccrr = new CcLocationReportRecord(r, null, null);
    assertEquals(gga, ccrr.getNmeaSentenceGPGGA());
    assertEquals(rmc, ccrr.getNmeaSentenceGPRMC());
  }


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