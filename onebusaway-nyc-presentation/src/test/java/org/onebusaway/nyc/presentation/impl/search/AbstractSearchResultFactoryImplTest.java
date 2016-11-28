package org.onebusaway.nyc.presentation.impl.search;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.onebusaway.geocoder.enterprise.services.EnterpriseGeocoderResult;
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
  public SearchResult getGeocoderResult(EnterpriseGeocoderResult geocodeResult,
      Set<RouteBean> routeFilter) {
    // TODO Auto-generated method stub
    return null;
  }

}
