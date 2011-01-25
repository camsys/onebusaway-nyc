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
package org.onebusaway.nyc.vehicle_tracking.model;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.onebusaway.gtfs.csv.schema.BeanWrapper;
import org.onebusaway.gtfs.csv.schema.BeanWrapperFactory;
import org.onebusaway.gtfs.csv.schema.FieldMapping;

public class DateTimeFieldMappingFactoryTest {
  
  @Test
  public void test() throws ParseException {

    DateTimeFieldMappingFactory factory = new DateTimeFieldMappingFactory();
    FieldMapping mapping = factory.createFieldMapping(null,
        NycTestLocationRecord.class, "dt", "timestamp", Long.class, true);

    NycTestLocationRecord record = new NycTestLocationRecord();
    record.setTimestamp(1284377940000L);
    BeanWrapper obj = BeanWrapperFactory.wrap(record);
    Map<String, Object> csvValues = new HashMap<String, Object>();

    mapping.translateFromObjectToCSV(null, obj, csvValues);

    Object value = csvValues.get("dt");
    assertEquals("2010-09-13 07:39:00", value);
  }
}
