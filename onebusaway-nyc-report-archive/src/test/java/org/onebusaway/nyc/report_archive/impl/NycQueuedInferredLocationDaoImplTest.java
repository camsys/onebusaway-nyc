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
import org.onebusaway.nyc.report_archive.model.CcAndInferredLocationRecord;
import org.onebusaway.nyc.report_archive.model.CcLocationReportRecord;

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
  private CcLocationReportDaoImpl _ccDao;

  @Before
  public void setup() throws IOException {

    Configuration config = new AnnotationConfiguration();
    config = config.configure("org/onebusaway/nyc/report_archive/hibernate-configuration.xml");
    _sessionFactory = config.buildSessionFactory();

    _dao = new NycQueuedInferredLocationDaoImpl();
    _dao.setSessionFactory(_sessionFactory);

    _ccDao = new CcLocationReportDaoImpl();
    _ccDao.setSessionFactory(_sessionFactory);
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

    CcLocationReportRecord bhs = getCcRecord();
    _ccDao.saveOrUpdateReport(bhs);

    record = getTestRecord();
    _dao.saveOrUpdateRecords(record);
    assertEquals(2, getNumberOfRecords());
    assertEquals(1, getNumberOfCurrentRecords());
    
    List<CcAndInferredLocationRecord> lastKnownRecords = _dao.getAllLastKnownRecords();
    // I believe this should be one as both saveOrUpdateRecords happen for the same bus. Also this will prob end up being similar to getNumberOfCurrentRecords 

    assertEquals(1, lastKnownRecords.size()); 
    assertEquals(record.getVehicleId(), lastKnownRecords.get(0).getVehicleId());
  }

  private CcLocationReportRecord getCcRecord() {
      ArchivedInferredLocationRecord record = getTestRecord();
      CcLocationReportRecord cc = new CcLocationReportRecord();
      cc.setTimeReported(record.getTimeReported());
      cc.setVehicleId(record.getVehicleId());
      cc.setDestSignCode(123);
      cc.setDirectionDeg(new BigDecimal("10.1"));
      cc.setLatitude(new BigDecimal("70.25"));
      cc.setLongitude(new BigDecimal("-101.10"));
      cc.setManufacturerData("manufacturerData");
      cc.setOperatorIdDesignator("operatorIdDesignator");
      cc.setRequestId(456);
      cc.setRouteIdDesignator("routeIdDesignator");
      cc.setRunIdDesignator("runIdDesignator");
      cc.setSpeed(new BigDecimal("5.6"));
      cc.setTimeReceived(new Date());
      cc.setVehicleAgencyDesignator("vehicleAgencyDesignator");
      cc.setVehicleAgencyId(789);
      cc.setNmeaSentenceGPGGA("$GPRMC,105850.00,A,4038.445646,N,07401.094043,W,002.642,128.77,220611,,,A*7C");
      cc.setNmeaSentenceGPRMC("$GPGGA,105850.000,4038.44565,N,07401.09404,W,1,09,01.7,+00042.0,M,,M,,*49");
      cc.setRawMessage("This is a standin for the raw message");
      return cc;
  }

  private ArchivedInferredLocationRecord getTestRecord() {

    ArchivedInferredLocationRecord record = new ArchivedInferredLocationRecord();
    record.setTimeReported(new Date(123456789L));
    record.setVehicleId(120);
    record.setAgencyId("NYC");
    record.setTimeReceived(new Date());
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
    record.setInferredDestSignCode("123");
    record.setInferenceIsFormal(false);
    record.setDepotId("123");
    record.setEmergencyFlag(true);
    record.setInferredOperatorId("6");
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
