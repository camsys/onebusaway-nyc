package org.onebusaway.nyc.gtfsrt.tests;

import com.google.transit.realtime.GtfsRealtime.*;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.gtfsrt.impl.TripUpdateServiceImpl;
import org.onebusaway.nyc.gtfsrt.service.TripUpdateFeedBuilder;
import org.onebusaway.nyc.presentation.service.realtime.PresentationService;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.blocks.BlockConfigurationBean;
import org.onebusaway.transit_data.model.blocks.BlockInstanceBean;
import org.onebusaway.transit_data.model.blocks.BlockTripBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripForVehicleQueryBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;

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

  private BlockTripBean blockTrip(String tripId) {
    BlockTripBean btb = new BlockTripBean();
    btb.setTrip(new TripBean());
    btb.getTrip().setId(tripId);
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
    tpr.setTimepointPredictedTime(time);
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
