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

package org.onebusaway.nyc.transit_data_manager.siri;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Date;
import java.util.List;

import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.onebusaway.nyc.siri.support.SiriXmlSerializer;
import org.onebusaway.nyc.transit_data_manager.util.NycSiriUtil;
import org.onebusaway.transit_data.model.service_alerts.EEffect;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.service_alerts.SituationAffectsBean;
import org.onebusaway.transit_data_federation.impl.realtime.siri.SiriEndpointDetails;

import uk.org.siri.siri.AffectsScopeStructure;
import uk.org.siri.siri.AffectsScopeStructure.Operators;
import uk.org.siri.siri.PtSituationElementStructure;
import uk.org.siri.siri.ServiceDelivery;
import uk.org.siri.siri.Siri;

@RunWith(MockitoJUnitRunner.class)
public class NycSiriServiceGatewayTest extends NycSiriServiceGateway {

  @Test
  public void testGetPtSituationAsServiceAlertBean() {
    SiriHelper siriHelper = new SiriHelper();
    PtSituationElementStructure ptSituation = siriHelper.createPtSituationElementStructure(
        "summaryText", "  descriptionText    ", "    MTA NYCT_123",
        "2011-11-08T00:00:00.000Z", "", "MTA NYCT_B63", "statusType");
    SiriEndpointDetails endpointDetails = new SiriEndpointDetails();
    ServiceAlertBean serviceAlertBean = NycSiriUtil.getPtSituationAsServiceAlertBean(
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
  public void testGetPtSituationAsServiceAlertBeanWithCleanup() {
    SiriHelper siriHelper = new SiriHelper();

    PtSituationElementStructure ptSituation = siriHelper.createPtSituationElementStructure(
            "summaryText", "• descriptionText    ", "    MTA NYCT_123",
            "2011-11-08T00:00:00.000Z", "", "MTA NYCT_B63", "statusType");
    SiriEndpointDetails endpointDetails = new SiriEndpointDetails();
    ServiceAlertBean serviceAlertBean = NycSiriUtil.getPtSituationAsServiceAlertBean(
            ptSituation, endpointDetails);

    assertNotNull(serviceAlertBean.getSummaries());
    assertEquals("&#x2022; descriptionText",
            serviceAlertBean.getDescriptions().get(0).getValue());

    ptSituation = siriHelper.createPtSituationElementStructure(
            "summaryText", "· descriptionText    ", "    MTA NYCT_123",
            "2011-11-08T00:00:00.000Z", "", "MTA NYCT_B63", "statusType");
    endpointDetails = new SiriEndpointDetails();
    serviceAlertBean = NycSiriUtil.getPtSituationAsServiceAlertBean(ptSituation, endpointDetails);

    assertNotNull(serviceAlertBean.getSummaries());
    assertEquals("&middot; descriptionText",
            serviceAlertBean.getDescriptions().get(0).getValue());

    ptSituation = siriHelper.createPtSituationElementStructure(
            "summaryText", "— descriptionText    ", "    MTA NYCT_123",
            "2011-11-08T00:00:00.000Z", "", "MTA NYCT_B63", "statusType");
    endpointDetails = new SiriEndpointDetails();
    serviceAlertBean = NycSiriUtil.getPtSituationAsServiceAlertBean(ptSituation, endpointDetails);

    assertNotNull(serviceAlertBean.getSummaries());
    assertEquals("&#x2014; descriptionText",
            serviceAlertBean.getDescriptions().get(0).getValue());

    // m4 <A0><A0><A0> issue
    ptSituation = siriHelper.createPtSituationElementStructure(
            "summaryText", "\240\240\240 descriptionText    ", "    MTA NYCT_270232",
            "2020-11-23T00:00:00.000Z", "", "MTA NYCT_M4", "statusType");
    endpointDetails = new SiriEndpointDetails();
    serviceAlertBean = NycSiriUtil.getPtSituationAsServiceAlertBean(ptSituation, endpointDetails);

    assertNotNull(serviceAlertBean.getSummaries());
    assertEquals(" descriptionText",
            serviceAlertBean.getDescriptions().get(0).getValue());

  }

  @Test
  public void testGetPtSituationAsServiceAlertBeanAllOperators() {
    SiriHelper siriHelper = new SiriHelper();
    PtSituationElementStructure ptSituation = siriHelper.createPtSituationElementStructure(
        "summaryText", "descriptionText    ", "    MTA NYCT_123",
        "2011-11-08T00:00:00.000Z", "", "MTA NYCT_B63", "statusType");
    
    AffectsScopeStructure affects = new AffectsScopeStructure();
    Operators operators = new Operators();
    operators.setAllOperators("");
    affects.setOperators(operators);
    ptSituation.setAffects(affects);
    
    SiriEndpointDetails endpointDetails = new SiriEndpointDetails();
    ServiceAlertBean serviceAlertBean = NycSiriUtil.getPtSituationAsServiceAlertBean(
        ptSituation, endpointDetails);
    
    List<SituationAffectsBean> allAffects = serviceAlertBean.getAllAffects();
    assertNotNull(allAffects);
    assertEquals(1, allAffects.size());
    SituationAffectsBean bean = allAffects.get(0);
    assertEquals(NycSiriUtil.ALL_OPERATORS, bean.getAgencyId());
  }

  @Test
  public void testCreateRequest() throws Exception {
    setup();
    SiriXmlSerializer siriXmlSerializer = new SiriXmlSerializer();
    Siri request = createSubsAndSxRequest();
    String xml = siriXmlSerializer.getXml(request);
    // Lame test
    assertTrue(xml.contains("<SubscriptionRequest>"));
  }

  @Test
  public void testPostServiceDeliveryActions() throws Exception {
    MockSiriServicePersister mockPersister = new MockSiriServicePersister();
    setPersister(mockPersister);
    mockPersister.put("one",
        ServiceAlertsTestSupport.createServiceAlertBean("MTA NYCT_100"));
    SituationExchangeResults result = mock(SituationExchangeResults.class);
    ServiceDelivery delivery = mock(ServiceDelivery.class);
    addSubscription();
    addSubscription();

    handleServiceDeliveries(result, delivery);

    // for (ServiceAlertSubscription s : getActiveServiceAlertSubscriptions())
    //   verify(s).send(any(List.class), any(Collection.class));
  }

  private ServiceAlertSubscription addSubscription() {
    ServiceAlertSubscription subscription = mock(ServiceAlertSubscription.class);
    getPersister().saveOrUpdateSubscription(subscription);
    return subscription;
  }

}
