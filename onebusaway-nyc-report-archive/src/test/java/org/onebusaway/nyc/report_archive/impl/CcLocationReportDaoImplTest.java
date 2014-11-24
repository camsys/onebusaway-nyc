/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.report_archive.impl;

import static org.junit.Assert.*;

import org.onebusaway.nyc.report.model.CcLocationReportRecord;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.nyc.queue.model.RealtimeEnvelope;

import lrms_final_09_07.Angle;
import tcip_final_4_0_0.CcLocationReport;
import tcip_final_4_0_0.CPTOperatorIden;
import tcip_final_4_0_0.CPTVehicleIden;
import tcip_final_4_0_0.SCHRouteIden;
import tcip_final_4_0_0.SCHRunIden;
import tcip_final_4_0_0.SPDataQuality;
import tcip_4_0_0_local.NMEA;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;

public class CcLocationReportDaoImplTest {

  private SessionFactory _sessionFactory;

  private CcLocationReportDaoImpl _dao;

  @Before
  public void setup() throws IOException {

    Configuration config = new AnnotationConfiguration();
    config = config.configure("org/onebusaway/nyc/report_archive/hibernate-configuration.xml");
    _sessionFactory = config.buildSessionFactory();

    _dao = new CcLocationReportDaoImpl();
    _dao.setSessionFactory(_sessionFactory);

  }

  @After
  public void teardown() {
    if (_sessionFactory != null)
      _sessionFactory.close();
  }

  @Test
  public void testSave() {

    assertEquals(0, _dao.getNumberOfReports());

    CcLocationReportRecord report = new CcLocationReportRecord();
		report.setUUID("foo");
		report.setArchiveTimeReceived(new Date(122345l));
    report.setDataQuality(new Byte("4"));
    report.setDestSignCode(123);
    report.setDirectionDeg(new BigDecimal("10.1"));
    report.setLatitude(new BigDecimal("70.25"));
    report.setLongitude(new BigDecimal("-101.10"));
    report.setManufacturerData("manufacturerData");
    report.setOperatorIdDesignator("opIdDesignator");
    report.setRequestId(456);
    report.setRouteIdDesignator("rteIdDesignator");
    report.setRunIdDesignator("runIdDesignator");
    report.setSpeed(new BigDecimal("5.6"));
    report.setTimeReported(new Date(System.currentTimeMillis()));
    report.setTimeReceived(new Date(System.currentTimeMillis()));
    report.setVehicleAgencyDesignator("vehicleAgencyDesignator");
    report.setVehicleAgencyId(789);
    report.setVehicleId(120);
    report.setNmeaSentenceGPGGA("$GPRMC,105850.00,A,4038.445646,N,07401.094043,W,002.642,128.77,220611,,,A*7C");
    report.setNmeaSentenceGPRMC("$GPGGA,105850.000,4038.44565,N,07401.09404,W,1,09,01.7,+00042.0,M,,M,,*49");
    report.setRawMessage("This is a standin for the raw message");
    _dao.saveOrUpdateReport(report);
    assertEquals(1, _dao.getNumberOfReports());
  }

  @Test
  public void testConstructorNull() {
      // if deserialization fails, we can receive null object
      CcLocationReportRecord r = new CcLocationReportRecord(null, "contents", "zoneOffset");
      // no exception thrown
  }

  @Test
  public void testConstructor() {
			RealtimeEnvelope envelope = new RealtimeEnvelope();
			envelope.setUUID("foo");
			envelope.setTimeReceived(122345l);
      CcLocationReport m = new CcLocationReport();
			envelope.setCcLocationReport(m);
      m.setRequestId(1205);
      m.setDataQuality(new SPDataQuality());
      m.getDataQuality().setQualitativeIndicator("4");
      m.setDestSignCode(4631l);
      m.setDirection(new Angle());
      m.getDirection().setDeg(new BigDecimal(128.77));
      m.setLatitude(40640760);
      m.setLongitude(-74018234);
      m.setManufacturerData("VFTP123456789");
      m.setOperatorID(new CPTOperatorIden());
      m.getOperatorID().setOperatorId(0);
      m.getOperatorID().setDesignator("123456");
      m.setRequestId(1);
      m.setRouteID(new SCHRouteIden());
      m.getRouteID().setRouteId(0);
      m.getRouteID().setRouteDesignator("63");
      m.setRunID(new SCHRunIden());
      m.getRunID().setRunId(0);
      m.getRunID().setDesignator("1");
      m.setSpeed((short)36);
      m.setStatusInfo(0);
      m.setTimeReported("2011-06-22T10:58:10.0-00:00");
      m.setVehicle(new CPTVehicleIden());
      m.getVehicle().setAgencydesignator("MTA NYCT");
      m.getVehicle().setAgencyId(2008l);
      m.getVehicle().setVehicleId(2560);
      m.setLocalCcLocationReport(new tcip_4_0_0_local.CcLocationReport());
      m.getLocalCcLocationReport().setNMEA(new NMEA());
      m.getLocalCcLocationReport().getNMEA().getSentence().add("$GPRMC,105850.00,A,4038.445646,N,07401.094043,W,002.642,128.77,220611,,,A*7C");
      m.getLocalCcLocationReport().getNMEA().getSentence().add("$GPGGA,105850.000,4038.44565,N,07401.09404,W,1,09,01.7,+00042.0,M,,M,,*49");
      String contents = "TBD";
      CcLocationReportRecord r = new CcLocationReportRecord(envelope, contents, "-04:00");
      assertEquals((int)r.getRequestId(), (int)m.getRequestId());
      // todo test others
      for (String s : m.getLocalCcLocationReport().getNMEA().getSentence()) {
	  if (s.indexOf("$GPGGA") != -1) {
	      assertEquals(s, r.getNmeaSentenceGPGGA());
	  } else if (s.indexOf("$GPRMC") != -1) {
	      assertEquals(s, r.getNmeaSentenceGPRMC());
	  }
      }
  }

    @Test
    public void TestHandleException() {
	Throwable t = new Throwable();
	_dao.handleException("content", t, null);
	_dao.handleException("content2", t, new Date());
    }
}
