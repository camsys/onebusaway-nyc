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

package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.impl.transit_graph.BlockTripEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.TripEntryImpl;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;

import org.locationtech.jts.geom.Coordinate;

@RunWith(MockitoJUnitRunner.class)
public class BlockStateServiceTest {

  @InjectMocks
  BlockStateService service = new BlockStateService();

  @Test
  public void testDetourCheckingValid() throws Exception {
	  ShapePoints shapePoints = new ShapePoints();
	  shapePoints.setShapeId(new AgencyAndId("1", "1"));

	  double [] lats = new double[3];
	  lats[0] = 40.729389;
	  lats[1] = 40.72843;
	  lats[2] = 40.727129;
	  shapePoints.setLats(lats);

	  double [] lons = new double[3];
	  lons[0] = -73.978093;
	  lons[1] = -73.975689;
	  lons[2] = -73.972578;
	  shapePoints.setLons(lons);

	  service.addShapeToDetourGeometryMap(shapePoints);	
	  
	  BlockTripEntryImpl trip = new BlockTripEntryImpl();
	  TripEntryImpl trip2 = new TripEntryImpl();
	  trip2.setShapeId(new AgencyAndId("1", "1"));
	  trip.setTrip(trip2);
	  
	  assertTrue(service.locationIsEligibleForDetour((BlockTripEntry)trip, new CoordinatePoint(40.726901,-73.980389)));
  }

  @Test
  public void testDetourCheckingNotValid() throws Exception {
	  ShapePoints shapePoints = new ShapePoints();
	  shapePoints.setShapeId(new AgencyAndId("1", "1"));

	  double [] lats = new double[3];
	  lats[0] = 40.729389;
	  lats[1] = 40.72843;
	  lats[2] = 40.727129;
	  shapePoints.setLats(lats);

	  double [] lons = new double[3];
	  lons[0] = -73.978093;
	  lons[1] = -73.975689;
	  lons[2] = -73.972578;
	  shapePoints.setLons(lons);
	  
	  service.addShapeToDetourGeometryMap(shapePoints);	
	  
	  BlockTripEntryImpl trip = new BlockTripEntryImpl();
	  TripEntryImpl trip2 = new TripEntryImpl();
	  trip2.setShapeId(new AgencyAndId("1", "1"));
	  trip.setTrip(trip2);
	  
	  assertFalse(service.locationIsEligibleForDetour((BlockTripEntry)trip, new CoordinatePoint(40.671785,-73.959274)));
  }
  
}
