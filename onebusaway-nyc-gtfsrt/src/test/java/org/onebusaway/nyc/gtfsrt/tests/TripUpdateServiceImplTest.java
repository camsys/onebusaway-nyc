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

package org.onebusaway.nyc.gtfsrt.tests;

import com.google.transit.realtime.GtfsRealtime.*;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.gtfsrt.impl.TripUpdateServiceImpl;
import org.onebusaway.nyc.gtfsrt.service.TripUpdateFeedBuilder;
import org.onebusaway.nyc.gtfsrt.util.GtfsRealtimeLibrary;
import org.onebusaway.nyc.presentation.service.realtime.PresentationService;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.blocks.BlockConfigurationBean;
import org.onebusaway.transit_data.model.blocks.BlockInstanceBean;
import org.onebusaway.transit_data.model.blocks.BlockTripBean;
import org.onebusaway.transit_data.model.trips.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TripUpdateServiceImplTest {
  private TripUpdateServiceImpl service;
  private NycTransitDataService tds;
  private PresentationService presentationService;

  @Before
  public void setup() {
    TripUpdateFeedBuilder feedBuilder = new TripUpdateFeedBuilder() {
      @Override
      public TripUpdate.Builder makeTripUpdate(TripBean trip, VehicleStatusBean vehicle, List<TimepointPredictionRecord> records) {
        return TripUpdate.newBuilder().setTrip(TripDescriptor.newBuilder().setTripId(trip.getId()));
      }

      @Override
      public TripUpdate.Builder makeCanceledTrip(TripBean trip) {
        TripStatusBean bean = new TripStatusBean();
        bean.setServiceDate(System.currentTimeMillis());
        return GtfsRealtimeLibrary.makeCanceledTrip(trip, bean);
      }
    };

    presentationService = mock(PresentationService.class);
    when(presentationService.include(any(TripStatusBean.class))).thenReturn(Boolean.TRUE);
    doNothing().when(presentationService).setTime(any(Long.class));

    tds = mock(NycTransitDataService.class);
    when(tds.getAgenciesWithCoverage()).thenReturn(UnitTestSupport.agenciesWithCoverage("agency"));
    when(tds.getTripDetailsForVehicleAndTime(any(TripForVehicleQueryBean.class))).thenReturn(new TripDetailsBean());

    service = new TripUpdateServiceImpl();
    service.setFeedBuilder(feedBuilder);
    service.setTransitDataService(tds);
    service.setPresentationService(presentationService);
  }

  @Test
  public void testRegularTripUpdate() {
    VehicleStatusBean vsb = vehicleStatus("vehicle", "block", 0);
    BlockInstanceBean block = blockInstance(blockTrip("trip"));
    when(tds.getAllVehiclesForAgency("agency", 0)).thenReturn(UnitTestSupport.listBean(vsb));
    when(tds.getBlockInstance("block", 0)).thenReturn(block);
    when(tds.getPredictionRecordsForVehicleAndTrip("vehicle", "trip"))
            .thenReturn(Collections.singletonList(tpr("stop", 0, 0)));
    List<FeedEntity.Builder> ret = service.getEntities(0);
    assertEquals(1, ret.size());
    FeedEntity.Builder fe = ret.get(0);
    assertTrip("trip", fe);
  }

  @Test
  public void testTripUpdateMultipleTripsOnBlockOnePred() {
    VehicleStatusBean vsb = vehicleStatus("vehicle", "block", 0);
    BlockInstanceBean block = blockInstance(blockTrip("trip0"), blockTrip("trip1"));
    when(tds.getAllVehiclesForAgency("agency", 0)).thenReturn(UnitTestSupport.listBean(vsb));
    when(tds.getBlockInstance("block", 0)).thenReturn(block);
    when(tds.getPredictionRecordsForVehicleAndTrip("vehicle", "trip0"))
            .thenReturn(Collections.singletonList(tpr("stop", 0, 0)));
    List<FeedEntity.Builder> ret = service.getEntities(0);
    assertEquals(1, ret.size());
    Collections.sort(ret, ALPHABETIC_BY_ID);
    assertTrip("trip0", ret.get(0));
  }

  @Test
  public void testTripUpdateMultipleTripsOnBlockTwoPreds() {
    VehicleStatusBean vsb = vehicleStatus("vehicle", "block", 0);
    BlockInstanceBean block = blockInstance(blockTrip("trip0"), blockTrip("trip1"));
    when(tds.getAllVehiclesForAgency("agency", 0)).thenReturn(UnitTestSupport.listBean(vsb));
    when(tds.getBlockInstance("block", 0)).thenReturn(block);
    when(tds.getPredictionRecordsForVehicleAndTrip("vehicle", "trip0"))
            .thenReturn(Collections.singletonList(tpr("stop", 0, 0)));
    when(tds.getPredictionRecordsForVehicleAndTrip("vehicle", "trip1"))
            .thenReturn(Collections.singletonList(tpr("stop", 0, 0)));
    List<FeedEntity.Builder> ret = service.getEntities(0);
    assertEquals(2, ret.size());
    Collections.sort(ret, ALPHABETIC_BY_ID);
    assertTrip("trip0", ret.get(0));
    assertTrip("trip1", ret.get(1));
  }

  @Test
  public void test2ndTripOnBlock() {
    VehicleStatusBean vsb = vehicleStatus("vehicle", "block", 1);
    BlockInstanceBean block = blockInstance(blockTrip("trip0"), blockTrip("trip1"));
    when(tds.getAllVehiclesForAgency("agency", 0)).thenReturn(UnitTestSupport.listBean(vsb));
    when(tds.getBlockInstance("block", 0)).thenReturn(block);
    when(tds.getPredictionRecordsForVehicleAndTrip("vehicle", "trip1"))
            .thenReturn(Collections.singletonList(tpr("stop", 0, 0)));
    List<FeedEntity.Builder> ret = service.getEntities(0);
    assertEquals(1, ret.size());
    assertTrip("trip1", ret.get(0));
  }

  @Test
  public void testCancelled() {
    List <CancelledTripBean> canceledList = new ArrayList<>();
    CancelledTripBean cancelledTripBean = new CancelledTripBean();
    cancelledTripBean.setTrip("1_trip1");
    canceledList.add(cancelledTripBean);
    ListBean<CancelledTripBean> beans = new ListBean(canceledList, false);

    VehicleStatusBean vsb = vehicleStatus("vehicle", "1_block", 1);
    BlockInstanceBean block = blockInstance(blockTrip("1_trip0"), blockTripRoute("1_trip1", "1_route1"));
    when(tds.getAllVehiclesForAgency("agency", 0)).thenReturn(UnitTestSupport.listBean(vsb));
    when(tds.getBlockInstance("1_block", 0)).thenReturn(block);
    when(tds.getAllCancelledTrips()).thenReturn(beans);
    when(tds.getTrip("1_trip1")).thenReturn(blockTripRoute("1_trip1", "1_route1").getTrip());
    List<FeedEntity.Builder> entities = service.getEntities(0);
    assertTrue(entities.size() == 1);
    assertEquals("trip1", entities.get(0).getId());  // note that agency doesn't come back
    assertEquals(TripDescriptor.ScheduleRelationship.CANCELED, entities.get(0).getTripUpdate().getTrip().getScheduleRelationship());
  }

  private BlockTripBean blockTrip(String tripId) {
    BlockTripBean btb = new BlockTripBean();
    btb.setTrip(new TripBean());
    btb.getTrip().setId(tripId);
    return btb;
  }

  private BlockTripBean blockTripRoute(String tripId, String routeId) {
    BlockTripBean btb = new BlockTripBean();
    btb.setTrip(new TripBean());
    btb.getTrip().setId(tripId);
    RouteBean.Builder routeBuilder = RouteBean.builder();
    routeBuilder.setId(routeId);
    btb.getTrip().setRoute(routeBuilder.create());
    btb.getTrip().setDirectionId("0");
    return btb;
  }

  private BlockInstanceBean blockInstance(BlockTripBean... trips) {
    BlockInstanceBean block = new BlockInstanceBean();
    block.setBlockConfiguration(new BlockConfigurationBean());
    block.getBlockConfiguration().setTrips(Arrays.asList(trips));
    return block;
  }

  private VehicleStatusBean vehicleStatus(String vehicle, String block, int tripSequence) {
    VehicleStatusBean vsb = new VehicleStatusBean();
    vsb.setVehicleId(vehicle);
    vsb.setTripStatus(new TripStatusBean());
    vsb.getTripStatus().setBlockTripSequence(tripSequence);
    vsb.setTrip(new TripBean());
    vsb.getTrip().setBlockId(block);
    return vsb;
  }

  private TimepointPredictionRecord tpr(String stop, int sequence, int time) {
    TimepointPredictionRecord tpr = new TimepointPredictionRecord();
    tpr.setStopSequence(sequence);
    tpr.setTimepointId(new AgencyAndId("agency", stop));
    tpr.setTimepointPredictedArrivalTime(time);
    tpr.setTimepointScheduledTime(time);
    return tpr;
  }

  private void assertTrip(String trip, FeedEntityOrBuilder fe) {
    assertTrue(fe.hasTripUpdate());
    assertFalse(fe.hasVehicle());
    assertFalse(fe.hasAlert());

    assertEquals(trip, fe.getTripUpdate().getTrip().getTripId());
    // convention:
    assertEquals(trip, fe.getId());
  }

  private static final Comparator<FeedEntityOrBuilder> ALPHABETIC_BY_ID = new Comparator<FeedEntityOrBuilder>() {
    @Override
    public int compare(FeedEntityOrBuilder o1, FeedEntityOrBuilder o2) {
      return o1.getId().compareTo(o2.getId());
    }
  };
}
