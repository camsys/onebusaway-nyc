package org.onebusaway.nyc.vehicle_tracking.model;

import org.onebusaway.gtfs.csv.schema.annotations.CsvField;
import org.onebusaway.nyc.vehicle_tracking.impl.GeometryFieldMappingFactory;

import com.vividsolutions.jts.geom.Geometry;

public class BaseLocationRecord {

  private String baseName;

  @CsvField(mapping = GeometryFieldMappingFactory.class)
  private Geometry geometry;

  public String getBaseName() {
    return baseName;
  }

  public void setBaseName(String baseName) {
    this.baseName = baseName;
  }

  public Geometry getGeometry() {
    return geometry;
  }

  public void setGeometry(Geometry geometry) {
    this.geometry = geometry;
  }
}
