package org.onebusaway.nyc.vehicle_tracking.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import javax.annotation.PostConstruct;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.gtfs.csv.CsvEntityReader;
import org.onebusaway.gtfs.csv.ListEntityHandler;
import org.onebusaway.gtfs.csv.exceptions.CsvEntityIOException;
import org.onebusaway.nyc.vehicle_tracking.model.BaseLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.NycTransitDataBundle;
import org.onebusaway.nyc.vehicle_tracking.services.BaseLocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.strtree.STRtree;

@Component
class BaseLocationServiceImpl implements BaseLocationService {

  private GeometryFactory _factory = new GeometryFactory();

  private STRtree _baseLocationTree;

  private STRtree _terminalLocationTree;

  private NycTransitDataBundle _bundle;

  @Autowired
  public void setBundle(NycTransitDataBundle bundle) {
    _bundle = bundle;
  }

  @PostConstruct
  public void setup() throws CsvEntityIOException, IOException {
    _baseLocationTree = readRecordsIntoTree(_bundle.getBaseLocationsPath());
    _terminalLocationTree = readRecordsIntoTree(_bundle.getTerminalLocationsPath());
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

  /****
   * 
   ****/

  private STRtree readRecordsIntoTree(File path) throws IOException,
      FileNotFoundException {

    CsvEntityReader reader = new CsvEntityReader();

    ListEntityHandler<BaseLocationRecord> records = new ListEntityHandler<BaseLocationRecord>();
    reader.addEntityHandler(records);

    if (!path.exists())
      throw new RuntimeException("Your bundle is missing " + path.getName());

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
