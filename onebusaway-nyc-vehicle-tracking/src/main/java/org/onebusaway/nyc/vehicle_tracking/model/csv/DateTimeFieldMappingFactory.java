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
/**
 * 
 */
package org.onebusaway.nyc.vehicle_tracking.model.csv;

import org.onebusaway.csv_entities.CsvEntityContext;
import org.onebusaway.csv_entities.exceptions.CsvEntityException;
import org.onebusaway.csv_entities.schema.AbstractFieldMapping;
import org.onebusaway.csv_entities.schema.BeanWrapper;
import org.onebusaway.csv_entities.schema.EntitySchemaFactory;
import org.onebusaway.csv_entities.schema.FieldMapping;
import org.onebusaway.csv_entities.schema.FieldMappingFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

public class DateTimeFieldMappingFactory implements FieldMappingFactory {

  private static SimpleDateFormat _format = new SimpleDateFormat(
      "yyyy-MM-dd' 'HH:mm:ss");

  static {
    _format.setTimeZone(TimeZone.getTimeZone("America/New_York"));
  }

  @Override
  public FieldMapping createFieldMapping(EntitySchemaFactory schemaFactory,
      Class<?> entityType, String csvFieldName, String objFieldName,
      Class<?> objFieldType, boolean required) {
    return new DateTimeFieldMapping(entityType, csvFieldName, objFieldName,
        required);
  }

  private class DateTimeFieldMapping extends AbstractFieldMapping {

    public DateTimeFieldMapping(Class<?> entityType, String csvFieldName,
        String objFieldName, boolean required) {
      super(entityType, csvFieldName, objFieldName, required);
    }

    @Override
    public void translateFromCSVToObject(CsvEntityContext context,
        Map<String, Object> csvValues, BeanWrapper object)
        throws CsvEntityException {

      if (isMissingAndOptional(csvValues))
        return;

      final String valueAsString = (String) csvValues.get(_csvFieldName);

      try {

        final Date valueAsDate = _format.parse(valueAsString);
        final Class<?> type = object.getPropertyType(_objFieldName);

        if (type.equals(Long.class) || type.equals(Long.TYPE))
          object.setPropertyValue(_objFieldName, valueAsDate.getTime());
        else
          object.setPropertyValue(_objFieldName, valueAsDate);

      } catch (final ParseException e) {
        throw new CsvDateFormatException(_entityType, "bad date-time string:"
            + valueAsString, e);
      }
    }

    @Override
    public void translateFromObjectToCSV(CsvEntityContext context,
        BeanWrapper object, Map<String, Object> csvValues)
        throws CsvEntityException {

      final Object obj = object.getPropertyValue(_objFieldName);
      final Date valueAsDate = obj instanceof Long ? new Date(
          ((Long) obj).longValue()) : (Date) obj;
      final String valueAsString = _format.format(valueAsDate);
      csvValues.put(_csvFieldName, valueAsString);
    }
  }

  public static class CsvDateFormatException extends CsvEntityException {

    public CsvDateFormatException(Class<?> entityType, String message,
        Throwable cause) {
      super(entityType, message, cause);
    }

    private static final long serialVersionUID = 1L;

  }
}