package org.onebusaway.nyc.presentation.impl.realtime;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.nyc.presentation.impl.realtime.siri.SiriSupport;
import org.onebusaway.nyc.presentation.service.realtime.PresentationService;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.RouteBean.Builder;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.TripStopTimeBean;
import org.onebusaway.transit_data.model.TripStopTimesBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;

import uk.org.siri.siri.SituationRefStructure;
import uk.org.siri.siri.SituationSimpleRefStructure;
import uk.org.siri.siri.VehicleActivityStructure.MonitoredVehicleJourney;

public class SiriSupportTest extends SiriSupport {

  private static final String MOCK_SERVICE_ALERT_ID = "mock service alert id";
  private static final String STOP_ID = "stop id";
  private static final double TRIP_STATUS_BEAN_DISTANCE_ALONG_TRIP = 1.0;

  @Test
  public void testGetMonitoredVehicleJourney() {    
    TripDetailsBean trip = setupMock();
    PresentationService presentationService = mock(PresentationService.class);
    setPresentationService(presentationService );

    StopBean monitoredCallStopBean = mock(StopBean.class);
    when(monitoredCallStopBean.getId()).thenReturn(STOP_ID);
    boolean includeOnwardCalls = false;
    MonitoredVehicleJourney journey = getMonitoredVehicleJourney(trip.getTrip(), trip, monitoredCallStopBean, includeOnwardCalls);
    assertNotNull(journey);
    List<SituationRefStructure> situationRefs = journey.getSituationRef();
    assertNotNull(situationRefs);
    assertEquals(1, situationRefs.size());
    SituationRefStructure situationRef = situationRefs.get(0);
    SituationSimpleRefStructure simpleRef = situationRef.getSituationSimpleRef();
    assertEquals(MOCK_SERVICE_ALERT_ID, simpleRef.getValue());
    
  }

  public TripDetailsBean setupMock() {
    TripDetailsBean tripDetails = new TripDetailsBean();
    TripBean tripBean = new TripBean();
    tripDetails.setTrip(tripBean);
    Builder routeBeanBuilder = RouteBean.builder();
    routeBeanBuilder.setId("foo");
    RouteBean route = routeBeanBuilder.create();
    tripBean.setRoute(route);
    TripStatusBean status = new TripStatusBean();
    CoordinatePoint location = new CoordinatePoint(90.0, 90.0);
    status.setLocation(location );
    status.setStatus("normal");
    status.setDistanceAlongTrip(TRIP_STATUS_BEAN_DISTANCE_ALONG_TRIP);
    tripDetails.setStatus(status);
    TripStopTimesBean schedule = new TripStopTimesBean();
    List<TripStopTimeBean> stopTimes = new ArrayList<TripStopTimeBean>();
    TripStopTimeBean tripStopTimeBean = new TripStopTimeBean();
    StopBean stop = new StopBean();
    stop.setId(STOP_ID);
    tripStopTimeBean.setStop(stop );
    tripStopTimeBean.setDistanceAlongTrip(TRIP_STATUS_BEAN_DISTANCE_ALONG_TRIP + 0.5);
    stopTimes.add(tripStopTimeBean );
    schedule.setStopTimes(stopTimes );
    tripDetails.setSchedule(schedule );
    
    List<ServiceAlertBean> situations = new ArrayList<ServiceAlertBean>();
    ServiceAlertBean serviceAlert = new ServiceAlertBean();
    serviceAlert.setId(MOCK_SERVICE_ALERT_ID);
    situations.add(serviceAlert );
    tripDetails.setSituations(situations );
    return tripDetails;
  }

}
