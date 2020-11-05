/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
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

package org.onebusaway.nyc.gtfsrt.tests;

import com.google.transit.realtime.GtfsRealtime.*;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.nyc.gtfsrt.impl.ServiceAlertServiceImpl;
import org.onebusaway.nyc.gtfsrt.service.ServiceAlertFeedBuilder;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class ServiceAlertServiceImplTest {

  ServiceAlertServiceImpl service;

  @Before
  public void setup() {
    ServiceAlertFeedBuilder feedBuilder = mock(ServiceAlertFeedBuilder.class);
    when(feedBuilder.getAlertFromServiceAlert((ServiceAlertBean) any()))
            .thenReturn(Alert.newBuilder());

    ListBean<ServiceAlertBean> listBean = UnitTestSupport.listBean(bean("0"), bean("1"), bean("2"));
    NycTransitDataService tds = mock(NycTransitDataService.class);
    when(tds.getAllServiceAlertsForAgencyId("agency")).thenReturn(listBean);
    when(tds.getAgenciesWithCoverage()).thenReturn(UnitTestSupport.agenciesWithCoverage("agency"));

    service = new ServiceAlertServiceImpl();
    service.setFeedBuilder(feedBuilder);
    service.setTransitDataService(tds);
  }

  @Test
  public void test() {
    List<FeedEntity.Builder> entities = service.getEntities(0);
    assertEquals(3, entities.size());
    for (int i = 0; i < entities.size(); i++) {
      FeedEntity.Builder fe = entities.get(i);
      assertTrue(fe.hasAlert());
      assertFalse(fe.hasTripUpdate());
      assertFalse(fe.hasVehicle());
      assertEquals(Integer.toString(i), fe.getId());
    }
  }

  private static ServiceAlertBean bean(String id) {
    ServiceAlertBean sab = new ServiceAlertBean();
    sab.setId(id);
    return sab;
  }
}