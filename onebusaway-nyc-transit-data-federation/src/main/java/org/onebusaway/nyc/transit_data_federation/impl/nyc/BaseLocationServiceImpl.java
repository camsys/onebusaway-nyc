/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.transit_data_federation.impl.nyc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.onebusaway.csv_entities.CsvEntityReader;
import org.onebusaway.csv_entities.ListEntityHandler;
import org.onebusaway.csv_entities.exceptions.CsvEntityIOException;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.bundle.model.NycFederatedTransitDataBundle;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.NonRevenueStopData;
import org.onebusaway.nyc.transit_data_federation.impl.bundle.NycRefreshableResources;
import org.onebusaway.nyc.transit_data_federation.model.nyc.BaseLocationRecord;
import org.onebusaway.nyc.transit_data_federation.services.nyc.BaseLocationService;
import org.onebusaway.utility.ObjectSerializationLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.strtree.STRtree;

/**
 * Returns the name of a "base"/depot given an input coordinate.
 * 
 * @author jmaki
 *
 */
@Component
class BaseLocationServiceImpl implements BaseLocationService {

  private GeometryFactory _factory = new GeometryFactory();

  private STRtree _baseLocationTree;

  private STRtree _terminalLocationTree;
  
  private Map<AgencyAndId, List<NonRevenueStopData>> _nonRevenueStopDataByTripId = new HashMap<AgencyAndId, List<NonRevenueStopData>>();

  private NycFederatedTransitDataBundle _bundle;

  @Autowired
  public void setBundle(NycFederatedTransitDataBundle bundle) {
    _bundle = bundle;
  }

  @PostConstruct
  @Refreshable(dependsOn = {NycRefreshableResources.TERMINAL_DATA, NycRefreshableResources.NON_REVENUE_STOP_DATA})
  public void setup() throws CsvEntityIOException, IOException, ClassNotFoundException {
    _baseLocationTree = readRecordsIntoTree(_bundle.getBaseLocationsPath());
    _terminalLocationTree = readRecordsIntoTree(_bundle.getTerminalLocationsPath());
    File nonRevenueStopsFile = _bundle.getNonRevenueStopsPath();
    if (nonRevenueStopsFile.exists())
      _nonRevenueStopDataByTripId = ObjectSerializationLibrary.readObject(nonRevenueStopsFile);
  }

  /****
   * {@link BaseLocationService} Interface
   ****/
  @Override
  public String getBaseNameForLocation(CoordinatePoint location) {
    return findNameForLocation(_baseLocationTree, location);
  }

  @Override
  public String getTerminalNameForLocation(CoordinatePoint location) {
    return findNameForLocation(_terminalLocationTree, location);
  }
  
  @Override
  public List<NonRevenueStopData> getNonRevenueStopsForTripId(AgencyAndId tripId) {
    // Returns null if there are not any non revenue stops for the trip.
    // Could return an empty list instead?
    return _nonRevenueStopDataByTripId.get(tripId);
  }

  /****
   * 
   ****/
  private STRtree readRecordsIntoTree(File path) throws IOException,
      FileNotFoundException {

    CsvEntityReader reader = new CsvEntityReader();

    ListEntityHandler<BaseLocationRecord> records = new ListEntityHandler<BaseLocationRecord>();
    reader.addEntityHandler(records);

    if (!path.exists())
    	return null;    

    try {
      reader.readEntities(BaseLocationRecord.class, new FileReader(path));
    } catch (CsvEntityIOException e) {
      throw new RuntimeException("Error parsing CSV file " + path, e);
    }
    
    List<BaseLocationRecord> values = records.getValues();

    STRtree baseLocationTree = new STRtree(values.size());

    for (BaseLocationRecord record : values) {
      Geometry geometry = record.getGeometry();
      Envelope env = geometry.getEnvelopeInternal();
      baseLocationTree.insert(env, record);
    }

    baseLocationTree.build();

    return baseLocationTree;
  }

  private String findNameForLocation(STRtree tree, CoordinatePoint location) {
    Envelope env = new Envelope(new Coordinate(location.getLon(),
        location.getLat()));

    if(tree == null)
    	return null;
    
    @SuppressWarnings("unchecked")
    List<BaseLocationRecord> values = tree.query(env);

    Point point = _factory.createPoint(new Coordinate(location.getLon(),
        location.getLat()));

    for (BaseLocationRecord record : values) {
      Geometry geometry = record.getGeometry();
      if (geometry.contains(point))
        return record.getBaseName();
    }

    return null;
  }

}
