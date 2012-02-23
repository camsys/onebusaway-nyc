package org.onebusaway.nyc.presentation.impl.service_alerts;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.services.TransitDataService;

import uk.org.siri.siri.AffectedVehicleJourneyStructure;
import uk.org.siri.siri.AffectsScopeStructure;
import uk.org.siri.siri.AffectsScopeStructure.VehicleJourneys;
import uk.org.siri.siri.DefaultedTextStructure;
import uk.org.siri.siri.EntryQualifierStructure;
import uk.org.siri.siri.MonitoredStopVisitStructure;
import uk.org.siri.siri.PtSituationElementStructure;
import uk.org.siri.siri.ServiceDelivery;
import uk.org.siri.siri.SituationExchangeDeliveryStructure;
import uk.org.siri.siri.SituationRefStructure;
import uk.org.siri.siri.SituationSimpleRefStructure;
import uk.org.siri.siri.VehicleActivityStructure;
import uk.org.siri.siri.VehicleActivityStructure.MonitoredVehicleJourney;
import uk.org.siri.siri.WorkflowStatusEnumeration;
import static org.mockito.Mockito.*;

public class ServiceAlertsHelperTest extends ServiceAlertsHelper {

  private static final boolean SKIP_JOURNEY = true;
  private TransitDataService transitDataService;

  @Test
  public void testGetServiceAlertBeanAsPtSituationElementStructure() {
    ServiceAlertBean serviceAlertBean = new ServiceAlertBean();
    PtSituationElementStructure structure = getServiceAlertBeanAsPtSituationElementStructure(serviceAlertBean);
    assertNotNull(structure);
  }
  
  @Test
  public void testAddSituationExchangeEmpty() {
    ServiceDelivery serviceDelivery = new ServiceDelivery();
    List<VehicleActivityStructure> activities = new ArrayList<VehicleActivityStructure>();
    addSituationExchangeToServiceDelivery(serviceDelivery, activities, transitDataService);
    List<SituationExchangeDeliveryStructure> list = serviceDelivery.getSituationExchangeDelivery();
    assertEquals(0, list.size());
  }

  @Test
  public void testAddSituationExchangeNonEmpty() {

    ServiceAlertBean serviceAlertBean = ServiceAlertsTestSupport.createServiceAlertBean("MTA NYCT_100");

    transitDataService = mock(TransitDataService.class);
    when(transitDataService.getServiceAlertForId(anyString())).thenReturn(
        serviceAlertBean);

    ServiceDelivery serviceDelivery = new ServiceDelivery();
    List<VehicleActivityStructure> activities = new ArrayList<VehicleActivityStructure>();
    createActivity(activities, "MTA NYCT_100");

    addSituationExchangeToServiceDelivery(serviceDelivery, activities, transitDataService);

    List<SituationExchangeDeliveryStructure> list = serviceDelivery.getSituationExchangeDelivery();
    assertEquals(1, list.size());
    SituationExchangeDeliveryStructure situationExchangeDeliveryStructure = list.get(0);
    List<PtSituationElementStructure> ptSituationElements = situationExchangeDeliveryStructure.getSituations().getPtSituationElement();
    PtSituationElementStructure ptSituationElementStructure = ptSituationElements.get(0);
    DefaultedTextStructure description = ptSituationElementStructure.getDescription();
    DefaultedTextStructure summary = ptSituationElementStructure.getSummary();
    assertEquals("description", description.getValue());
    assertEquals("summary", summary.getValue());
    AffectsScopeStructure affects = ptSituationElementStructure.getAffects();
    VehicleJourneys journeys = affects.getVehicleJourneys();
    assertNotNull(journeys);
    List<AffectedVehicleJourneyStructure> list2 = journeys.getAffectedVehicleJourney();
    assertEquals(4, list2.size());
  }

  /**
   * I have sometimes seen vehicleActivities without monitoredVehicleJourneys,
   * which is what we need to hang service alerts off of. That may be an error,
   * but let's make sure we don't blow up if that happens.
   * 
   */
  @Test
  public void testAddSituationExchangeNonEmptyMissingJourney() {

    ServiceAlertBean serviceAlertBean = ServiceAlertsTestSupport.createServiceAlertBean("MTA NYCT_100");

    transitDataService = mock(TransitDataService.class);
    when(transitDataService.getServiceAlertForId(anyString())).thenReturn(
        serviceAlertBean);

    ServiceDelivery serviceDelivery = new ServiceDelivery();
    List<VehicleActivityStructure> activities = new ArrayList<VehicleActivityStructure>();
    createActivity(activities, "MTA NYCT_100", SKIP_JOURNEY);

    addSituationExchangeToServiceDelivery(serviceDelivery, activities, transitDataService);

    List<SituationExchangeDeliveryStructure> list = serviceDelivery.getSituationExchangeDelivery();
    assertEquals(0, list.size());
  }

  @Test
  public void testAddSituationExchangeDuplicate() {
    ServiceAlertBean serviceAlertBean = ServiceAlertsTestSupport.createServiceAlertBean("MTA NYCT_100");

    transitDataService = mock(TransitDataService.class);
    when(transitDataService.getServiceAlertForId(anyString())).thenReturn(
        serviceAlertBean);

    ServiceDelivery serviceDelivery = new ServiceDelivery();
    List<VehicleActivityStructure> activities = new ArrayList<VehicleActivityStructure>();
    createActivity(activities, "MTA NYCT_100");
    createActivity(activities, "MTA NYCT_100");

    addSituationExchangeToServiceDelivery(serviceDelivery, activities, transitDataService);

    List<SituationExchangeDeliveryStructure> list = serviceDelivery.getSituationExchangeDelivery();
    assertEquals(1, list.size());
    SituationExchangeDeliveryStructure situationExchangeDeliveryStructure = list.get(0);
    List<PtSituationElementStructure> ptSituationElements = situationExchangeDeliveryStructure.getSituations().getPtSituationElement();
    PtSituationElementStructure ptSituationElementStructure = ptSituationElements.get(0);
    DefaultedTextStructure description = ptSituationElementStructure.getDescription();
    DefaultedTextStructure summary = ptSituationElementStructure.getSummary();
    assertEquals("description", description.getValue());
    assertEquals("summary", summary.getValue());
  }

  @Test
  public void testAddSituationExchangeDuplicateForStops() {
    ServiceAlertBean serviceAlertBean = ServiceAlertsTestSupport.createServiceAlertBean("MTA NYCT_100");

    transitDataService = mock(TransitDataService.class);
    when(transitDataService.getServiceAlertForId(anyString())).thenReturn(
        serviceAlertBean);

    ServiceDelivery serviceDelivery = new ServiceDelivery();
    List<MonitoredStopVisitStructure> activities = new ArrayList<MonitoredStopVisitStructure>();
    createStopActivity(activities, "MTA NYCT_100");
    createStopActivity(activities, "MTA NYCT_100");

    addSituationExchangeToSiriForStops(serviceDelivery, activities,
        transitDataService, null);

    List<SituationExchangeDeliveryStructure> list = serviceDelivery.getSituationExchangeDelivery();
    assertEquals(1, list.size());
    SituationExchangeDeliveryStructure situationExchangeDeliveryStructure = list.get(0);
    List<PtSituationElementStructure> ptSituationElements = situationExchangeDeliveryStructure.getSituations().getPtSituationElement();
    PtSituationElementStructure ptSituationElementStructure = ptSituationElements.get(0);
    DefaultedTextStructure description = ptSituationElementStructure.getDescription();
    DefaultedTextStructure summary = ptSituationElementStructure.getSummary();
    assertEquals("description", description.getValue());
    assertEquals("summary", summary.getValue());
  }

  @Test
  public void testAddClosedSituationExchangesToSiri() {
    ServiceDelivery serviceDelivery = new ServiceDelivery();
    List<String> deletedIds = new ArrayList<String>();
    deletedIds.add("MTA NYCT_100");
    deletedIds.add("MTA NYCT_101");

    addClosedSituationExchangesToSiri(serviceDelivery, deletedIds);

    List<SituationExchangeDeliveryStructure> list = serviceDelivery.getSituationExchangeDelivery();
    assertEquals(1, list.size());
    SituationExchangeDeliveryStructure situationExchangeDeliveryStructure = list.get(0);
    List<PtSituationElementStructure> ptSituationElements = situationExchangeDeliveryStructure.getSituations().getPtSituationElement();
    assertEquals(2, ptSituationElements.size());
    for (PtSituationElementStructure ptSit : ptSituationElements) {
      assertTrue(deletedIds.contains(ptSit.getSituationNumber().getValue()));
      assertEquals(WorkflowStatusEnumeration.CLOSED, ptSit.getProgress());
    }
  }

  public void createActivity(List<VehicleActivityStructure> activities,
      String id) {
    createActivity(activities, id, false);
  }

  public void createActivity(List<VehicleActivityStructure> activities,
      String id, boolean skipJourney) {
    VehicleActivityStructure vehicleActivity = new VehicleActivityStructure();
    activities.add(vehicleActivity);
    SituationRefStructure situationRefStructure = new SituationRefStructure();

    MonitoredVehicleJourney monitoredVehicleJourney = new MonitoredVehicleJourney();
    // vehicleActivity.setMonitoredVehicleJourney(monitoredVehicleJourney);
    monitoredVehicleJourney.getSituationRef().add(situationRefStructure);

    if (!skipJourney) {
      vehicleActivity.setMonitoredVehicleJourney(monitoredVehicleJourney);
    }

    SituationSimpleRefStructure situationSimpleRef = new SituationSimpleRefStructure();
    situationRefStructure.setSituationSimpleRef(situationSimpleRef);
    situationSimpleRef.setValue(id);
  }

  private void createStopActivity(List<MonitoredStopVisitStructure> activities,
      String id) {
    MonitoredStopVisitStructure activity = new MonitoredStopVisitStructure();
    activities.add(activity);

    MonitoredVehicleJourney monitoredVehicleJourney = new MonitoredVehicleJourney();
    activity.setMonitoredVehicleJourney(monitoredVehicleJourney);

    SituationRefStructure situationRefStructure = new SituationRefStructure();
    monitoredVehicleJourney.getSituationRef().add(situationRefStructure);
    SituationSimpleRefStructure situationSimpleRef = new SituationSimpleRefStructure();
    situationRefStructure.setSituationSimpleRef(situationSimpleRef);
    situationSimpleRef.setValue(id);
  }

}
