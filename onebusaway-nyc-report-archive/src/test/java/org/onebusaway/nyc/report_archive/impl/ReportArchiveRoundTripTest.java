/**
 * Copyright (C) 2026 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.nyc.report_archive.impl;

import static org.junit.Assert.*;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.nyc.report.impl.CcLocationCache;
import org.onebusaway.nyc.report.impl.RecordValidationServiceImpl;
import org.onebusaway.nyc.report.model.ArchivedInferredLocationRecord;
import org.onebusaway.nyc.report.model.CcLocationReportRecord;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Characterization test: saves one fully populated {@link ArchivedInferredLocationRecord}
 * and one {@link CcLocationReportRecord} through the real archive DAOs into in-memory
 * HSQLDB, clears the session, reloads by id and checks every mapped column.
 *
 * Why: this pins how these entities round-trip through Hibernate 5.2 on Java 11 today,
 * so the later Hibernate 5.6 / Java 21 upgrade has a baseline. A change in type mapping
 * (java.util.Date without @Temporal, Double into DECIMAL columns, BIT booleans, Byte,
 * the @Lob raw message), in timestamp precision (millis), in BigDecimal scale handling,
 * or in the Java type handed back on reload would fail here.
 */
public class ReportArchiveRoundTripTest {

  // Fixed instant with non-zero millis so a precision loss shows up.
  private static final long FIXED_MILLIS = 1793112912345L;

  private static final String NON_ASCII = "Caf\u00e9 \u00d1and\u00fa";

  private SessionFactory _sessionFactory;

  private ArchiveCcAndInferredLocationDaoImpl _inferredDao;

  private CcLocationReportDaoImpl _ccDao;

  @Before
  public void setup() throws IOException {
    Configuration config = new Configuration();
    config = config.configure("org/onebusaway/nyc/report_archive/hibernate-configuration.xml");
    _sessionFactory = config.buildSessionFactory();

    // Empty cache: findRealtimeRecord misses, so saveOrUpdateRecord writes only the
    // archived inferred row and no obanyc_last_known_vehicle row.
    _inferredDao = new ArchiveCcAndInferredLocationDaoImpl();
    _inferredDao.setValidationService(new RecordValidationServiceImpl());
    _inferredDao.setSessionFactory(_sessionFactory);
    _inferredDao.setCcLocationCache(new CcLocationCache(10));

    _ccDao = new CcLocationReportDaoImpl();
    _ccDao.setSessionFactory(_sessionFactory);
  }

  @After
  public void teardown() {
    if (_sessionFactory != null)
      _sessionFactory.close();
  }

  @Test
  public void archivedInferredLocationRecordRoundTrips() {
    Transaction t = getSession().beginTransaction();

    ArchivedInferredLocationRecord saved = new ArchivedInferredLocationRecord();
    saved.setUUID("8f14e45f-ceea-467a-9575-2b6f1b8e0d11");
    saved.setTimeReported(new Date(FIXED_MILLIS));
    saved.setVehicleId(7582);
    saved.setAgencyId("MTA NYCT");
    saved.setArchiveTimeReceived(new Date(FIXED_MILLIS + 1001L));
    saved.setServiceDate(new Date(FIXED_MILLIS + 2002L));
    saved.setScheduleDeviation(-120);
    saved.setInferredBlockId("MTA NYCT_FB_A6-Weekday-SDon_E_FB_26580_B4-101");
    saved.setInferredTripId("MTA NYCT_FB_A6-Weekday-SDon-044900_B4_101");
    // Binary-exact doubles: DECIMAL(14,6) rounding is not what is being pinned here.
    saved.setDistanceAlongBlock(12345.5);
    saved.setDistanceAlongTrip(678.25);
    // Fewer digits than the DECIMAL(9,6) scale: pins that reload pads to scale 6.
    saved.setInferredLatitude(new BigDecimal("40.7128"));
    saved.setInferredLongitude(new BigDecimal("-73.935242"));
    saved.setInferredPhase(NON_ASCII);
    saved.setInferredStatus("IN_PROGRESS");
    saved.setInferredRouteId("MTA NYCT_B44+");
    // Nullable column left null on purpose.
    saved.setInferredDirectionId(null);
    saved.setLastUpdateTime(FIXED_MILLIS + 3003L);
    saved.setLastLocationUpdateTime(FIXED_MILLIS + 4004L);
    saved.setInferredDestSignCode(4631);
    saved.setInferenceIsFormal(true);
    saved.setEmergencyFlag(false);
    saved.setDepotId("FB");
    saved.setInferredOperatorId("123456");
    saved.setInferredRunId("B44-101");
    saved.setNextScheduledStopId("MTA_303241");
    saved.setNextScheduledStopDistance(42.75);
    saved.setPreviousScheduledStopId("MTA_303240");
    saved.setPreviousScheduledStopDistance(310.125);
    saved.setAssignedRunId("B44-102");
    saved.setAssignedBlockId("MTA NYCT_FB_A6-Weekday-SDon_E_FB_26580_B4-102");

    // DAO does saveOrUpdate, flush and clear, so the get below has to hit the database.
    _inferredDao.saveOrUpdateRecord(saved);
    Long id = saved.getId();
    assertNotNull(id);

    ArchivedInferredLocationRecord reloaded =
        (ArchivedInferredLocationRecord) getSession().get(ArchivedInferredLocationRecord.class, id);
    assertNotNull(reloaded);
    assertNotSame(saved, reloaded);

    assertEquals(id, reloaded.getId());
    assertEquals("8f14e45f-ceea-467a-9575-2b6f1b8e0d11", reloaded.getUUID());
    assertDateRoundTrip(FIXED_MILLIS, reloaded.getTimeReported());
    assertEquals(Integer.valueOf(7582), reloaded.getVehicleId());
    assertEquals("MTA NYCT", reloaded.getAgencyId());
    assertDateRoundTrip(FIXED_MILLIS + 1001L, reloaded.getArchiveTimeReceived());
    // No @Temporal on serviceDate, so it is a full timestamp, not a DATE.
    assertDateRoundTrip(FIXED_MILLIS + 2002L, reloaded.getServiceDate());
    assertEquals(Integer.valueOf(-120), reloaded.getScheduleDeviation());
    assertEquals("MTA NYCT_FB_A6-Weekday-SDon_E_FB_26580_B4-101", reloaded.getInferredBlockId());
    assertEquals("MTA NYCT_FB_A6-Weekday-SDon-044900_B4_101", reloaded.getInferredTripId());
    assertEquals(Double.valueOf(12345.5), reloaded.getDistanceAlongBlock());
    assertEquals(Double.valueOf(678.25), reloaded.getDistanceAlongTrip());
    assertEquals(new BigDecimal("40.712800"), reloaded.getInferredLatitude());
    assertEquals(new BigDecimal("-73.935242"), reloaded.getInferredLongitude());
    assertEquals(NON_ASCII, reloaded.getInferredPhase());
    assertEquals("IN_PROGRESS", reloaded.getInferredStatus());
    assertEquals("MTA NYCT_B44+", reloaded.getInferredRouteId());
    assertNull(reloaded.getInferredDirectionId());
    assertEquals(FIXED_MILLIS + 3003L, reloaded.getLastUpdateTime());
    assertEquals(FIXED_MILLIS + 4004L, reloaded.getLastLocationUpdateTime());
    assertEquals(Integer.valueOf(4631), reloaded.getInferredDestSignCode());
    assertTrue(reloaded.isInferenceIsFormal());
    assertFalse(reloaded.isEmergencyFlag());
    assertEquals("FB", reloaded.getDepotId());
    assertEquals("123456", reloaded.getInferredOperatorId());
    assertEquals("B44-101", reloaded.getInferredRunId());
    assertEquals("MTA_303241", reloaded.getNextScheduledStopId());
    assertEquals(Double.valueOf(42.75), reloaded.getNextScheduledStopDistance());
    assertEquals("MTA_303240", reloaded.getPreviousScheduledStopId());
    assertEquals(Double.valueOf(310.125), reloaded.getPreviousScheduledStopDistance());
    assertEquals("B44-102", reloaded.getAssignedRunId());
    assertEquals("MTA NYCT_FB_A6-Weekday-SDon_E_FB_26580_B4-102", reloaded.getAssignedBlockId());

    t.commit();
  }

  @Test
  public void ccLocationReportRecordRoundTrips() {
    Transaction t = getSession().beginTransaction();

    CcLocationReportRecord saved = new CcLocationReportRecord();
    saved.setUUID("c9f0f895-fb98-4b91-8f2a-3d3b6e2a7c42");
    saved.setRequestId(1205);
    saved.setVehicleId(2560);
    saved.setVehicleAgencyId(2008);
    saved.setVehicleAgencyDesignator(NON_ASCII);
    saved.setTimeReported(new Date(FIXED_MILLIS));
    saved.setTimeReceived(new Date(FIXED_MILLIS + 1001L));
    saved.setArchiveTimeReceived(new Date(FIXED_MILLIS + 2002L));
    saved.setLatitude(new BigDecimal("40.640760"));
    saved.setLongitude(new BigDecimal("-74.018234"));
    saved.setDirectionDeg(new BigDecimal("128.77"));
    saved.setSpeed(new BigDecimal("36.5"));
    saved.setDataQuality(Byte.valueOf((byte) 4));
    saved.setManufacturerData("VFTP123456789");
    saved.setOperatorIdDesignator("123456");
    saved.setRunIdDesignator("63-101");
    saved.setDestSignCode(4631);
    // Nullable column left null on purpose.
    saved.setEmergencyCode(null);
    saved.setRouteIdDesignator("63");
    saved.setNmeaSentenceGPGGA("$GPGGA,105850.000,4038.44565,N,07401.09404,W,1,09,01.7,+00042.0,M,,M,,*49");
    saved.setNmeaSentenceGPRMC("$GPRMC,105850.00,A,4038.445646,N,07401.094043,W,002.642,128.77,220611,,,A*7C");
    // @Lob column: non-ASCII here pins CLOB character handling.
    saved.setRawMessage("{\"ccLocationReport\":{\"note\":\"" + NON_ASCII + "\"}}");
    saved.setVehiclePowerState(1);

    _ccDao.saveOrUpdateReport(saved);
    Long id = saved.getId();
    assertNotNull(id);

    CcLocationReportRecord reloaded =
        (CcLocationReportRecord) getSession().get(CcLocationReportRecord.class, id);
    assertNotNull(reloaded);
    assertNotSame(saved, reloaded);

    assertEquals(id, reloaded.getId());
    assertEquals("c9f0f895-fb98-4b91-8f2a-3d3b6e2a7c42", reloaded.getUUID());
    assertEquals(Integer.valueOf(1205), reloaded.getRequestId());
    assertEquals(Integer.valueOf(2560), reloaded.getVehicleId());
    assertEquals(Integer.valueOf(2008), reloaded.getVehicleAgencyId());
    assertEquals(NON_ASCII, reloaded.getVehicleAgencyDesignator());
    assertDateRoundTrip(FIXED_MILLIS, reloaded.getTimeReported());
    assertDateRoundTrip(FIXED_MILLIS + 1001L, reloaded.getTimeReceived());
    assertDateRoundTrip(FIXED_MILLIS + 2002L, reloaded.getArchiveTimeReceived());
    assertEquals(new BigDecimal("40.640760"), reloaded.getLatitude());
    assertEquals(new BigDecimal("-74.018234"), reloaded.getLongitude());
    assertEquals(new BigDecimal("128.77"), reloaded.getDirectionDeg());
    assertEquals(new BigDecimal("36.5"), reloaded.getSpeed());
    assertEquals(Byte.valueOf((byte) 4), reloaded.getDataQuality());
    assertEquals("VFTP123456789", reloaded.getManufacturerData());
    assertEquals("123456", reloaded.getOperatorIdDesignator());
    assertEquals("63-101", reloaded.getRunIdDesignator());
    assertEquals(Integer.valueOf(4631), reloaded.getDestSignCode());
    assertNull(reloaded.getEmergencyCode());
    assertEquals("63", reloaded.getRouteIdDesignator());
    assertEquals("$GPGGA,105850.000,4038.44565,N,07401.09404,W,1,09,01.7,+00042.0,M,,M,,*49",
        reloaded.getNmeaSentenceGPGGA());
    assertEquals("$GPRMC,105850.00,A,4038.445646,N,07401.094043,W,002.642,128.77,220611,,,A*7C",
        reloaded.getNmeaSentenceGPRMC());
    assertEquals("{\"ccLocationReport\":{\"note\":\"" + NON_ASCII + "\"}}", reloaded.getRawMessage());
    assertEquals(Integer.valueOf(1), reloaded.getVehiclePowerState());

    t.commit();
  }

  /**
   * Checks the millisecond value survives and that Hibernate hands back a
   * java.sql.Timestamp, which is what an unannotated java.util.Date maps to today.
   */
  private static void assertDateRoundTrip(long expectedMillis, Date actual) {
    assertNotNull(actual);
    assertEquals(Timestamp.class, actual.getClass());
    assertEquals(expectedMillis, actual.getTime());
  }

  private Session getSession() {
    return _sessionFactory.getCurrentSession();
  }
}
