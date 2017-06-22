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
package org.onebusaway.nyc.ops.impl;

import static org.junit.Assert.*;

import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.onebusaway.nyc.ops.impl.CcAndInferredLocationDaoImpl;
import org.onebusaway.nyc.report.impl.CcAndInferredLocationFilter;
import org.onebusaway.nyc.report.impl.CcLocationCache;
import org.onebusaway.nyc.report.impl.RecordValidationServiceImpl;
import org.onebusaway.nyc.report.model.ArchivedInferredLocationRecord;
import org.onebusaway.nyc.report.model.CcAndInferredLocationRecord;
import org.onebusaway.nyc.report.model.CcLocationReportRecord;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class  CcAndInferredLocationDaoImplTest {

  private SessionFactory _sessionFactory;

  private CcAndInferredLocationDaoImpl _dao;
  private CcLocationCache _cache;

  @Before
  public void setup() throws IOException {

    Configuration config = new Configuration();
    config = config.configure("org/onebusaway/nyc/ops/hibernate-configuration.xml");
    ServiceRegistry serviceRegistry = new ServiceRegistryBuilder().applySettings(
    config.getProperties()). buildServiceRegistry();
    _sessionFactory = config.buildSessionFactory(serviceRegistry);

    _cache = new CcLocationCache(10);
    
    _dao = new CcAndInferredLocationDaoImpl();
    _dao.setValidationService(new RecordValidationServiceImpl());
    _dao.setSessionFactory(_sessionFactory);
    _dao.setCcLocationCache(_cache);

  }

  @After
  public void teardown() {
    if (_sessionFactory != null)
      _sessionFactory.close();
  }
  
  @Test
  public void test() throws Exception {

    getSession().beginTransaction();

    assertEquals(0, getNumberOfRecords());

    CcLocationReportRecord bhs = getCcRecord();
    _cache.put(bhs); // this happens via ArchivingInputQueueListener 

    ArchivedInferredLocationRecord record = getTestRecord();
    _dao.saveOrUpdateRecords(record);

    assertEquals(1, getNumberOfCurrentRecords());
    
    record = getTestRecord();

    _dao.saveOrUpdateRecord(record);
    assertEquals(1, getNumberOfCurrentRecords());

    Map<CcAndInferredLocationFilter, String> filter = new HashMap<CcAndInferredLocationFilter, String>();
    filter.put(CcAndInferredLocationFilter.DEPOT_ID, "");
    filter.put(CcAndInferredLocationFilter.INFERRED_ROUTEID, "");
    filter.put(CcAndInferredLocationFilter.INFERRED_PHASE, "");
    
    List<CcAndInferredLocationRecord> lastKnownRecords = _dao.getAllLastKnownRecords(filter);
    // this should be one as both saveOrUpdateRecords happen for the same bus. Also this will prob end up being similar to getNumberOfCurrentRecords 

    assertEquals(1, lastKnownRecords.size()); 
    assertEquals(record.getVehicleId(), lastKnownRecords.get(0).getVehicleId());
    
    Integer aVehicleId = new Integer(120);
    
    CcAndInferredLocationRecord latestRec = _dao.getLastKnownRecordForVehicle(aVehicleId);

    // check that we received a non null value
    assertNotNull(latestRec);
    
    // Now check to see that the single vehicle result matches the result in the getAllLastKnownRecords result for the same vehicle.
    boolean foundMatch = false;
    for (CcAndInferredLocationRecord rec : lastKnownRecords) {
    	if (rec.getVehicleId().intValue() == aVehicleId.intValue()) {
    		foundMatch = true;
    		assertTrue(rec.getUUID() == latestRec.getUUID());
    	}
    }
    
    assertTrue(foundMatch);
  }
  
  private CcLocationReportRecord getCcRecord() {
      ArchivedInferredLocationRecord record = getTestRecord();
      CcLocationReportRecord cc = new CcLocationReportRecord();
			cc.setUUID(record.getUUID());
      cc.setTimeReported(record.getTimeReported());
			cc.setArchiveTimeReceived(new Date(122345l));
      cc.setVehicleId(record.getVehicleId());
      cc.setDestSignCode(123);
      cc.setDirectionDeg(new BigDecimal("10.1"));
      cc.setLatitude(new BigDecimal("70.25"));
      cc.setLongitude(new BigDecimal("-101.10"));
      cc.setManufacturerData("manufacturerData");
      cc.setOperatorIdDesignator("opIdDesignator");
      cc.setRequestId(456);
      cc.setRouteIdDesignator("rteIdDesignator");
      cc.setRunIdDesignator("runIdDesignator");
      cc.setSpeed(new BigDecimal("5.6"));
      cc.setTimeReceived(new Date(System.currentTimeMillis()));
      cc.setVehicleAgencyDesignator("vehicleAgencyDesignator");
      cc.setVehicleAgencyId(789);
      cc.setNmeaSentenceGPGGA("$GPRMC,105850.00,A,4038.445646,N,07401.094043,W,002.642,128.77,220611,,,A*7C");
      cc.setNmeaSentenceGPRMC("$GPGGA,105850.000,4038.44565,N,07401.09404,W,1,09,01.7,+00042.0,M,,M,,*49");
      cc.setRawMessage("This is a standin for the raw message");
      return cc;
  }

  private ArchivedInferredLocationRecord getTestRecord() {

    ArchivedInferredLocationRecord record = new ArchivedInferredLocationRecord();
		record.setUUID("foo");
    record.setTimeReported(new Date(123456789L));
    record.setVehicleId(120);
    record.setAgencyId("NYC");
    record.setArchiveTimeReceived(new Date(System.currentTimeMillis()));
    record.setServiceDate(new Date(20111010L));
    record.setScheduleDeviation(1);
    record.setInferredBlockId("1");
    record.setInferredTripId("2");
    record.setDistanceAlongBlock(1000.0);
    record.setDistanceAlongTrip(5000.0);
    record.setInferredLatitude(new BigDecimal("70.35"));
    record.setInferredLongitude(new BigDecimal("-101.20"));
    record.setInferredPhase("phase");
    record.setInferredStatus("status");

    record.setLastUpdateTime(20111010L);
    record.setLastLocationUpdateTime(20111010L);
    record.setInferredDestSignCode(123);
    record.setInferenceIsFormal(false);
    record.setDepotId("123");
    record.setEmergencyFlag(true);
    record.setInferredOperatorId("6");
    record.setInferredRunId("1");

    record.setNextScheduledStopId("1");
    record.setNextScheduledStopDistance(1.0);

    return record;
  }    
 

  @SuppressWarnings("unchecked")
  private int getNumberOfRecords() {
      Query query = getSession().createQuery("select count(*) from ArchivedInferredLocationRecord");
      Long count = (Long) query.uniqueResult();
      return count.intValue();

  }

  @SuppressWarnings("unchecked")
  private int getNumberOfCurrentRecords() {
      Query query = getSession().createQuery("select count(*) from CcAndInferredLocationRecord");
      Long count = (Long) query.uniqueResult();
      return count.intValue();
  }

  private Session getSession(){
        return _sessionFactory.getCurrentSession();
    }
}
