package org.onebusaway.nyc.vehicle_tracking.impl;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.hibernate.SessionFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners( {
    DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class})
@ContextConfiguration(locations = {"classpath:org/onebusaway/nyc/vehicle_tracking/impl/NycVehicleLocationRecordDaoImplTest.xml"})
public class NycVehicleLocationRecordDaoImplTest {

  private SessionFactory _sessionFactory;

  @Autowired
  public void setSessionFactory(SessionFactory sessionFactory) {
    _sessionFactory = sessionFactory;
  }

  @Test
  public void test() {

    VehicleTrackingMutableDaoImpl dao = new VehicleTrackingMutableDaoImpl();
    dao.setMutableSessionFactory(_sessionFactory);

    long t = System.currentTimeMillis();

    NycVehicleLocationRecord record = new NycVehicleLocationRecord();
    record.setBearing(180);
    record.setDestinationSignCode("ABC");
    record.setLatitude(47.0);
    record.setLongitude(-122.0);
    record.setTime(t);
    record.setVehicleId(new AgencyAndId("2008", "123"));

    String aVeryLongString = "a very long string";
    for (int i = 0; i < 7; ++i) {
      aVeryLongString += aVeryLongString;
    }
    record.setRawData(aVeryLongString);

    dao.saveOrUpdateVehicleLocationRecord(record);

    List<NycVehicleLocationRecord> records = dao.getRecordsForTimeRange(
        t - 1000, t + 1000);
    assertEquals(1, records.size());

    NycVehicleLocationRecord r = records.get(0);

    assertEquals(180.0, r.getBearing(), 0.0);
    assertEquals(47.0, r.getLatitude(), 0.0);
    assertEquals(-122.0, r.getLongitude(), 0.0);
    assertEquals(t, r.getTime());
    assertEquals(new AgencyAndId("2008", "123"), r.getVehicleId());

    records = dao.getRecordsForTimeRange(t - 2000, t - 1000);

    assertEquals(0, records.size());

  }
}
