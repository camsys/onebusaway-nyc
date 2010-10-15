package org.onebusaway.nyc.vehicle_tracking.impl;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.gtfs.csv.CsvEntityReader;
import org.onebusaway.gtfs.csv.ListEntityHandler;
import org.onebusaway.gtfs.csv.exceptions.CsvEntityIOException;
import org.onebusaway.nyc.vehicle_tracking.model.BaseLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.services.BaseLocationService;
import org.springframework.stereotype.Component;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.strtree.STRtree;

@Component
class BaseLocationServiceImpl implements BaseLocationService {

  private STRtree _tree;

  private List<File> _paths = new ArrayList<File>();

  public void setPath(File path) {
    _paths.add(path);
  }

  @PostConstruct
  public void setup() throws CsvEntityIOException, IOException {

    CsvEntityReader reader = new CsvEntityReader();

    ListEntityHandler<BaseLocationRecord> records = new ListEntityHandler<BaseLocationRecord>();
    reader.addEntityHandler(records);

    for (File path : _paths)
      reader.readEntities(BaseLocationRecord.class, new FileReader(path));

    List<BaseLocationRecord> values = records.getValues();

    _tree = new STRtree(values.size());

    for (BaseLocationRecord record : values) {
      CoordinateBounds b = record.toBounds();
      Envelope env = new Envelope(b.getMinLon(), b.getMaxLon(), b.getMinLat(),
          b.getMaxLat());
      _tree.insert(env, record.getBaseName());
    }

    _tree.build();
  }

  @Override
  public String getBaseNameForLocation(CoordinatePoint location) {

    Envelope env = new Envelope(new Coordinate(location.getLon(),
        location.getLat()));

    @SuppressWarnings("unchecked")
    List<String> values = _tree.query(env);

    if (values.isEmpty())
      return null;

    return values.get(0);
  }
}
