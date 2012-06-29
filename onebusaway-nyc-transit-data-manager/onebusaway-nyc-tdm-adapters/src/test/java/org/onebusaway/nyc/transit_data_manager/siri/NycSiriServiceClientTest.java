package org.onebusaway.nyc.transit_data_manager.siri;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.siri.core.ESiriModuleType;
import org.onebusaway.transit_data.model.AgencyBean;
import org.onebusaway.transit_data.model.AgencyWithCoverageBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data_federation.impl.realtime.siri.SiriEndpointDetails;

import uk.org.siri.siri.EntryQualifierStructure;
import uk.org.siri.siri.PtSituationElementStructure;
import uk.org.siri.siri.ServiceDelivery;
import uk.org.siri.siri.SituationExchangeDeliveryStructure;
import uk.org.siri.siri.SituationExchangeDeliveryStructure.Situations;

@RunWith(MockitoJUnitRunner.class)
public class NycSiriServiceClientTest extends NycSiriServiceClient {
  
  @Mock
//  NycTransitDataService transitDataService = mock(NycTransitDataService.class);
  NycTransitDataService transitDataService;
  
  @Test
  public void testHandleServiceDelivery() {
    setTransitDataService(transitDataService);
    List<AgencyWithCoverageBean> agenciesWithCoverage = new ArrayList<AgencyWithCoverageBean>();
    AgencyWithCoverageBean agencyWithCoverage = mock(AgencyWithCoverageBean.class);
    AgencyBean agencyBean = mock(AgencyBean.class);
    when(agencyWithCoverage.getAgency()).thenReturn(agencyBean );
    agenciesWithCoverage.add(agencyWithCoverage );
    when(transitDataService.getAgenciesWithCoverage()).thenReturn(agenciesWithCoverage );
    
    ServiceDelivery serviceDelivery = mock(ServiceDelivery.class);
    SituationExchangeDeliveryStructure deliveryForModule = mock(SituationExchangeDeliveryStructure.class);
    ESiriModuleType moduleType = ESiriModuleType.SITUATION_EXCHANGE;
    SiriEndpointDetails endpointDetails = mock(SiriEndpointDetails.class);
    SituationExchangeResults result = mock(SituationExchangeResults.class);
    List<String> preAlertIds = new ArrayList<String>();
    
    Situations situations = mock(Situations.class);
    when(deliveryForModule.getSituations()).thenReturn(situations );
    List<PtSituationElementStructure> listPtSitEltStructure = new ArrayList<PtSituationElementStructure>();
    when(situations.getPtSituationElement()).thenReturn(listPtSitEltStructure );
    PtSituationElementStructure ptSitEltStructure = mock(PtSituationElementStructure.class);
    listPtSitEltStructure.add(ptSitEltStructure );
    
    EntryQualifierStructure sitNumber = mock(EntryQualifierStructure.class);
    when(ptSitEltStructure.getSituationNumber()).thenReturn(sitNumber );
    
    when(sitNumber.getValue()).thenReturn("MTA NYCT_1");
    
    handleServiceDelivery(serviceDelivery, deliveryForModule, moduleType,
        endpointDetails, result, preAlertIds);
    
    verify(transitDataService).removeAllServiceAlertsForAgencyId(anyString());
    verify(transitDataService).createServiceAlert(anyString(), any(ServiceAlertBean.class));
    
  }
}