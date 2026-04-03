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
import org.onebusaway.transit_data.model.trip_mods.StopTimeSnapshot;
import org.onebusaway.transit_data.model.trip_mods.TripModificationDiff;
import org.onebusaway.transit_data.model.trips.*;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.AssertionErrors.assertNotEquals;

public class TripUpdateServiceImplTest {
  private TripUpdateServiceImpl service;
  private NycTransitDataService tds;
  private PresentationService presentationService;
  private Method reconstructMethod;


  @Before
  public void setup() throws NoSuchMethodException {
    TripUpdateFeedBuilder feedBuilder = new TripUpdateFeedBuilder() {
      @Override
      public TripUpdate.Builder makeTripUpdate(TripBean trip, VehicleStatusBean vehicle, Collection<TimepointPredictionRecord> records, TripModificationDiff tripModificationDiff) {
        return TripUpdate.newBuilder().setTrip(TripDescriptor.newBuilder().setTripId(trip.getId()));
      }

      @Override
      public TripUpdate.Builder makeTripUpdate(TripBean trip, VehicleStatusBean vehicle, Collection<TimepointPredictionRecord> records) {
        return TripUpdate.newBuilder().setTrip(TripDescriptor.newBuilder().setTripId(trip.getId()));
      }

      @Override
      public TripUpdate.Builder makeCanceledTrip(TripBean trip) {
        TripStatusBean bean = new TripStatusBean();
        bean.setServiceDate(System.currentTimeMillis());
        return GtfsRealtimeLibrary.makeCanceledTrip(trip, bean, null);
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

    reconstructMethod = TripUpdateServiceImpl.class.getDeclaredMethod(
            "reconstructOriginalTripTimepointRecords",
            Collection.class, Map.class);
    reconstructMethod.setAccessible(true);
  }

  @Test
  public void testRegularTripUpdate() {
    VehicleStatusBean vsb = vehicleStatus("vehicle", "block", 0);
    BlockInstanceBean block = blockInstance(blockTrip("MTA_trip"));
    when(tds.getAllVehiclesForAgency("agency", 0)).thenReturn(UnitTestSupport.listBean(vsb));
    when(tds.getBlockInstance("block", 0)).thenReturn(block);
    when(tds.getPredictionRecordsForVehicleAndTripByStopId("vehicle", "MTA_trip"))
            .thenReturn(Collections.singletonMap("stop", tpr("stop", 0, 0)));
    List<FeedEntity.Builder> ret = service.getEntities(0);
    assertEquals(1, ret.size());
    FeedEntity.Builder fe = ret.get(0);
    assertTrip("MTA_trip", fe);
  }

  @Test
  public void testTripUpdateMultipleTripsOnBlockOnePred() {
    VehicleStatusBean vsb = vehicleStatus("vehicle", "block", 0);
    BlockInstanceBean block = blockInstance(blockTrip("MTA_trip0"), blockTrip("trip1"));
    when(tds.getAllVehiclesForAgency("agency", 0)).thenReturn(UnitTestSupport.listBean(vsb));
    when(tds.getBlockInstance("block", 0)).thenReturn(block);
    when(tds.getPredictionRecordsForVehicleAndTripByStopId("vehicle", "MTA_trip0"))
            .thenReturn(Collections.singletonMap("stop", tpr("stop", 0, 0)));
    List<FeedEntity.Builder> ret = service.getEntities(0);
    assertEquals(1, ret.size());
    Collections.sort(ret, ALPHABETIC_BY_ID);
    assertTrip("MTA_trip0", ret.get(0));
  }

  @Test
  public void testTripUpdateMultipleTripsOnBlockTwoPreds() {
    VehicleStatusBean vsb = vehicleStatus("vehicle", "block", 0);
    BlockInstanceBean block = blockInstance(blockTrip("MTA_trip0"), blockTrip("MTA_trip1"));
    when(tds.getAllVehiclesForAgency("agency", 0)).thenReturn(UnitTestSupport.listBean(vsb));
    when(tds.getBlockInstance("block", 0)).thenReturn(block);
    when(tds.getPredictionRecordsForVehicleAndTripByStopId("vehicle", "MTA_trip0"))
            .thenReturn(Collections.singletonMap("stop", tpr("stop", 0, 0)));
    when(tds.getPredictionRecordsForVehicleAndTripByStopId("vehicle", "MTA_trip1"))
            .thenReturn(Collections.singletonMap("stop", tpr("stop", 0, 0)));
    List<FeedEntity.Builder> ret = service.getEntities(0);
    assertEquals(2, ret.size());
    Collections.sort(ret, ALPHABETIC_BY_ID);
    assertTrip("MTA_trip0", ret.get(0));
    assertTrip("MTA_trip1", ret.get(1));
  }

  @Test
  public void test2ndTripOnBlock() {
    VehicleStatusBean vsb = vehicleStatus("vehicle", "block", 1);
    BlockInstanceBean block = blockInstance(blockTrip("MTA_trip0"), blockTrip("MTA_trip1"));
    when(tds.getAllVehiclesForAgency("agency", 0)).thenReturn(UnitTestSupport.listBean(vsb));
    when(tds.getBlockInstance("block", 0)).thenReturn(block);
    when(tds.getPredictionRecordsForVehicleAndTripByStopId("vehicle", "MTA_trip1"))
            .thenReturn(Collections.singletonMap("stop", tpr("stop", 0, 0)));
    List<FeedEntity.Builder> ret = service.getEntities(0);
    assertEquals(1, ret.size());
    assertTrip("MTA_trip1", ret.get(0));
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
    BlockConfigurationBean config = new BlockConfigurationBean();
    config.setTrips(Arrays.asList(trips));
    config.setTimeZone("America/New_York");
    block.setBlockConfiguration(config);
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



  // --- Helpers ---

  @SuppressWarnings("unchecked")
  private Collection<TimepointPredictionRecord> invoke(
          Collection<StopTimeSnapshot> originalTripStopTimes,
          Map<String, TimepointPredictionRecord> tprsByStopId) throws Exception {
    return (Collection<TimepointPredictionRecord>) reconstructMethod.invoke(
            service, originalTripStopTimes, tprsByStopId);
  }

  private StopTimeSnapshot makeSnapshot(String stopId, int gtfsSequence) {
    StopTimeSnapshot snapshot = new StopTimeSnapshot();
    snapshot.setStopId(stopId);
    snapshot.setGtfsSequence(gtfsSequence);
    return snapshot;
  }

  private TimepointPredictionRecord makeRecord(String stopId, int sequence) {
    TimepointPredictionRecord record = new TimepointPredictionRecord();
    record.setTimepointId(AgencyAndId.convertFromString(stopId));
    record.setStopSequence(sequence);
    record.setScheduleRealtionship(
            TimepointPredictionRecord.ScheduleRelationship.SCHEDULED.getValue());
    return record;
  }

  private Map<String, TimepointPredictionRecord> tprMap(TimepointPredictionRecord... records) {
    Map<String, TimepointPredictionRecord> map = new LinkedHashMap<>();
    for (TimepointPredictionRecord r : records) {
      map.put(r.getTimepointId().toString(), r);
    }
    return map;
  }

  private Map<String, TimepointPredictionRecord> indexByStopId(
          Collection<TimepointPredictionRecord> records) {
    Map<String, TimepointPredictionRecord> index = new LinkedHashMap<>();
    for (TimepointPredictionRecord r : records) {
      index.put(r.getTimepointId().toString(), r);
    }
    return index;
  }

  // --- Scenario 1: More added stops than removed stops ---
  //
  // Original trip: A(seq1), B(seq2), F(seq3), G(seq4) — 4 stops
  // Modification: remove B, add E
  // Modified TPR: A(seq1), E(seq2), F(seq3), G(seq4) — sequences rebuilt
  //
  // Expected reconstruction of original trip:
  //   A -> found in TPR, include with original seq 1
  //   B -> not in TPR (was removed), SKIPPED at original seq 2
  //   F -> found in TPR, include with original seq 3
  //   G -> found in TPR, include with original seq 4

  @Test
  public void testMoreAddedThanRemoved_onlyRemovedStopBecomesSkipped() throws Exception {
    List<StopTimeSnapshot> originalStops = Arrays.asList(
            makeSnapshot("STOP_A", 1),
            makeSnapshot("STOP_B", 2),
            makeSnapshot("STOP_F", 3),
            makeSnapshot("STOP_G", 4)
    );

    // Modified trip TPRs — STOP_B replaced by STOP_E, sequences rebuilt
    Map<String, TimepointPredictionRecord> tprs = tprMap(
            makeRecord("STOP_A", 1),
            makeRecord("STOP_E", 2),  // added stop, not in original
            makeRecord("STOP_F", 3),
            makeRecord("STOP_G", 4)
    );

    Collection<TimepointPredictionRecord> result = invoke(originalStops, tprs);
    Map<String, TimepointPredictionRecord> byStopId = indexByStopId(result);

    assertEquals("Should contain 3 normal stops and 1 SKIPPED stop", 4, result.size());

    assertFalse("STOP_E is an added stop and should not appear in original reconstruction",
            byStopId.containsKey("STOP_E"));

    assertEquals("STOP_A should use original sequence 1",
            1, byStopId.get("STOP_A").getStopSequence());
    assertNotEquals("STOP_A should not be SKIPPED",
            TimepointPredictionRecord.ScheduleRelationship.SKIPPED.getValue(),
            byStopId.get("STOP_A").getScheduleRelationship().getValue());

    assertEquals("STOP_B should be SKIPPED at original sequence 2",
            TimepointPredictionRecord.ScheduleRelationship.SKIPPED.getValue(),
            byStopId.get("STOP_B").getScheduleRelationship().getValue());
    assertEquals("STOP_B SKIPPED record should retain original sequence 2",
            2, byStopId.get("STOP_B").getStopSequence());

    assertEquals("STOP_F should use original sequence 3",
            3, byStopId.get("STOP_F").getStopSequence());
    assertEquals("STOP_G should use original sequence 4",
            4, byStopId.get("STOP_G").getStopSequence());
  }

  // --- Scenario 2: More removed stops than added stops ---
  //
  // Original trip: A(seq1), B(seq2), C(seq3), D(seq4), F(seq5) — 5 stops
  // Modification: remove B, C, D and add single stop E
  // Modified TPR: A(seq1), E(seq2), F(seq3) — sequences rebuilt after modification
  //
  // Expected reconstruction:
  //   A -> found in TPR, include with original seq 1
  //   B -> not in TPR, SKIPPED at original seq 2
  //   C -> not in TPR, SKIPPED at original seq 3
  //   D -> not in TPR, SKIPPED at original seq 4
  //   F -> found in TPR, include with original seq 5

  @Test
  public void testMoreRemovedThanAdded_allRemovedStopsBecomeSkipped() throws Exception {
    List<StopTimeSnapshot> originalStops = Arrays.asList(
            makeSnapshot("STOP_A", 1),
            makeSnapshot("STOP_B", 2),
            makeSnapshot("STOP_C", 3),
            makeSnapshot("STOP_D", 4),
            makeSnapshot("STOP_F", 5)
    );

    // Modified TPR reflects rebuilt sequences: E replaces B/C/D, F renumbered to seq 3
    Map<String, TimepointPredictionRecord> tprs = tprMap(
            makeRecord("STOP_A", 1),
            makeRecord("STOP_E", 2),  // single added stop replacing 3 originals
            makeRecord("STOP_F", 3)   // renumbered from original seq 5 to rebuilt seq 3
    );

    Collection<TimepointPredictionRecord> result = invoke(originalStops, tprs);
    Map<String, TimepointPredictionRecord> byStopId = indexByStopId(result);

    assertEquals("Should contain 2 normal stops and 3 SKIPPED stops", 5, result.size());

    assertFalse("STOP_E is an added stop and should not appear in original reconstruction",
            byStopId.containsKey("STOP_E"));

    assertNotEquals("STOP_A should not be SKIPPED",
            TimepointPredictionRecord.ScheduleRelationship.SKIPPED.getValue(),
            byStopId.get("STOP_A").getScheduleRelationship().getValue());
    assertEquals("STOP_A should retain original sequence 1",
            1, byStopId.get("STOP_A").getStopSequence());

    int skippedValue = TimepointPredictionRecord.ScheduleRelationship.SKIPPED.getValue();
    assertEquals("STOP_B should be SKIPPED", skippedValue,
            byStopId.get("STOP_B").getScheduleRelationship().getValue());
    assertEquals("STOP_B should retain original sequence 2",
            2, byStopId.get("STOP_B").getStopSequence());

    assertEquals("STOP_C should be SKIPPED", skippedValue,
            byStopId.get("STOP_C").getScheduleRelationship().getValue());
    assertEquals("STOP_C should retain original sequence 3",
            3, byStopId.get("STOP_C").getStopSequence());

    assertEquals("STOP_D should be SKIPPED", skippedValue,
            byStopId.get("STOP_D").getScheduleRelationship().getValue());
    assertEquals("STOP_D should retain original sequence 4",
            4, byStopId.get("STOP_D").getStopSequence());

    assertNotEquals("STOP_F should not be SKIPPED", skippedValue,
            byStopId.get("STOP_F").getScheduleRelationship().getValue());
    assertEquals("STOP_F should retain original sequence 5",
            5, byStopId.get("STOP_F").getStopSequence());
  }

  // --- Scenario 3: Same number of added and removed stops ---
  //
  // Original trip: A(seq1), B(seq2), C(seq3), F(seq4) — 4 stops
  // Modification: remove B and C, add D and E
  // Modified TPR: A(seq1), D(seq2), E(seq3), F(seq4) — sequences rebuilt
  //
  // Expected reconstruction:
  //   A -> found in TPR, include with original seq 1
  //   B -> not in TPR, SKIPPED at original seq 2
  //   C -> not in TPR, SKIPPED at original seq 3
  //   F -> found in TPR, include with original seq 4

  @Test
  public void testEqualAddedAndRemoved_symmetricSubstitution() throws Exception {
    List<StopTimeSnapshot> originalStops = Arrays.asList(
            makeSnapshot("STOP_A", 1),
            makeSnapshot("STOP_B", 2),
            makeSnapshot("STOP_C", 3),
            makeSnapshot("STOP_F", 4)
    );

    Map<String, TimepointPredictionRecord> tprs = tprMap(
            makeRecord("STOP_A", 1),
            makeRecord("STOP_D", 2),  // replaces STOP_B
            makeRecord("STOP_E", 3),  // replaces STOP_C
            makeRecord("STOP_F", 4)
    );

    Collection<TimepointPredictionRecord> result = invoke(originalStops, tprs);
    Map<String, TimepointPredictionRecord> byStopId = indexByStopId(result);

    assertEquals("Should contain 2 normal stops and 2 SKIPPED stops", 4, result.size());

    assertFalse("STOP_D is an added stop and should not appear", byStopId.containsKey("STOP_D"));
    assertFalse("STOP_E is an added stop and should not appear", byStopId.containsKey("STOP_E"));

    int skippedValue = TimepointPredictionRecord.ScheduleRelationship.SKIPPED.getValue();

    assertEquals("STOP_B should be SKIPPED at original sequence 2", skippedValue,
            byStopId.get("STOP_B").getScheduleRelationship().getValue());
    assertEquals(2, byStopId.get("STOP_B").getStopSequence());

    assertEquals("STOP_C should be SKIPPED at original sequence 3", skippedValue,
            byStopId.get("STOP_C").getScheduleRelationship().getValue());
    assertEquals(3, byStopId.get("STOP_C").getStopSequence());

    assertEquals("STOP_A should retain original sequence 1",
            1, byStopId.get("STOP_A").getStopSequence());
    assertEquals("STOP_F should retain original sequence 4",
            4, byStopId.get("STOP_F").getStopSequence());
  }

  // --- Scenario 4: Original stop sequence preserved regardless of rebuilt TPR sequence ---
  //
  // This verifies that makeOriginalTripRecord uses the original trip's gtfsSequence
  // from the StopTimeSnapshot, not the rebuilt sequence from the TPR. STOP_F moved
  // from original seq 5 to rebuilt seq 3 in the modified trip — the reconstruction
  // must use seq 5 for the original trip entity.

  @Test
  public void testOriginalSequencePreservedOverRebuiltTprSequence() throws Exception {
    List<StopTimeSnapshot> originalStops = Arrays.asList(
            makeSnapshot("STOP_A", 1),
            makeSnapshot("STOP_B", 2),
            makeSnapshot("STOP_C", 3),
            makeSnapshot("STOP_D", 4),
            makeSnapshot("STOP_F", 5)
    );

    // STOP_F's rebuilt seq in modified trip is 3, original was 5
    Map<String, TimepointPredictionRecord> tprs = tprMap(
            makeRecord("STOP_A", 1),
            makeRecord("STOP_E", 2),
            makeRecord("STOP_F", 3)  // rebuilt sequence — original was 5
    );

    Collection<TimepointPredictionRecord> result = invoke(originalStops, tprs);
    Map<String, TimepointPredictionRecord> byStopId = indexByStopId(result);

    assertEquals("STOP_F should appear in reconstruction", true, byStopId.containsKey("STOP_F"));
    assertEquals("STOP_F should use original sequence 5, not rebuilt sequence 3",
            5, byStopId.get("STOP_F").getStopSequence());
  }
}
