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
package org.onebusaway.nyc.transit_data_federation.services.nyc.model;

import java.util.Map;

import org.onebusaway.csv_entities.*;
import org.onebusaway.csv_entities.exceptions.CsvEntityException;
import org.onebusaway.csv_entities.schema.BeanWrapper;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;

public class GeometryFieldMappingFactory implements org.onebusaway.csv_entities.schema.FieldMappingFactory {

  public org.onebusaway.csv_entities.schema.FieldMapping createFieldMapping(org.onebusaway.csv_entities.schema.EntitySchemaFactory schemaFactory,
      Class<?> entityType, String csvFieldName, String objFieldName,
      Class<?> objFieldType, boolean required) {
    return new FieldMappingImpl(entityType, csvFieldName, objFieldName);
  }

  private static class FieldMappingImpl extends org.onebusaway.csv_entities.schema.AbstractFieldMapping {

    public FieldMappingImpl(Class<?> entityType, String csvFieldName,
        String objFieldName) {
      super(entityType, csvFieldName, objFieldName, true);
    }

    public void translateFromCSVToObject(org.onebusaway.csv_entities.CsvEntityContext context,
        Map<String, Object> csvValues, org.onebusaway.csv_entities.schema.BeanWrapper object) {

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
