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

