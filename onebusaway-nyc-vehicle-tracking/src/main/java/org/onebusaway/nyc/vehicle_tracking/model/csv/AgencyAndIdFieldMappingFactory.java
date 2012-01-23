/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
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
package org.onebusaway.nyc.vehicle_tracking.model.csv;

import org.onebusaway.csv_entities.CsvEntityContext;
import org.onebusaway.csv_entities.schema.BeanWrapper;
import org.onebusaway.csv_entities.schema.DefaultFieldMapping;
import org.onebusaway.csv_entities.schema.EntitySchemaFactory;
import org.onebusaway.csv_entities.schema.FieldMapping;
import org.onebusaway.csv_entities.schema.FieldMappingFactory;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;

import java.util.Collection;
import java.util.Map;

public class AgencyAndIdFieldMappingFactory implements FieldMappingFactory {

  @Override
  public FieldMapping createFieldMapping(EntitySchemaFactory schemaFactory,
      Class<?> entityType, String csvFieldName, String objFieldName,
      Class<?> objFieldType, boolean required) {

    return new FieldMappingImpl(entityType, csvFieldName, objFieldName,
        String.class, required);
  }

  private class FieldMappingImpl extends DefaultFieldMapping {

    public FieldMappingImpl(Class<?> entityType, String csvFieldName,
        String objFieldName, Class<?> objFieldType, boolean required) {
      super(entityType, csvFieldName, objFieldName, objFieldType, required);
    }

    @Override
    public void getCSVFieldNames(Collection<String> names) {
      names.add(_csvFieldName);
    }

    @Override
    public void translateFromObjectToCSV(CsvEntityContext context,
        BeanWrapper object, Map<String, Object> csvValues) {
      AgencyAndId id = (AgencyAndId) object.getPropertyValue(_objFieldName);
      csvValues.put(_csvFieldName, AgencyAndIdLibrary.convertToString(id));
    }

    @Override
    public void translateFromCSVToObject(CsvEntityContext context,
        Map<String, Object> csvValues, BeanWrapper object) {

      String value = (String) csvValues.get(_csvFieldName);
      AgencyAndId agencyAndId = AgencyAndIdLibrary.convertFromString(value);
      object.setPropertyValue(_objFieldName, agencyAndId);
    }
  }
}
