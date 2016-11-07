package org.onebusaway.nyc.transit_data_manager.config;

import java.io.IOException;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class TdmIsoDateTimeNoMillisSerializer extends JsonSerializer<DateTime> {

  private static DateTimeFormatter formatter = ISODateTimeFormat.dateTimeNoMillis();
  
  @Override
  public void serialize(DateTime dateTime, JsonGenerator jsonGen,
      SerializerProvider serProv) throws IOException, JsonProcessingException {
    
    jsonGen.writeString(formatter.print(dateTime));
    
    
  }

}
