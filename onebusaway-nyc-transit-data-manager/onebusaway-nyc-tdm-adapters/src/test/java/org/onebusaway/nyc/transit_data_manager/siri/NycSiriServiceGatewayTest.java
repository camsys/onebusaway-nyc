package org.onebusaway.nyc.transit_data_manager.siri;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.onebusaway.nyc.transit_data_federation.siri.SiriXmlSerializer;
import org.onebusaway.transit_data.model.service_alerts.EEffect;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data_federation.impl.realtime.siri.SiriEndpointDetails;

import uk.org.siri.siri.PtSituationElementStructure;
import uk.org.siri.siri.ServiceDelivery;
import uk.org.siri.siri.Siri;

@RunWith(MockitoJUnitRunner.class)
public class NycSiriServiceGatewayTest extends NycSiriServiceGateway {

  // @InjectMocks
  // NycSiriService nycSiriService = new NycSiriServiceGateway();

  @Test
  public void testGetPtSituationAsServiceAlertBean() {
    SiriHelper siriHelper = new SiriHelper();
    PtSituationElementStructure ptSituation = siriHelper.createPtSituationElementStructure(
        "summaryText", "descriptionText", "MTA NYCT_123",
        "2011-11-08T00:00:00.000Z", "", "MTA NYCT_B63", "statusType");
    SiriEndpointDetails endpointDetails = new SiriEndpointDetails();
    ServiceAlertBean serviceAlertBean = getPtSituationAsServiceAlertBean(
        ptSituation, endpointDetails);
    assertNotNull(serviceAlertBean.getSummaries());
    assertEquals("summaryText",
        serviceAlertBean.getSummaries().get(0).getValue());
    assertEquals("descriptionText",
        serviceAlertBean.getDescriptions().get(0).getValue());
    assertEquals("MTA NYCT_123", serviceAlertBean.getId());
    assertEquals(
        ISODateTimeFormat.dateTime().parseDateTime("2011-11-08T00:00:00.000Z").toDate(),
        new Date(serviceAlertBean.getPublicationWindows().get(0).getFrom()));
    assertEquals(0, serviceAlertBean.getPublicationWindows().get(0).getTo());

    assertEquals("MTA NYCT_B63",
        serviceAlertBean.getAllAffects().get(0).getRouteId());

    assertEquals(EEffect.MODIFIED_SERVICE,
        serviceAlertBean.getConsequences().get(0).getEffect());

  }

  @Test
  public void testCreateRequest() throws Exception {
    Siri request = createSubsAndSxRequest();
    String xml = SiriXmlSerializer.getXml(request);
    // Lame test
    assertTrue(xml.contains("<SubscriptionRequest>"));
  }

  @Test
  public void testPostServiceDeliveryActions() throws Exception {
    SituationExchangeResults result = mock(SituationExchangeResults.class);
    ServiceDelivery delivery = mock(ServiceDelivery.class);
    List<ServiceAlertSubscription> serviceAlertSubscriptions = new ArrayList<ServiceAlertSubscription>();
    addSubscription(serviceAlertSubscriptions);
    addSubscription(serviceAlertSubscriptions);
    setServiceAlertSubscriptions(serviceAlertSubscriptions);

    handleServiceDeliveries(result, delivery);

    for (ServiceAlertSubscription s : serviceAlertSubscriptions)
      verify(s).send(eq(result), any(Map.class));
  }

  private ServiceAlertSubscription addSubscription(
      List<ServiceAlertSubscription> serviceAlertSubscriptions) {
    ServiceAlertSubscription subscription = mock(ServiceAlertSubscription.class);
    serviceAlertSubscriptions.add(subscription);
    return subscription;
  }

}
