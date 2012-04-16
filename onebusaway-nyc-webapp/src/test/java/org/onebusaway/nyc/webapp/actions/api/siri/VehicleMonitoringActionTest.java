package org.onebusaway.nyc.webapp.actions.api.siri;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.onebusaway.nyc.presentation.impl.service_alerts.ServiceAlertsTestSupport;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.siri.SiriXmlSerializer;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.service_alerts.SituationQueryBean;

import uk.org.siri.siri.LocationStructure;
import uk.org.siri.siri.SituationRefStructure;
import uk.org.siri.siri.SituationSimpleRefStructure;
import uk.org.siri.siri.VehicleActivityStructure;
import uk.org.siri.siri.VehicleActivityStructure.MonitoredVehicleJourney;

@RunWith(MockitoJUnitRunner.class)
public class VehicleMonitoringActionTest extends VehicleMonitoringAction {

  private static final long serialVersionUID = 1L;

  @Mock
  private RealtimeService realtimeService;
  
  @Mock
  private NycTransitDataService transitDataService;
  
  @InjectMocks
  private VehicleMonitoringAction action;

  @Mock
  HttpServletRequest request;
  
  @Test
  public void testExecuteByRoute() {
    when(request.getParameter(eq("LineRef"))).thenReturn("S51");
    when(request.getParameter(eq("OperatorRef"))).thenReturn("MTA NYCT");
    List<VehicleActivityStructure> vehicleActivities = new ArrayList<VehicleActivityStructure>();
    when(realtimeService.getVehicleActivityForRoute(eq("MTA NYCT_S51"), anyString(), eq(0))).thenReturn(vehicleActivities);
    
    VehicleActivityStructure vehicleActivity = new VehicleActivityStructure();
    vehicleActivities.add(vehicleActivity);
    
    MonitoredVehicleJourney mvJourney = new MonitoredVehicleJourney();
    vehicleActivity.setMonitoredVehicleJourney(mvJourney );
    
    LocationStructure locationStructure = new LocationStructure();
    mvJourney.setVehicleLocation(locationStructure );
    
    locationStructure.setLatitude(BigDecimal.valueOf(88.0));
    locationStructure.setLongitude(BigDecimal.valueOf(89.0));
    
    ServiceAlertBean serviceAlertBean = ServiceAlertsTestSupport.createServiceAlertBean("MTA NYCT_1");
    when(transitDataService.getServiceAlertForId(anyString())).thenReturn(serviceAlertBean );
    
    List<SituationRefStructure> sitRef = mvJourney.getSituationRef();
    SituationRefStructure sitRefStructure = new SituationRefStructure();
    sitRef.add(sitRefStructure );
    SituationSimpleRefStructure sitSimpleRef = new SituationSimpleRefStructure();
    sitRefStructure.setSituationSimpleRef(sitSimpleRef );
    sitSimpleRef.setValue("situation ref");

    SiriXmlSerializer serializer = new SiriXmlSerializer();
    when(realtimeService.getSiriXmlSerializer()).thenReturn(serializer );

    action.setServletRequest(request);
    action.execute();
    String monitoring = action.getVehicleMonitoring();
    assertTrue("Result XML does not match expected", monitoring.matches("(?s).*<ServiceDelivery><ResponseTimestamp>.+</ResponseTimestamp><VehicleMonitoringDelivery><ResponseTimestamp>.+</ResponseTimestamp><ValidUntil>.+</ValidUntil><VehicleActivity><MonitoredVehicleJourney><SituationRef><SituationSimpleRef>situation ref</SituationSimpleRef></SituationRef><VehicleLocation><Longitude>89.0</Longitude><Latitude>88.0</Latitude></VehicleLocation></MonitoredVehicleJourney></VehicleActivity></VehicleMonitoringDelivery><SituationExchangeDelivery><Situations><PtSituationElement><SituationNumber>MTA NYCT_1</SituationNumber><Summary xml:lang=\"EN\">summary</Summary><Description xml:lang=\"EN\">description</Description><Affects><VehicleJourneys><AffectedVehicleJourney><LineRef>MTA NYCT_B63</LineRef><DirectionRef>0</DirectionRef></AffectedVehicleJourney><AffectedVehicleJourney><LineRef>MTA NYCT_B63</LineRef><DirectionRef>1</DirectionRef></AffectedVehicleJourney><AffectedVehicleJourney><LineRef>MTA NYCT_S55</LineRef><DirectionRef>0</DirectionRef></AffectedVehicleJourney><AffectedVehicleJourney><LineRef>MTA NYCT_S55</LineRef><DirectionRef>1</DirectionRef></AffectedVehicleJourney></VehicleJourneys></Affects></PtSituationElement></Situations></SituationExchangeDelivery></ServiceDelivery></Siri>.*"));
  }

  @Test
  public void testExecuteByRouteNoActivity() {
    when(request.getParameter(eq("LineRef"))).thenReturn("S51");
    when(request.getParameter(eq("OperatorRef"))).thenReturn("MTA NYCT");
    List<VehicleActivityStructure> vehicleActivities = new ArrayList<VehicleActivityStructure>();
    when(realtimeService.getVehicleActivityForRoute(eq("MTA NYCT_S51"), anyString(), eq(0))).thenReturn(vehicleActivities);
    
    ServiceAlertBean serviceAlertBean = ServiceAlertsTestSupport.createServiceAlertBean("MTA NYCT_1");
    when(transitDataService.getServiceAlertForId(anyString())).thenReturn(serviceAlertBean );
    
    ListBean<ServiceAlertBean> serviceAlertListBean = new ListBean<ServiceAlertBean>();
    List<ServiceAlertBean> list = new ArrayList<ServiceAlertBean>();
    list.add(serviceAlertBean);
    serviceAlertListBean.setList(list );
    when(transitDataService.getServiceAlerts(any(SituationQueryBean.class))).thenReturn(serviceAlertListBean );
    
    SiriXmlSerializer serializer = new SiriXmlSerializer();
    when(realtimeService.getSiriXmlSerializer()).thenReturn(serializer );

    action.setServletRequest(request);
    action.execute();
    String monitoring = action.getVehicleMonitoring();
    System.err.println(monitoring);
    assertTrue("Result XML does not match expected", monitoring.matches("(?s).*<SituationExchangeDelivery><Situations><PtSituationElement><SituationNumber>MTA NYCT_1</SituationNumber><Summary xml:lang=\"EN\">summary</Summary><Description xml:lang=\"EN\">description</Description><Affects><VehicleJourneys><AffectedVehicleJourney><LineRef>MTA NYCT_B63</LineRef><DirectionRef>0</DirectionRef></AffectedVehicleJourney><AffectedVehicleJourney><LineRef>MTA NYCT_B63</LineRef><DirectionRef>1</DirectionRef></AffectedVehicleJourney><AffectedVehicleJourney><LineRef>MTA NYCT_S55</LineRef><DirectionRef>0</DirectionRef></AffectedVehicleJourney><AffectedVehicleJourney><LineRef>MTA NYCT_S55</LineRef><DirectionRef>1</DirectionRef></AffectedVehicleJourney></VehicleJourneys></Affects></PtSituationElement></Situations></SituationExchangeDelivery></ServiceDelivery></Siri>.*"));
  }

}
