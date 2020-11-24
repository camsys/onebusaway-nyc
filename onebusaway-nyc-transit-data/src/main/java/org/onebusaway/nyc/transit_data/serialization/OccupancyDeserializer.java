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

package org.onebusaway.nyc.transit_data.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class OccupancyDeserializer  extends JsonDeserializer<Integer> {
  @Override
  public Integer deserialize(JsonParser parser, DeserializationContext ctxt)
          throws IOException, JsonParseException {
      
      String integerStr = parser.getText();
      
      // Try to guess bad values before attempting to catch exception
      if (integerStr == null || integerStr.equals("UNKNOWN") || integerStr.isEmpty() || integerStr.equals("NaN")) {
          return null;
      }
      
      try{
        return Integer.parseInt(integerStr);   
      }
      catch(NumberFormatException nfe){
        return null;
      }
  }

}

