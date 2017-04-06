package org.onebusaway.nyc.gtfsrt.tests;

import com.google.transit.realtime.GtfsRealtime;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.onebusaway.transit_data.model.trips.TripBean;

import static org.junit.Assert.*;

public class GtfsRtAssertLibrary {
  public static void assertTripDescriptorMatches(TripBean bean, GtfsRealtime.TripDescriptor desc) {
    assertEquals(bean.getId(), desc.getTripId());
    assertEquals(bean.getRoute().getId(), desc.getRouteId());
    // TODO apparently we are using an old version of gtfs-rt that doesn't have directon (fix)
    // TODO start_time, start_date
  }

  public static void assertVehicleDescriptorMatches(VehicleLocationRecordBean record, GtfsRealtime.VehicleDescriptor desc) {
    assertEquals(record.getVehicleId(), desc.getId());
  }

}
