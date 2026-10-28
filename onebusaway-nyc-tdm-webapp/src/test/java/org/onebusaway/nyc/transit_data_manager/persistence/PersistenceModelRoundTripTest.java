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
package org.onebusaway.nyc.transit_data_manager.persistence;

import static org.junit.Assert.*;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.nyc.transit_data_manager.persistence.model.CrewAssignmentRecord;
import org.onebusaway.nyc.transit_data_manager.persistence.model.DepotRecord;
import org.onebusaway.nyc.transit_data_manager.persistence.model.VehiclePipoRecord;

/**
 * Characterization test for the TDM archive entities in the persistence.model package:
 * saves one fully populated {@link VehiclePipoRecord}, {@link CrewAssignmentRecord}
 * and {@link DepotRecord} into in-memory HSQLDB, clears the session, reloads by id and
 * checks every mapped column.
 *
 * Why: this pins how these entities round-trip through Hibernate 5.2 on Java 11 today,
 * so the later Hibernate 5.6 / Java 21 upgrade has a baseline. The interesting parts are
 * the two temporal mappings: TemporalType.DATE columns drop the time of day (reloaded as
 * local midnight, java.sql.Date) while TemporalType.TIMESTAMP columns keep millis
 * (reloaded as java.sql.Timestamp). A change in either would fail here.
 *
 * Save and reload go straight through the Hibernate session: the production writer,
 * VehicleAndCrewDataPersistenceServiceImpl, only does session.saveOrUpdate on records built
 * from upstream feeds and has no read method, so there is no DAO read path to reuse.
 * {@link DepotRecord} is included partly because its DATE column is literally named "date",
 * a SQL reserved word; a stricter dialect or schema tool after the upgrade would show here.
 */
public class PersistenceModelRoundTripTest {

  // Fixed instant with non-zero millis so a precision loss shows up.
  private static final long FIXED_MILLIS = 1793112912345L;

  private static final String NON_ASCII = "Caf\u00e9 \u00d1and\u00fa";

  private SessionFactory _sessionFactory;

  @Before
  public void setup() throws IOException {
    // Reuse the module's existing test config and add the archive entities that
    // data-sources.xml registers on archiveSessionFactory.
    Configuration config = new Configuration();
    config = config.configure("org/onebusaway/nyc/transit_data_manager/hibernate-configuration.xml");
    config.addAnnotatedClass(VehiclePipoRecord.class);
    config.addAnnotatedClass(CrewAssignmentRecord.class);
    config.addAnnotatedClass(DepotRecord.class);
    _sessionFactory = config.buildSessionFactory();
  }

  @After
  public void teardown() {
    if (_sessionFactory != null)
      _sessionFactory.close();
  }

  @Test
  public void vehiclePipoRecordRoundTrips() {
    Transaction t = getSession().beginTransaction();

    VehiclePipoRecord saved = new VehiclePipoRecord();
    saved.setVehicleId(7582);
    saved.setAgencyIdTcip(2008);
    saved.setAgencyId("MTA NYCT");
    saved.setDepotId("FB");
    // Has a time of day on purpose: the DATE column should drop it.
    saved.setServiceDate(new Date(FIXED_MILLIS));
    saved.setPulloutTime(new Date(FIXED_MILLIS + 1001L));
    saved.setRun(NON_ASCII);
    saved.setOperatorId("123456");
    // Nullable column left null on purpose.
    saved.setPullinTime(null);

    getSession().save(saved);
    Integer id = saved.getId();
    assertNotNull(id);
    getSession().flush();
    getSession().clear();

    VehiclePipoRecord reloaded = (VehiclePipoRecord) getSession().get(VehiclePipoRecord.class, id);
    assertNotNull(reloaded);
    assertNotSame(saved, reloaded);

    assertEquals(id, reloaded.getId());
    assertEquals(Integer.valueOf(7582), reloaded.getVehicleId());
    assertEquals(Integer.valueOf(2008), reloaded.getAgencyIdTcip());
    assertEquals("MTA NYCT", reloaded.getAgencyId());
    assertEquals("FB", reloaded.getDepotId());
    assertDateOnly(FIXED_MILLIS, reloaded.getServiceDate());
    assertTimestamp(FIXED_MILLIS + 1001L, reloaded.getPulloutTime());
    assertEquals(NON_ASCII, reloaded.getRun());
    assertEquals("123456", reloaded.getOperatorId());
    assertNull(reloaded.getPullinTime());

    t.commit();
  }

  @Test
  public void crewAssignmentRecordRoundTrips() {
    Transaction t = getSession().beginTransaction();

    CrewAssignmentRecord saved = new CrewAssignmentRecord();
    saved.setAgencyId("MTA NYCT");
    saved.setDepotId(NON_ASCII);
    saved.setOperatorId("123456");
    saved.setRun("B44-101");
    saved.setServiceDate(new Date(FIXED_MILLIS));
    saved.setUpdated(new Date(FIXED_MILLIS + 2002L));

    getSession().save(saved);
    Integer id = saved.getId();
    assertNotNull(id);
    getSession().flush();
    getSession().clear();

    CrewAssignmentRecord reloaded = (CrewAssignmentRecord) getSession().get(CrewAssignmentRecord.class, id);
    assertNotNull(reloaded);
    assertNotSame(saved, reloaded);

    assertEquals(id, reloaded.getId());
    assertEquals("MTA NYCT", reloaded.getAgencyId());
    assertEquals(NON_ASCII, reloaded.getDepotId());
    assertEquals("123456", reloaded.getOperatorId());
    assertEquals("B44-101", reloaded.getRun());
    assertDateOnly(FIXED_MILLIS, reloaded.getServiceDate());
    assertTimestamp(FIXED_MILLIS + 2002L, reloaded.getUpdated());

    t.commit();
  }

  @Test
  public void depotRecordRoundTrips() {
    Transaction t = getSession().beginTransaction();

    DepotRecord saved = new DepotRecord();
    saved.setVehicleId(7582);
    saved.setAgencyId(NON_ASCII);
    saved.setDepotId("FB");
    saved.setDate(new Date(FIXED_MILLIS));

    getSession().save(saved);
    Integer id = saved.getId();
    assertNotNull(id);
    getSession().flush();
    getSession().clear();

    DepotRecord reloaded = (DepotRecord) getSession().get(DepotRecord.class, id);
    assertNotNull(reloaded);
    assertNotSame(saved, reloaded);

    assertEquals(id, reloaded.getId());
    assertEquals(Integer.valueOf(7582), reloaded.getVehicleId());
    assertEquals(NON_ASCII, reloaded.getAgencyId());
    assertEquals("FB", reloaded.getDepotId());
    assertDateOnly(FIXED_MILLIS, reloaded.getDate());

    t.commit();
  }

  /**
   * TemporalType.DATE: expect a java.sql.Date at local midnight of the saved day. Expected
   * value is computed in the JVM default zone because that is the zone the JDBC driver uses
   * to truncate, so the test is stable on any build machine.
   */
  private static void assertDateOnly(long savedMillis, Date actual) {
    assertNotNull(actual);
    assertEquals(java.sql.Date.class, actual.getClass());
    Calendar midnight = Calendar.getInstance();
    midnight.setTimeInMillis(savedMillis);
    midnight.set(Calendar.HOUR_OF_DAY, 0);
    midnight.set(Calendar.MINUTE, 0);
    midnight.set(Calendar.SECOND, 0);
    midnight.set(Calendar.MILLISECOND, 0);
    assertEquals(midnight.getTimeInMillis(), actual.getTime());
  }

  /** TemporalType.TIMESTAMP: expect a java.sql.Timestamp with the millis intact. */
  private static void assertTimestamp(long expectedMillis, Date actual) {
    assertNotNull(actual);
    assertEquals(Timestamp.class, actual.getClass());
    assertEquals(expectedMillis, actual.getTime());
  }

  private Session getSession() {
    return _sessionFactory.getCurrentSession();
  }
}
