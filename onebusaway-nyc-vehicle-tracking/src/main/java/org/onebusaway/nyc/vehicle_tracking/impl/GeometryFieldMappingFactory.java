package org.onebusaway.nyc.vehicle_tracking.impl;

import java.util.Map;

import org.onebusaway.gtfs.csv.CsvEntityContext;
import org.onebusaway.gtfs.csv.exceptions.CsvEntityException;
import org.onebusaway.gtfs.csv.schema.AbstractFieldMapping;
import org.onebusaway.gtfs.csv.schema.BeanWrapper;
import org.onebusaway.gtfs.csv.schema.EntitySchemaFactory;
import org.onebusaway.gtfs.csv.schema.FieldMapping;
import org.onebusaway.gtfs.csv.schema.FieldMappingFactory;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;

public class GeometryFieldMappingFactory implements FieldMappingFactory {

  public FieldMapping createFieldMapping(EntitySchemaFactory schemaFactory,
      Class<?> entityType, String csvFieldName, String objFieldName,
      Class<?> objFieldType, boolean required) {
    return new FieldMappingImpl(entityType, csvFieldName, objFieldName);
  }

  private static class FieldMappingImpl extends AbstractFieldMapping {

    public FieldMappingImpl(Class<?> entityType, String csvFieldName,
        String objFieldName) {
      super(entityType, csvFieldName, objFieldName, true);
    }

    public void translateFromCSVToObject(CsvEntityContext context,
        Map<String, Object> csvValues, BeanWrapper object) {

      if (isMissingAndOptional(csvValues))
        return;

      Object value = csvValues.get(_csvFieldName);

      try {
        Geometry geometry = new WKTReader(new GeometryFactory()).read(value.toString());
        object.setPropertyValue(_objFieldName, geometry);
      } catch (ParseException ex) {
        throw new GeometryCsvEntityException(_entityType, "error parsing WKT: "
            + value, ex);
      }
    }

    public void translateFromObjectToCSV(CsvEntityContext context,
        BeanWrapper object, Map<String, Object> csvValues) {

      Geometry geometry = (Geometry) object.getPropertyValue(_objFieldName);
      WKTWriter writer = new WKTWriter();
      String value = writer.write(geometry);
      csvValues.put(_csvFieldName, value);
    }
  }

  public static class GeometryCsvEntityException extends CsvEntityException {

    private static final long serialVersionUID = 1L;

    public GeometryCsvEntityException(Class<?> entityType, String message,
        Throwable cause) {
      super(entityType, message, cause);
    }
  }
}
