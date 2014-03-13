package org.onebusaway.nyc.transit_data_manager.config;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

public class NullSerializer extends JsonSerializer<Object> {

  private String serializeNullAsStr = "";
  
  @Override
  public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider)
      throws IOException, JsonProcessingException {
    
    jgen.writeString(serializeNullAsStr);
    
  }

}
