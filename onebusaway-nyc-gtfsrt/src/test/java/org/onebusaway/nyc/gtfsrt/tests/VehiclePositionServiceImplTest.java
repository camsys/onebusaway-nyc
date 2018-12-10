package org.onebusaway.nyc.gtfsrt.tests;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.*;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.nyc.gtfsrt.impl.VehicleUpdateServiceImpl;
import org.onebusaway.nyc.gtfsrt.service.VehicleUpdateFeedBuilder;
import org.onebusaway.nyc.presentation.service.realtime.PresentationService;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.realtime.api.OccupancyStatus;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripForVehicleQueryBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VehiclePositionServiceImplTest {
  VehicleUpdateServiceImpl service;
  NycTransitDataService tds;
  PresentationService presentationService;

  @Before
  public void setup() {
    VehicleUpdateFeedBuilder feedBuilder = new VehicleUpdateFeedBuilder() {
      @Override
      public VehiclePosition.Builder makeVehicleUpdate(VehicleStatusBean status, VehicleLocationRecordBean record, OccupancyStatus occupancy) {
        return VehiclePosition.newBuilder()
                .setVehicle(VehicleDescriptor.newBuilder().setId(status.getVehicleId()));
      }
    };

    presentationService = mock(PresentationService.class);
    when(presentationService.include(any(TripStatusBean.class))).thenReturn(Boolean.TRUE);
    doNothing().when(presentationService).setTime(any(Long.class));

    tds = mock(NycTransitDataService.class);
    when(tds.getAgenciesWithCoverage()).thenReturn(UnitTestSupport.agenciesWithCoverage("agency"));
    when(tds.getTripDetailsForVehicleAndTime(any(TripForVehicleQueryBean.class))).thenReturn(new TripDetailsBean());

    service = new VehicleUpdateServiceImpl();
    service.setFeedBuilder(feedBuilder);
    service.setTransitDataService(tds);
    service.setPresentationService(presentationService);
  }

  @Test
  public void testRegularVehiclePositions() {

    ListBean<VehicleStatusBean> vehicles = UnitTestSupport.listBean(vsb("0"), vsb("1"), vsb("2"));
    when(tds.getAllVehiclesForAgency("agency", 0))
            .thenReturn(vehicles);

    when(tds.getVehicleLocationRecordForVehicleId(anyString(), anyLong())).thenReturn(new VehicleLocationRecordBean());

    List<FeedEntity.Builder> entities = service.getEntities(0);
    assertEquals(3, entities.size());
    for (int i = 0; i < entities.size(); i++) {
      FeedEntity.Builder fe = entities.get(i);
      assertFalse(fe.hasAlert());
      assertFalse(fe.hasTripUpdate());
      assertTrue(fe.hasVehicle());
      assertEquals(Integer.toString(i), fe.getId());
    }
  }

  @Test
  public void testVehiclePositionsNoTrip() {
    VehicleStatusBean vsb = new VehicleStatusBean();
    vsb.setVehicleId("1");
    ListBean<VehicleStatusBean> list = UnitTestSupport.listBean(vsb);
    when(tds.getAllVehiclesForAgency("agency", 0)).thenReturn(list);

    List<FeedEntity.Builder> entities = service.getEntities(0);
    assertEquals(0, entities.size());
  }

  @Test
  public void testVehiclePositionsNoVlr() {

    ListBean<VehicleStatusBean> vehicles = UnitTestSupport.listBean(vsb("0"), vsb("1"), vsb("2"));
    when(tds.getAllVehiclesForAgency("agency", 0))
            .thenReturn(vehicles);

    when(tds.getVehicleLocationRecordForVehicleId(anyString(), anyLong())).thenReturn(null);

    List<FeedEntity.Builder> entities = service.getEntities(0);
    assertEquals(0, entities.size());
  }

  private static VehicleStatusBean vsb(String id) {
    VehicleStatusBean vsb = new VehicleStatusBean();
    vsb.setVehicleId(id);
    vsb.setTrip(new TripBean());
    return vsb;
  }
}
