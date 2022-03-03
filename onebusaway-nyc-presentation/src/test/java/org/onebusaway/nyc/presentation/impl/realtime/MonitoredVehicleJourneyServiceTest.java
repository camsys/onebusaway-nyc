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

package org.onebusaway.nyc.presentation.impl.realtime;

import org.junit.Test;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.presentation.impl.realtime.siri.OnwardCallsMode;
import org.onebusaway.nyc.presentation.impl.realtime.siri.SiriBuilderServiceHelperImpl;
import org.onebusaway.nyc.presentation.impl.realtime.siri.SiriMonitoredCallBuilderServiceImpl;
import org.onebusaway.nyc.presentation.impl.realtime.siri.SiriMonitoredVehicleJourneyBuilderServiceImpl;
import org.onebusaway.nyc.presentation.service.realtime.PresentationService;
import org.onebusaway.nyc.presentation.service.realtime.siri.SiriOnwardCallsBuilderService;
import org.onebusaway.nyc.siri.support.SiriDistanceExtension;
import org.onebusaway.nyc.siri.support.SiriExtensionWrapper;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data.model.*;
import org.onebusaway.transit_data.model.RouteBean.Builder;
import org.onebusaway.transit_data.model.blocks.BlockConfigurationBean;
import org.onebusaway.transit_data.model.blocks.BlockInstanceBean;
import org.onebusaway.transit_data.model.blocks.BlockStopTimeBean;
import org.onebusaway.transit_data.model.blocks.BlockTripBean;
import org.onebusaway.transit_data.model.schedule.StopTimeBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import uk.org.siri.siri.ProgressRateEnumeration;
import uk.org.siri.siri.SituationRefStructure;
import uk.org.siri.siri.SituationSimpleRefStructure;
import uk.org.siri.siri.VehicleActivityStructure.MonitoredVehicleJourney;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MonitoredVehicleJourneyServiceTest {

  private static final String MOCK_SERVICE_ALERT_ID = "mock service alert id";
  private static final String STOP_ID = "1";

  @Test
  public void testGetMonitoredVehicleJourney() {
    TripDetailsBean tripDetails = getMockTripDetailsBean();
    TripDetailsBean futureTripDetails = getMockFutureTripDetailsBean();


    PresentationService presentationService = mock(PresentationService.class);
    NycTransitDataService nycTransitDataService = mock(NycTransitDataService.class);
    //SiriMonitoredCallBuilderService siriMonitoredCallBuilderService = mock(SiriMonitoredCallBuilderService.class);
    SiriOnwardCallsBuilderService siriOnwardCallsBuilderService = mock(SiriOnwardCallsBuilderService.class);

    BlockInstanceBean blockInstance = new BlockInstanceBean();
    BlockConfigurationBean blockConfig = new BlockConfigurationBean();
    blockConfig.setBlockId("BLOCK");
    blockConfig.setTrips(getBlockTrips());
    blockInstance.setBlockConfiguration(blockConfig);

    when(nycTransitDataService.getBlockInstance(eq("BLOCK"), anyLong())).thenReturn(blockInstance);
    when(nycTransitDataService.getLastVehicleOccupancyRecordForVehicleId(any(AgencyAndId.class))).thenReturn(null);
    when(nycTransitDataService.stopHasRevenueServiceOnRoute(anyString(),anyString(),anyString(),anyString())).thenReturn(null);

    //when(siriMonitoredCallBuilderService.makeMonitoredCall(any(BlockInstanceBean.class), any(TripBean.class), any(TripStatusBean.class), any(StopBean.class), anyMap(), anyBoolean(), anyLong())).thenReturn(new MonitoredCallStructure());

    when(presentationService.hasFormalBlockLevelMatch(any(TripStatusBean.class))).thenReturn(true);
    when(presentationService.getPresentableDistance(any(SiriDistanceExtension.class))).thenReturn("Distance Here");
    when(presentationService.isOnDetour(any(TripStatusBean.class))).thenReturn(false);

    when(siriOnwardCallsBuilderService.makeOnwardCalls(any(BlockInstanceBean.class), any(TripBean.class), any(TripStatusBean.class), any(OnwardCallsMode.class), anyMap(), anyInt(), anyLong())).thenReturn(null);

    StopBean monitoredCallStopBean = mock(StopBean.class);
    when(monitoredCallStopBean.getId()).thenReturn(STOP_ID);

    SiriMonitoredVehicleJourneyBuilderServiceImpl mvjService =
            new SiriMonitoredVehicleJourneyBuilderServiceImpl();


    SiriBuilderServiceHelperImpl siriBuilderServiceHelper = new SiriBuilderServiceHelperImpl();
    siriBuilderServiceHelper.setNycTransitDataService(nycTransitDataService);

    SiriMonitoredCallBuilderServiceImpl monitoredCallBuilderService = new SiriMonitoredCallBuilderServiceImpl();
    monitoredCallBuilderService.setNycTransitDataService(nycTransitDataService);
    monitoredCallBuilderService.setPresentationService(presentationService);
    monitoredCallBuilderService.setSiriBuilderServiceHelper(siriBuilderServiceHelper);

    mvjService.setNycTransitDataService(nycTransitDataService);
    mvjService.setPresentationService(presentationService);
    mvjService.setMonitoredCallBuilderService(monitoredCallBuilderService);
    mvjService.setSiriOnwardCallsBuilderService(siriOnwardCallsBuilderService);


    MonitoredVehicleJourney journey = mvjService.makeMonitoredVehicleJourney(tripDetails.getTrip(), tripDetails.getStatus(),
            null, Collections.emptyMap(), OnwardCallsMode.VEHICLE_MONITORING,
            0, System.currentTimeMillis(), Boolean.FALSE, Boolean.FALSE, Boolean.FALSE);

    MonitoredVehicleJourney futureJourney = mvjService.makeMonitoredVehicleJourney(futureTripDetails.getTrip(), futureTripDetails.getStatus(),
            null, Collections.emptyMap(), OnwardCallsMode.VEHICLE_MONITORING,
            0, System.currentTimeMillis(), Boolean.FALSE, Boolean.FALSE, Boolean.FALSE);

    assertNotNull(journey);
    assertNotNull(futureJourney);

    // Check route, trip, block and progress
    assertEquals("foo", journey.getLineRef().getValue());
    assertEquals("TEST_TRIP_ID", journey.getFramedVehicleJourneyRef().getDatedVehicleJourneyRef());
    assertEquals("BLOCK", journey.getBlockRef().getValue());
    assertEquals(ProgressRateEnumeration.NORMAL_PROGRESS, journey.getProgressRate());
    assertNull(journey.getProgressStatus());

    // Check route, trip, block and progress on future trip
    assertEquals("foo", futureJourney.getLineRef().getValue());
    assertEquals("TEST_TRIP_ID_2", futureJourney.getFramedVehicleJourneyRef().getDatedVehicleJourneyRef());
    assertEquals("BLOCK", futureJourney.getBlockRef().getValue());
    assertEquals(ProgressRateEnumeration.NORMAL_PROGRESS, futureJourney.getProgressRate());
    assertEquals("prevTrip", futureJourney.getProgressStatus().getValue());

    // Check for fix where future trips on the same block and vehicle (loops)
    // would copy the distance (and other info eg. predictions) from the current trip
    // causing duplicate entries to appear
    assertNotNull(journey.getMonitoredCall());
    assertNotNull(futureJourney.getMonitoredCall());

    SiriExtensionWrapper distanceExtension = (SiriExtensionWrapper) journey.getMonitoredCall().getExtensions().getAny();
    SiriExtensionWrapper distanceExtension2 = (SiriExtensionWrapper) futureJourney.getMonitoredCall().getExtensions().getAny();

    assertNotSame(distanceExtension.getDistances().getStopsFromCall(), distanceExtension2.getDistances().getStopsFromCall());

    List<SituationRefStructure> situationRefs = journey.getSituationRef();
    assertNotNull(situationRefs);
    assertEquals(1, situationRefs.size());
    SituationRefStructure situationRef = situationRefs.get(0);
    SituationSimpleRefStructure simpleRef = situationRef.getSituationSimpleRef();
    assertEquals(MOCK_SERVICE_ALERT_ID, simpleRef.getValue());

  }

  public TripDetailsBean getMockTripDetailsBean() {
    TripDetailsBean tripDetails = new TripDetailsBean();

    // Setup Trip Bean
    TripBean tripBean = getMockTripBean("TEST_TRIP_ID", "foo", "BLOCK");
    tripDetails.setTrip(tripBean);

    // Setup TripStatusBean
    TripStatusBean status = getMockTripStatusBean();
    tripDetails.setStatus(status);

    // Setup Trip StopTimes
    TripStopTimesBean schedule = new TripStopTimesBean();
    List<TripStopTimeBean> stopTimes = getTripStopTimes(100);
    schedule.setStopTimes(stopTimes);
    tripDetails.setSchedule(schedule);

    // Set Situations
    List<ServiceAlertBean> situations = getMockSituations();
    status.setSituations(situations);
    tripDetails.setSituations(situations);

    return tripDetails;
  }

  public TripDetailsBean getMockFutureTripDetailsBean() {
    TripDetailsBean tripDetails = new TripDetailsBean();

    // Setup Trip Bean
    TripBean tripBean = getMockTripBean("TEST_TRIP_ID_2", "foo", "BLOCK");
    tripDetails.setTrip(tripBean);

    // Setup TripStatusBean
    TripStatusBean status = getMockTripStatusBean();
    tripDetails.setStatus(status);

    // Setup Trip StopTimes
    TripStopTimesBean schedule = new TripStopTimesBean();
    List<TripStopTimeBean> stopTimes = getTripStopTimes(1000);
    schedule.setStopTimes(stopTimes);
    tripDetails.setSchedule(schedule);

    // Set Situations
    List<ServiceAlertBean> situations = getMockSituations();
    status.setSituations(situations);
    tripDetails.setSituations(situations);

    return tripDetails;
  }

  private TripBean getMockTripBean(String tripId, String routeId, String blockId){
    TripBean tripBean = new TripBean();
    tripBean.setId(tripId);
    Builder routeBuilder = RouteBean.builder();
    routeBuilder.setAgency(new AgencyBean());
    routeBuilder.setId(routeId);

    tripBean.setRoute(routeBuilder.create());
    tripBean.setBlockId(blockId);

    return tripBean;
  }

  private TripStatusBean getMockTripStatusBean(){
    TripBean tripBean = getMockTripBean("TEST_TRIP_ID", "foo", "BLOCK");
    StopBean stop = new StopBean();
    stop.setId("3");

    TripStatusBean status = new TripStatusBean();
    CoordinatePoint location = new CoordinatePoint(90.0, 90.0);

    status.setLocation(location );
    status.setPhase("IN_PROGRESS");
    status.setActiveTrip(tripBean);
    status.setStatus("normal");
    status.setDistanceAlongTrip(250);
    status.setVehicleId("MTA NYCT_1");
    status.setNextStop(stop);

    return status;
  }

  private List<BlockTripBean> getBlockTrips(){
    List<BlockTripBean> blockTripBeans = new ArrayList<>();

    BlockTripBean blockTrip = new BlockTripBean();
    blockTrip.setTrip(getMockTripBean("TEST_TRIP_ID","foo","BLOCK"));
    blockTrip.setBlockStopTimes(getBlockStopTimes(100, 100));
    blockTrip.setDistanceAlongBlock(0);
    blockTripBeans.add(blockTrip);

    BlockTripBean blockTrip2 = new BlockTripBean();
    blockTrip2.setTrip(getMockTripBean("TEST_TRIP_ID_2","foo","BLOCK"));
    blockTrip2.setBlockStopTimes(getBlockStopTimes(1000, 600));
    blockTrip2.setDistanceAlongBlock(600);
    blockTripBeans.add(blockTrip2);

    return blockTripBeans;
  }

  private List<BlockStopTimeBean> getBlockStopTimes(int startTimeSec, int startDistanceMeters){
    List<BlockStopTimeBean> blockStopTimeBeans = new ArrayList<>();
    for(int i=1; i < 6; i++ ){
      StopBean stop = new StopBean();
      stop.setId(Integer.toString(i));

      StopTimeBean stopTimeBean = new StopTimeBean();
      stopTimeBean.setArrivalTime(startTimeSec + (100*i));
      stopTimeBean.setDepartureTime(startTimeSec+ (100*i));
      stopTimeBean.setStop(stop);

      BlockStopTimeBean blockStopTime = new BlockStopTimeBean();
      blockStopTime.setDistanceAlongBlock(startDistanceMeters +(100 *i));
      blockStopTime.setStopTime(stopTimeBean);

      blockStopTimeBeans.add(blockStopTime);
    }
    return blockStopTimeBeans;
  }

  private List<TripStopTimeBean> getTripStopTimes(int startTimeSec){
    List<TripStopTimeBean> tripStopTimeBeans = new ArrayList<>();

    for(int i=1; i < 6; i++ ){
      StopBean stop = new StopBean();
      stop.setId(Integer.toString(i));

      TripStopTimeBean tripStopTime = new TripStopTimeBean();
      tripStopTime.setArrivalTime(startTimeSec + (100*i));
      tripStopTime.setDepartureTime(startTimeSec*i);
      tripStopTime.setStop(stop);
      tripStopTime.setDistanceAlongTrip(i*100);

      tripStopTimeBeans.add(tripStopTime);
    }
    return tripStopTimeBeans;
  }

  private List<ServiceAlertBean> getMockSituations(){
    List<ServiceAlertBean> situations = new ArrayList<ServiceAlertBean>();
    ServiceAlertBean serviceAlert = new ServiceAlertBean();
    serviceAlert.setId(MOCK_SERVICE_ALERT_ID);
    situations.add(serviceAlert);
    return situations;
  }

}
