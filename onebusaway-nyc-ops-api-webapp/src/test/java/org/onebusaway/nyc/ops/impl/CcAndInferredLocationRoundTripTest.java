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
package org.onebusaway.nyc.ops.impl;

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
import org.onebusaway.nyc.report.model.CcAndInferredLocationRecord;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Characterization test: saves one fully populated {@link CcAndInferredLocationRecord}
 * (the obanyc_last_known_vehicle row the ops API serves) through
 * {@link CcAndInferredLocationDaoImpl} into in-memory HSQLDB, then reloads it with the
 * DAO's getLastKnownRecordForVehicle and checks every mapped column.
 *
 * Why: this pins how the entity round-trips through Hibernate 5.2 on Java 11 today, so
 * the later Hibernate 5.6 / Java 21 upgrade has a baseline. Covered: the assigned
 * (vehicle id) primary key, BigDecimal into DECIMAL(9,6), (5,2) and (4,1) columns,
 * Double into DECIMAL(14,6), a BIT boolean, ISO timestamps that this entity stores as
 * strings, non-ASCII text and null nullable columns.
 *
 * Note: on Java 21 the existing CcAndInferredLocationDaoImplTest fails while building the
 * session factory (Hibernate tuplizer issue). This test builds the factory the same way,
 * so expect it to fail the same way there until the Hibernate upgrade lands.
 */
public class CcAndInferredLocationRoundTripTest {

  private static final String NON_ASCII = "Caf\u00e9 \u00d1and\u00fa";

  private static final Integer VEHICLE_ID = Integer.valueOf(7582);

  private SessionFactory _sessionFactory;

  private CcAndInferredLocationDaoImpl _dao;

  @Before
  public void setup() throws IOException {
    Configuration config = new Configuration();
    config = config.configure("org/onebusaway/nyc/ops/hibernate-configuration.xml");
    _sessionFactory = config.buildSessionFactory();

    _dao = new CcAndInferredLocationDaoImpl();
    _dao.setValidationService(new RecordValidationServiceImpl());
    _dao.setSessionFactory(_sessionFactory);
    _dao.setCcLocationCache(new CcLocationCache(10));
  }

  @After
  public void teardown() {
    if (_sessionFactory != null)
      _sessionFactory.close();
  }

  @Test
  public void ccAndInferredLocationRecordRoundTrips() throws Exception {
    Transaction t = getSession().beginTransaction();

    CcAndInferredLocationRecord saved = new CcAndInferredLocationRecord();
    saved.setUUID("8f14e45f-ceea-467a-9575-2b6f1b8e0d11");
    saved.setVehicleId(VEHICLE_ID);
    saved.setVehicleAgencyId(2008);
    // These three are ISO strings in the entity, not dates; pinned as opaque text.
    saved.setTimeReported("2026-10-27T10:55:12.345-04:00");
    saved.setTimeReceived("2026-10-27T10:55:13.346-04:00");
    saved.setArchiveTimeReceived("2026-10-27T10:55:14.347-04:00");
    saved.setOperatorIdDesignator("123456");
    saved.setRouteIdDesignator("B44+");
    saved.setRunIdDesignator("B44-101");
    saved.setDestSignCode(4631);
    // Nullable column left null on purpose.
    saved.setEmergencyCode(null);
    saved.setLatitude(new BigDecimal("40.640760"));
    saved.setLongitude(new BigDecimal("-74.018234"));
    saved.setSpeed(new BigDecimal("36.5"));
    saved.setDirectionDeg(new BigDecimal("128.77"));
    saved.setAgencyId("MTA NYCT");
    saved.setDepotId("FB");
    saved.setServiceDate("2026-10-27");
    saved.setInferredOperatorId("123456");
    saved.setInferredRunId("B44-101");
    saved.setInferredBlockId("MTA NYCT_FB_A6-Weekday-SDon_E_FB_26580_B4-101");
    saved.setInferredTripId("MTA NYCT_FB_A6-Weekday-SDon-044900_B4_101");
    saved.setInferredRouteId("MTA NYCT_B44+");
    saved.setInferredDirectionId("1");
    saved.setInferredDestSignCode(4632);
    // Fewer digits than the DECIMAL(9,6) scale: pins that reload pads to scale 6.
    saved.setInferredLatitude(new BigDecimal("40.6408"));
    saved.setInferredLongitude(new BigDecimal("-74.018200"));
    saved.setInferredPhase(NON_ASCII);
    saved.setInferredStatus("IN_PROGRESS");
    saved.setInferenceIsFormal(true);
    // Binary-exact doubles: DECIMAL(14,6) rounding is not what is being pinned here.
    saved.setDistanceAlongBlock(12345.5);
    saved.setDistanceAlongTrip(678.25);
    saved.setNextScheduledStopId("MTA_303241");
    saved.setNextScheduledStopDistance(42.75);
    // Nullable columns left null on purpose.
    saved.setPreviousScheduledStopId(null);
    saved.setPreviousScheduledStopDistance(null);
    saved.setScheduleDeviation(-120);
    saved.setAssignedRunId("B44-102");
    saved.setAssignedBlockId("MTA NYCT_FB_A6-Weekday-SDon_E_FB_26580_B4-102");

    // DAO does saveOrUpdate, flush and clear, so the reload below has to hit the database.
    _dao.saveOrUpdateRecord(saved);

    CcAndInferredLocationRecord reloaded = _dao.getLastKnownRecordForVehicle(VEHICLE_ID);
    assertNotNull(reloaded);
    assertNotSame(saved, reloaded);

    assertEquals("8f14e45f-ceea-467a-9575-2b6f1b8e0d11", reloaded.getUUID());
    assertEquals(VEHICLE_ID, reloaded.getVehicleId());
    assertEquals(Integer.valueOf(2008), reloaded.getVehicleAgencyId());
    assertEquals("2026-10-27T10:55:12.345-04:00", reloaded.getTimeReported());
    assertEquals("2026-10-27T10:55:13.346-04:00", reloaded.getTimeReceived());
    assertEquals("2026-10-27T10:55:14.347-04:00", reloaded.getArchiveTimeReceived());
    assertEquals("123456", reloaded.getOperatorIdDesignator());
    assertEquals("B44+", reloaded.getRouteIdDesignator());
    assertEquals("B44-101", reloaded.getRunIdDesignator());
    assertEquals(Integer.valueOf(4631), reloaded.getDestSignCode());
    assertNull(reloaded.getEmergencyCode());
    assertEquals(new BigDecimal("40.640760"), reloaded.getLatitude());
    assertEquals(new BigDecimal("-74.018234"), reloaded.getLongitude());
    assertEquals(new BigDecimal("36.5"), reloaded.getSpeed());
    assertEquals(new BigDecimal("128.77"), reloaded.getDirectionDeg());
    assertEquals("MTA NYCT", reloaded.getAgencyId());
    assertEquals("FB", reloaded.getDepotId());
    assertEquals("2026-10-27", reloaded.getServiceDate());
    assertEquals("123456", reloaded.getInferredOperatorId());
    assertEquals("B44-101", reloaded.getInferredRunId());
    assertEquals("MTA NYCT_FB_A6-Weekday-SDon_E_FB_26580_B4-101", reloaded.getInferredBlockId());
    assertEquals("MTA NYCT_FB_A6-Weekday-SDon-044900_B4_101", reloaded.getInferredTripId());
    assertEquals("MTA NYCT_B44+", reloaded.getInferredRouteId());
    assertEquals("1", reloaded.getInferredDirectionId());
    assertEquals(Integer.valueOf(4632), reloaded.getInferredDestSignCode());
    assertEquals(new BigDecimal("40.640800"), reloaded.getInferredLatitude());
    assertEquals(new BigDecimal("-74.018200"), reloaded.getInferredLongitude());
    assertEquals(NON_ASCII, reloaded.getInferredPhase());
    assertEquals("IN_PROGRESS", reloaded.getInferredStatus());
    assertTrue(reloaded.isInferenceIsFormal());
    assertEquals(Double.valueOf(12345.5), reloaded.getDistanceAlongBlock());
    assertEquals(Double.valueOf(678.25), reloaded.getDistanceAlongTrip());
    assertEquals("MTA_303241", reloaded.getNextScheduledStopId());
    assertEquals(Double.valueOf(42.75), reloaded.getNextScheduledStopDistance());
    assertNull(reloaded.getPreviousScheduledStopId());
    assertNull(reloaded.getPreviousScheduledStopDistance());
    assertEquals(Integer.valueOf(-120), reloaded.getScheduleDeviation());
    assertEquals("B44-102", reloaded.getAssignedRunId());
    assertEquals("MTA NYCT_FB_A6-Weekday-SDon_E_FB_26580_B4-102", reloaded.getAssignedBlockId());

    t.commit();
  }

  private Session getSession() {
    return _sessionFactory.getCurrentSession();
  }
}
