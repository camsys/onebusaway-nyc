package org.onebusaway.nyc.transit_data_manager.config;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class TdmIsoDateTimeNoMillisSerializer extends JsonSerializer<DateTime> {

  private static DateTimeFormatter formatter = ISODateTimeFormat.dateTimeNoMillis();
  
  @Override
  public void serialize(DateTime dateTime, JsonGenerator jsonGen,
      SerializerProvider serProv) throws IOException, JsonProcessingException {
    
    jsonGen.writeString(formatter.print(dateTime));
    
    
  }

}
