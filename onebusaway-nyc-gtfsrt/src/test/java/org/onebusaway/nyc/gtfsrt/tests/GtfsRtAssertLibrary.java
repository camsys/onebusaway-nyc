/**
 * Copyright (C) 2017 Cambridge Systematics, Inc.
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

import com.google.transit.realtime.GtfsRealtime;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.onebusaway.transit_data.model.trips.TripBean;

import static org.junit.Assert.*;

/**
 * Contains shared testing logic that OBA model classes match RT classes.
 */
public class GtfsRtAssertLibrary {
  public static void assertTripDescriptorMatches(TripBean bean, GtfsRealtime.TripDescriptor desc) {
    assertEquals(AgencyAndId.convertFromString(bean.getId()).getId(), desc.getTripId());
    assertEquals(AgencyAndId.convertFromString(bean.getRoute().getId()).getId(), desc.getRouteId());
    assertEquals(bean.getDirectionId(), Integer.toString(desc.getDirectionId()));
  }

  public static void assertVehicleDescriptorMatches(VehicleLocationRecordBean record, GtfsRealtime.VehicleDescriptor desc) {
    assertEquals(record.getVehicleId(), desc.getId());
  }

}
