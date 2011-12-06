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
package org.onebusaway.nyc.transit_data_manager.siri;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;

public class SiriServiceDaoTest {

  private SessionFactory _sessionFactory;

  private SiriServiceDao _dao;

  @Before
  public void setup() throws IOException {

    Configuration config = new AnnotationConfiguration();
    config = config.configure("org/onebusaway/nyc/transit_data_manager/hibernate-configuration.xml");
    _sessionFactory = config.buildSessionFactory();

    _dao = new SiriServiceDao();
    _dao.setSessionFactory(_sessionFactory);
  }

  @After
  public void teardown() {
    if (_sessionFactory != null)
      _sessionFactory.close();
  }

  @Test
  public void testSave() {
    ServiceAlertBean bean = ServiceAlertsTestSupport.createServiceAlertBean("my test id");
    _dao.saveOrUpdateServiceAlert(bean);
  }

  @Test
  public void testDeleteByServiceAlertId() {
    String id = "my test id2";
    ServiceAlertBean bean = ServiceAlertsTestSupport.createServiceAlertBean(id);
    _dao.saveOrUpdateServiceAlert(bean);
    ServiceAlertBean bean2 = _dao.deleteServiceAlertById(id);
    assertEquals(bean.getId(), bean2.getId());
    List<ServiceAlertBean> alerts = _dao.getAllActiveServiceAlerts();
    for (ServiceAlertBean b: alerts) {
      if (b.getId().equals(id)) {
        fail("Still found service alert with id");
      }
    }
  }

}
