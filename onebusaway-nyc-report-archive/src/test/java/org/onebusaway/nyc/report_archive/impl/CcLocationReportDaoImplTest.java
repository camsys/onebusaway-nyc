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

import org.onebusaway.nyc.report_archive.model.CcLocationReportRecord;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
  public void test() {

    assertEquals(0, _dao.getNumberOfReports());

    CcLocationReportRecord report = new CcLocationReportRecord();
    report.setDestSignCode(123);
    report.setDirectionDeg(new BigDecimal("10.1"));
    report.setLatitude(new BigDecimal("70.25"));
    report.setLongitude(new BigDecimal("-101.10"));
    report.setManufacturerData("manufacturerData");
    report.setOperatorId(123);
    report.setOperatorIdDesignator("operatorIdDesignator");
    report.setRequestId(456);
    report.setRouteId(789);
    report.setRouteIdDesignator("routeIdDesignator");
    report.setRunId(123);
    report.setRunIdDesignator("runIdDesignator");
    report.setSpeed(new BigDecimal("5.6"));
    report.setStatusInfo(456);
    report.setTimeReported(new Date());
    report.setTimeReceived(new Date());
    report.setVehicleAgencydesignator("vehicleAgencydesignator");
    report.setVehicleAgencyId(789);
    report.setVehicleId(120);
    report.setRawMessage("This is a standin for the raw message");
    
    _dao.saveOrUpdateReport(report);
    assertEquals(1, _dao.getNumberOfReports());
  }

}
