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

import org.onebusaway.nyc.report_archive.model.NycQueuedInferredLocationRecord;

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

    NycQueuedInferredLocationRecord record = getTestRecord();

    _dao.saveOrUpdateRecord(record);
    assertEquals(1, getNumberOfRecords());

    record = getTestRecord();
    _dao.saveOrUpdateRecords(record);
    assertEquals(2, getNumberOfRecords());
  }

  private NycQueuedInferredLocationRecord getTestRecord() {
    NycQueuedInferredLocationRecord record = new NycQueuedInferredLocationRecord();
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

    return record;
  }    

  private int getNumberOfRecords() {
    //    @SuppressWarnings("rawtypes")
    Long count = (Long) _dao.getHibernateTemplate().execute(new HibernateCallback() {
	public Object doInHibernate(Session session) throws HibernateException {
        Query query = session.createQuery("select count(*) from NycQueuedInferredLocationRecord");
	return (Long)query.uniqueResult();
	}
    });
    return count.intValue();
  }

}
