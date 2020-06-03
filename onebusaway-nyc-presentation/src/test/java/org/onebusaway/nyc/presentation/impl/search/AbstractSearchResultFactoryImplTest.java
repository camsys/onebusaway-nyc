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

package org.onebusaway.nyc.presentation.impl.search;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.onebusaway.nyc.geocoder.service.NycGeocoderResult;
import org.onebusaway.nyc.presentation.model.SearchResult;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;

public class AbstractSearchResultFactoryImplTest extends AbstractSearchResultFactoryImpl {

  private Set<String> serviceAlertDescriptions;
  private List<ServiceAlertBean> serviceAlertBeans;

  @Test
  public void testPopulateServiceAlertsSetOfStringListOfServiceAlertBeanNulls() {
    Set<String> serviceAlertDescriptions = null;
    List<ServiceAlertBean> serviceAlertBeans = null;
    populateServiceAlerts(serviceAlertDescriptions, serviceAlertBeans);
  }

  @Before
  public void setup() {
    serviceAlertDescriptions = new HashSet<String>();
    serviceAlertBeans = new ArrayList<ServiceAlertBean>();
    ServiceAlertBean e = new ServiceAlertBean();
    List<NaturalLanguageStringBean> descriptions = new ArrayList<NaturalLanguageStringBean>();
    descriptions.add(new NaturalLanguageStringBean("line one\nline two", "EN"));
    e.setDescriptions(descriptions );
    serviceAlertBeans.add(e );
  }
  
  @Test
  public void testPopulateServiceAlertsSetOfStringListOfServiceAlertBean() {
    populateServiceAlerts(serviceAlertDescriptions, serviceAlertBeans);
    assertEquals(1, serviceAlertDescriptions.size());
    String d = (String) serviceAlertDescriptions.toArray()[0];
    assertTrue("line one<br/>line two".equals(d));
  }

  @Test
  public void testPopulateServiceAlertsSetOfStringListOfServiceAlertBeanBoolean() {
    populateServiceAlerts(serviceAlertDescriptions, serviceAlertBeans, false);
    assertEquals(1, serviceAlertDescriptions.size());
    String d = (String) serviceAlertDescriptions.toArray()[0];
    assertTrue("line one\nline two".equals(d));
  }

  @Override
  public SearchResult getRouteResult(RouteBean routeBean) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public SearchResult getRouteResultForRegion(RouteBean routeBean) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public SearchResult getStopResult(StopBean stopBean,
      Set<RouteBean> routeFilter) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public SearchResult getGeocoderResult(NycGeocoderResult geocodeResult,
      Set<RouteBean> routeFilter) {
    // TODO Auto-generated method stub
    return null;
  }

}
