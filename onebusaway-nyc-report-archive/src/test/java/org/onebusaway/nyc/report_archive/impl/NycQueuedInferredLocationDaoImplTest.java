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

// import org.onebusaway.nyc.report_archive.model.NycQueuedInferredLocationRecord;
import org.onebusaway.nyc.report_archive.model.ArchivedInferredLocationRecord;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class  NycQueuedInferredLocationDaoImplTest {

  private SessionFactory _sessionFactory;

  private NycQueuedInferredLocationDaoImpl _dao;

  @Before
  public void setup() throws IOException {

    Configuration config = new AnnotationConfiguration();
    config = config.configure("org/onebusaway/nyc/report_archive/hibernate-configuration.xml");
    _sessionFactory = config.buildSessionFactory();

    _dao = new NycQueuedInferredLocationDaoImpl();
    _dao.setSessionFactory(_sessionFactory);
  }

  @After
  public void teardown() {
    if (_sessionFactory != null)
      _sessionFactory.close();
  }

  @Test
  public void test() {

    assertEquals(0, getNumberOfRecords());

    ArchivedInferredLocationRecord record = getTestRecord();

    _dao.saveOrUpdateRecord(record);
    assertEquals(1, getNumberOfRecords());
    assertEquals(1, getNumberOfCurrentRecords());

    record = getTestRecord();
    _dao.saveOrUpdateRecords(record);
    assertEquals(2, getNumberOfRecords());
    assertEquals(1, getNumberOfCurrentRecords());
    
    //List<ArchivedInferredLocationRecord> lastKnownRecords = _dao.getAllLastKnownRecords();
    //assertEquals(1, lastKnownRecords.size()); // I believe this should be one as both saveOrUpdateRecords happen for the same bus. Also this will prob end up being similar to getNumberOfCurrentRecords 
  }

  private ArchivedInferredLocationRecord getTestRecord() {
    ArchivedInferredLocationRecord record = new ArchivedInferredLocationRecord();
    record.setRecordTimestamp(new Date(123456789L));
    record.setVehicleId("120");
    record.setServiceDate(new Date(20111010L));
    record.setScheduleDeviation(1);
    record.setBlockId("1");
    record.setTripId("2");
    record.setDistanceAlongBlock(1000.0);
    record.setDistanceAlongTrip(5000.0);
    record.setInferredLatitude(new BigDecimal("70.35"));
    record.setInferredLongitude(new BigDecimal("-101.20"));
    record.setObservedLatitude(new BigDecimal("70.25"));
    record.setObservedLongitude(new BigDecimal("-101.10"));
    record.setPhase("phase");
    record.setStatus("status");
    record.setBearing(180.0);
    record.setRawMessage("This is a standin for the raw message");

    record.setActiveBundleId("1");
    record.setLastUpdateTime(20111010L);
    record.setLastLocationUpdateTime(20111010L);
    record.setLastObservedLatitude(new BigDecimal("70.25"));
    record.setLastObservedLongitude(new BigDecimal("-101.10"));
    record.setMostRecentObservedDestinationSignCode("123");
    record.setLastInferredDestinationSignCode("123");
    record.setInferenceEngineHostname("prod.obanyc.com");
    record.setInferenceIsEnabled(true);
    record.setInferenceEngineIsPrimary(true);
    record.setInferenceIsFormal(false);
    record.setDepotId("123");
    record.setEmergencyFlag(true);
    record.setLastInferredOperatorId("6");
    record.setInferredRunId("1");

    record.setNextScheduledStopId("1");
    record.setNextScheduledStopDistance(1.0);

    return record;
  }    

  private int getNumberOfRecords() {
    //    @SuppressWarnings("rawtypes")
    Long count = (Long) _dao.getHibernateTemplate().execute(new HibernateCallback() {
	public Object doInHibernate(Session session) throws HibernateException {
        Query query = session.createQuery("select count(*) from ArchivedInferredLocationRecord");
	return (Long)query.uniqueResult();
	}
    });
    return count.intValue();
  }

  private int getNumberOfCurrentRecords() {
    //    @SuppressWarnings("rawtypes")
    Long count = (Long) _dao.getHibernateTemplate().execute(new HibernateCallback() {
	public Object doInHibernate(Session session) throws HibernateException {
        Query query = session.createQuery("select count(*) from InferredLocationRecord");
	return (Long)query.uniqueResult();
	}
    });
    return count.intValue();
  }

}
