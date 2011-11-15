package org.onebusaway.nyc.webapp.actions.api;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.onebusaway.nyc.webapp.actions.api.siri.VehicleMonitoringAction;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.service_alerts.SituationAffectsBean;
import org.onebusaway.transit_data.services.TransitDataService;

import uk.org.siri.siri.ServiceDelivery;
import uk.org.siri.siri.SituationExchangeDeliveryStructure;
import uk.org.siri.siri.SituationRefStructure;
import uk.org.siri.siri.SituationSimpleRefStructure;
import uk.org.siri.siri.VehicleActivityStructure;
import uk.org.siri.siri.VehicleActivityStructure.MonitoredVehicleJourney;
import static org.mockito.Mockito.*;

public class VehicleMonitoringActionTest extends VehicleMonitoringAction {

  private static final long serialVersionUID = 1L;

  @Test
  public void testAddSituationExchangeEmpty() {
    ServiceDelivery serviceDelivery = new ServiceDelivery();
    List<VehicleActivityStructure> activities = new ArrayList<VehicleActivityStructure>();
    addSituationExchange(serviceDelivery, activities);
    List<SituationExchangeDeliveryStructure> list = serviceDelivery.getSituationExchangeDelivery();
    assertEquals(0, list.size());
  }

  @Test
  public void testAddSituationExchangeNonEmpty() {
    
    ServiceAlertBean serviceAlertBean = new ServiceAlertBean();
    List<NaturalLanguageStringBean> summaries = new ArrayList<NaturalLanguageStringBean>();
    serviceAlertBean.setSummaries(summaries );
    List<NaturalLanguageStringBean> descriptions = new ArrayList<NaturalLanguageStringBean>();
    serviceAlertBean.setDescriptions(descriptions);
    
    List<SituationAffectsBean> allAffects = new ArrayList<SituationAffectsBean>();
    serviceAlertBean.setAllAffects(allAffects );

    _transitDataService = mock(TransitDataService.class);
    when(_transitDataService.getServiceAlertForId(anyString())).thenReturn(serviceAlertBean);

    ServiceDelivery serviceDelivery = new ServiceDelivery();
    List<VehicleActivityStructure> activities = new ArrayList<VehicleActivityStructure>();
    VehicleActivityStructure vehicleActivity = new VehicleActivityStructure();
    activities.add(vehicleActivity);

    MonitoredVehicleJourney monitoredVehicleJourney = new MonitoredVehicleJourney();
    vehicleActivity.setMonitoredVehicleJourney(monitoredVehicleJourney);

    SituationRefStructure situationRefStructure = new SituationRefStructure();
    monitoredVehicleJourney.getSituationRef().add(situationRefStructure);
    SituationSimpleRefStructure situationSimpleRef = new SituationSimpleRefStructure();
    situationRefStructure.setSituationSimpleRef(situationSimpleRef);
    situationSimpleRef.setValue("MTA NYCT_100");
    
    addSituationExchange(serviceDelivery, activities);
    List<SituationExchangeDeliveryStructure> list = serviceDelivery.getSituationExchangeDelivery();
    assertEquals(1, list.size());
    
  }

}
