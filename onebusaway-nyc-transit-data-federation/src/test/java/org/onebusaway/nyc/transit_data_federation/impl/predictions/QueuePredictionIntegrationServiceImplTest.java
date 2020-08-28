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
package org.onebusaway.nyc.transit_data_federation.impl.predictions;

import com.google.common.cache.CacheBuilder;
import com.google.transit.realtime.GtfsRealtime.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalAnswers;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.TripStopTimeBean;
import org.onebusaway.transit_data.model.TripStopTimesBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsQueryBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import org.onebusaway.transit_data.services.TransitDataService;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class QueuePredictionIntegrationServiceImplTest {

  private QueuePredictionIntegrationServiceImpl service;

  @Spy
  private MockTransitDataService tds;

  @Before
  public void before() {
    ConfigurationService config = mock(ConfigurationService.class);
    when(config.getConfigurationValueAsString(anyString(), anyString()))
            .thenAnswer(AdditionalAnswers.returnsSecondArg());

    service = new QueuePredictionIntegrationServiceImpl();
    service.setConfigurationService(config);
    service.setTransitDataService(tds);
    service.setCache(CacheBuilder.newBuilder().<String, List<TimepointPredictionRecord>>build());
  }

  @Test
  public void testTripUpdate() {
    FeedMessage msg = FeedMessage.newBuilder()
            .addEntity(FeedEntity.newBuilder()
                    .setTripUpdate(getTripUpdate("trip1", "stop1"))
                    .setId("trip1"))
            .setHeader(FeedHeader.newBuilder().setGtfsRealtimeVersion("1.0").setTimestamp(1))
            .build();
    service.processResult(msg);
    List<TimepointPredictionRecord> records = service.getPredictionRecordsForVehicleAndTrip("vehicle1", "trip1");
    assertEquals(1, records.size());
    TimepointPredictionRecord record = records.get(0);
    assertEquals(new AgencyAndId("1", "stop1"), record.getTimepointId());
    assertEquals(1, record.getStopSequence());
    assertEquals(1, record.getTimepointPredictedArrivalTime());
  }

  @Test
  public void testTwoTripUpdates() {
    FeedMessage msg = FeedMessage.newBuilder()
            .addEntity(FeedEntity.newBuilder()
                    .setTripUpdate(getTripUpdate("trip1", "stop1"))
                    .setId("trip1"))
            .addEntity(FeedEntity.newBuilder()
                    .setTripUpdate(getTripUpdate("trip2", "stop2"))
                    .setId("trip2"))
            .setHeader(FeedHeader.newBuilder().setGtfsRealtimeVersion("1.0").setTimestamp(1))
            .build();
    service.processResult(msg);

    List<TimepointPredictionRecord> records1 = service.getPredictionRecordsForVehicleAndTrip("vehicle1", "trip1");
    assertEquals(1, records1.size());
    TimepointPredictionRecord record1 = records1.get(0);
    assertEquals(new AgencyAndId("1", "stop1"), record1.getTimepointId());
    assertEquals(1, record1.getStopSequence());
    assertEquals(1, record1.getTimepointPredictedArrivalTime());

    List<TimepointPredictionRecord> records2 = service.getPredictionRecordsForVehicleAndTrip("vehicle1", "trip2");
    assertEquals(1, records2.size());
    TimepointPredictionRecord record2 = records2.get(0);
    assertEquals(new AgencyAndId("1", "stop2"), record2.getTimepointId());
    assertEquals(1, record2.getStopSequence());
    assertEquals(1, record2.getTimepointPredictedArrivalTime());
  }

  private TripUpdate.Builder getTripUpdate(String tripId, String stopId) {
    return TripUpdate.newBuilder()
            .setTrip(TripDescriptor.newBuilder().setTripId(tripId))
            .setVehicle(VehicleDescriptor.newBuilder().setId("vehicle1"))
            .addStopTimeUpdate(stopTimeUpdate("1_" + stopId, 1, 1));
  }

  private TripUpdate.StopTimeUpdate.Builder stopTimeUpdate(String stopId, int sequence, int time) {
    return TripUpdate.StopTimeUpdate.newBuilder()
            .setArrival(TripUpdate.StopTimeEvent.newBuilder().setTime(time))
            .setDeparture(TripUpdate.StopTimeEvent.newBuilder().setTime(time))
            .setStopId(stopId)
            .setStopSequence(sequence);
  }

  static abstract class MockTransitDataService implements TransitDataService {
    @Override
    public ListBean<TripDetailsBean> getTripDetails(TripDetailsQueryBean query) {
      TripStatusBean status = new TripStatusBean();
      status.setServiceDate(0);
      TripStopTimeBean st = new TripStopTimeBean();
      st.setStop(new StopBean());
      if (query.getTripId().equals("trip1"))
        st.getStop().setId("1_stop1");
      else if (query.getTripId().equals("trip2"))
        st.getStop().setId("1_stop2");
      else
        return null;
      st.setArrivalTime(1);
      st.setDepartureTime(1);

      TripDetailsBean bean = new TripDetailsBean();
      bean.setSchedule(new TripStopTimesBean());
      bean.getSchedule().addStopTime(st);
      bean.setStatus(status);
      ListBean<TripDetailsBean> list = new ListBean<TripDetailsBean>();
      list.setList(Arrays.asList(bean));
      return list;
    }
  }

}
