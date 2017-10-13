package org.onebusaway.nyc.transit_data.serialization;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

import java.io.IOException;

public class OccupancyDeserializer  extends JsonDeserializer<Integer> {
  @Override
  public Integer deserialize(JsonParser parser, DeserializationContext ctxt)
          throws IOException, JsonProcessingException {
      
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

