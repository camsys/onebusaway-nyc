package org.onebusaway.nyc.transit_data_manager.siri;

import static org.junit.Assert.*;

import java.util.Date;

import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;
import org.onebusaway.transit_data.model.service_alerts.EEffect;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data_federation.impl.realtime.siri.SiriEndpointDetails;

import uk.org.siri.siri.PtSituationElementStructure;

public class SiriServiceTest extends SiriService {

  @Test
  public void testGetPtSituationAsServiceAlertBean() {
    SiriHelper siriHelper = new SiriHelper();
    PtSituationElementStructure ptSituation = siriHelper.createPtSituationElementStructure("summaryText",
        "descriptionText", "MTA NYCT_123", "2011-11-08T00:00:00.000Z", "", "MTA NYCT_B63", "statusType");
    SiriEndpointDetails endpointDetails = new SiriEndpointDetails();
    ServiceAlertBean serviceAlertBean = getPtSituationAsServiceAlertBean(ptSituation, endpointDetails);
    assertNotNull(serviceAlertBean.getSummaries());
    assertEquals("summaryText", serviceAlertBean.getSummaries().get(0).getValue());
    assertEquals("descriptionText", serviceAlertBean.getDescriptions().get(0).getValue());
    assertEquals("MTA NYCT_123", serviceAlertBean.getId());
    assertEquals(ISODateTimeFormat.dateTime().parseDateTime("2011-11-08T00:00:00.000Z")
        .toDate(), new Date(serviceAlertBean.getPublicationWindows().get(0).getFrom()));
    assertEquals(0, serviceAlertBean.getPublicationWindows().get(0).getTo());
    
    assertEquals("MTA NYCT_B63", serviceAlertBean.getAllAffects().get(0).getRouteId());
    
    assertEquals(EEffect.MODIFIED_SERVICE, serviceAlertBean.getConsequences().get(0).getEffect());
    
  }

}
