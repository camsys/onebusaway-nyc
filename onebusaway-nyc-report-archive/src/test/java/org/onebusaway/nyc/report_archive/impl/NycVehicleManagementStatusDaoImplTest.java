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

import org.onebusaway.nyc.report_archive.model.NycVehicleManagementStatusRecord;

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

public class  NycVehicleManagementStatusDaoImplTest {

  private SessionFactory _sessionFactory;

  private NycVehicleManagementStatusDaoImpl _dao;

  @Before
  public void setup() throws IOException {

    Configuration config = new AnnotationConfiguration();
    config = config.configure("org/onebusaway/nyc/report_archive/hibernate-configuration.xml");
    _sessionFactory = config.buildSessionFactory();

    _dao = new NycVehicleManagementStatusDaoImpl();
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

    NycVehicleManagementStatusRecord record = getTestRecord();

    _dao.saveOrUpdateRecord(record);
    assertEquals(1, getNumberOfRecords());
  }

  private NycVehicleManagementStatusRecord getTestRecord() {
    NycVehicleManagementStatusRecord record = new NycVehicleManagementStatusRecord();

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

    return record;
  }    

  private int getNumberOfRecords() {
    //    @SuppressWarnings("rawtypes")
    Long count = (Long) _dao.getHibernateTemplate().execute(new HibernateCallback() {
	public Object doInHibernate(Session session) throws HibernateException {
        Query query = session.createQuery("select count(*) from NycVehicleManagementStatusRecord");
	return (Long)query.uniqueResult();
	}
    });
    return count.intValue();
  }

}
