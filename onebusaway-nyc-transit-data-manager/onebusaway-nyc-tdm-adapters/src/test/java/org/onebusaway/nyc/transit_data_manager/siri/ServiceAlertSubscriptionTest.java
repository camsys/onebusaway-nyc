package org.onebusaway.nyc.transit_data_manager.siri;

import static org.mockito.Matchers.matches;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;

public class ServiceAlertSubscriptionTest extends ServiceAlertSubscription {

  private static final String TEST_ADDRESS = "http://localhost/foo/bar";
  private static final String TEST_SERVICE_ALERT_ID = "MTA NYC_101";

  @Test
  public void testSend() throws Exception {
    WebResourceWrapper webResourceWrapper = mock(WebResourceWrapper.class);
    setWebResourceWrapper(webResourceWrapper );
    setAddress(TEST_ADDRESS);
    
    SituationExchangeResults results = mock(SituationExchangeResults.class);
    List<DeliveryResult> deliveryResults = new ArrayList<DeliveryResult>();
    when(results.getDelivery()).thenReturn(deliveryResults );
    DeliveryResult deliveryResult = new DeliveryResult();
    List<PtSituationElementResult> ptSituationElements = new ArrayList<PtSituationElementResult>();
    PtSituationElementResult e = new PtSituationElementResult();
    ptSituationElements.add(e);
    e.id = TEST_SERVICE_ALERT_ID;
    e.result = SituationExchangeResults.ADDED;
    deliveryResult.setPtSituationElement(ptSituationElements );
    deliveryResults.add(deliveryResult );
    
    Map<String, ServiceAlertBean> currentServiceAlerts = new HashMap<String, ServiceAlertBean>();
    ServiceAlertBean testBean = ServiceAlertsTestSupport.createServiceAlertBean(TEST_SERVICE_ALERT_ID);
    currentServiceAlerts.put(TEST_SERVICE_ALERT_ID, testBean );

    send(results, currentServiceAlerts );
    
    // Lame-ish test.
    verify(webResourceWrapper).post(matches("(?s).+<SituationNumber>MTA NYC_101</SituationNumber>.+<VehicleJourneys>\\s*<AffectedVehicleJourney>\\s*<LineRef>" + 
        ServiceAlertsTestSupport.TEST_ROUTE_ID + "</LineRef>.+"), same(TEST_ADDRESS));
    
  }

}
