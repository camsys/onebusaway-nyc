package org.onebusaway.nyc.transit_data_manager.siri;

import static org.junit.Assert.*;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.custommonkey.xmlunit.Diff.*;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.ElementNameAndTextQualifier;
import org.custommonkey.xmlunit.examples.RecursiveElementNameAndTextQualifier;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;

public class ServiceAlertSubscriptionTest extends ServiceAlertSubscription {

  private static final long serialVersionUID = 1L;
  private static final String TEST_ADDRESS = "http://localhost/foo/bar";
  private static final String TEST_SERVICE_ALERT_ID = "MTA NYC_101";

  String EXPECTED_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Siri xmlns:ns2=\"http://www.ifopt.org.uk/acsb\" "
      + "xmlns:ns4=\"http://datex2.eu/schema/1_0/1_0\" xmlns:ns3=\"http://www.ifopt.org.uk/ifopt\" xmlns=\"http://www.siri.org.uk/siri\">"
      + "<ServiceDelivery><ProducerRef>test</ProducerRef><SituationExchangeDelivery><Situations><PtSituationElement><SituationNumber>"
      + "MTA NYC_101</SituationNumber><Summary xml:lang=\"EN\">summary</Summary><Description xml:lang=\"EN\">description</Description><Affects>"
      + "<VehicleJourneys><AffectedVehicleJourney><LineRef>test route id</LineRef></AffectedVehicleJourney></VehicleJourneys></Affects>"
      + "</PtSituationElement><PtSituationElement><SituationNumber>MTA NYCT_1000</SituationNumber><Progress>closed</Progress></PtSituationElement>"
      + "<PtSituationElement><SituationNumber>MTA NYCT_1001</SituationNumber><Progress>closed</Progress></PtSituationElement></Situations>"
      + "</SituationExchangeDelivery></ServiceDelivery></Siri>";

  @Test
  public void testSend() throws Exception {
    WebResourceWrapper webResourceWrapper = mock(WebResourceWrapper.class);
    setWebResourceWrapper(webResourceWrapper);
    setAddress(TEST_ADDRESS);

    SituationExchangeResults results = mock(SituationExchangeResults.class);
    List<DeliveryResult> deliveryResults = new ArrayList<DeliveryResult>();
    when(results.getDelivery()).thenReturn(deliveryResults);
    DeliveryResult deliveryResult = new DeliveryResult();
    List<PtSituationElementResult> ptSituationElements = new ArrayList<PtSituationElementResult>();
    PtSituationElementResult e = new PtSituationElementResult();
    ptSituationElements.add(e);
    e.id = TEST_SERVICE_ALERT_ID;
    e.result = SituationExchangeResults.ADDED;
    deliveryResult.setPtSituationElement(ptSituationElements);
    deliveryResults.add(deliveryResult);

    Map<String, ServiceAlertBean> currentServiceAlerts = new HashMap<String, ServiceAlertBean>();
    ServiceAlertBean testBean = ServiceAlertsTestSupport.createServiceAlertBean(TEST_SERVICE_ALERT_ID);
    currentServiceAlerts.put(TEST_SERVICE_ALERT_ID, testBean);

    List<String> deletedIds = new ArrayList<String>();
    deletedIds.add("MTA NYCT_1000");
    deletedIds.add("MTA NYCT_1001");

    String environment = "test";
    send(currentServiceAlerts, deletedIds, environment);

    ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
    verify(webResourceWrapper).post(argument.capture(), same(TEST_ADDRESS));
    String value = argument.getValue();
    System.out.println(value);
    Diff diff = new Diff(EXPECTED_XML, value);
    diff.overrideElementQualifier(new RecursiveElementNameAndTextQualifier());
	assertXMLEqual(diff, true);

  }

}
