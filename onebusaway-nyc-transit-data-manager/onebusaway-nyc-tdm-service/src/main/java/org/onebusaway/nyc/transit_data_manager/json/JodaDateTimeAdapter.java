package org.onebusaway.nyc.transit_data_manager.json;

import java.lang.reflect.Type;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.onebusaway.nyc.transit_data_manager.util.OneBusAwayDateFormats;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class JodaDateTimeAdapter implements JsonSerializer<DateTime>,
JsonDeserializer<DateTime>{

  @Override
  public DateTime deserialize(JsonElement json, Type typeOfT,
      JsonDeserializationContext context) throws JsonParseException {
    
    DateTimeFormatter fmt = OneBusAwayDateFormats.DATETIMEPATTERN_JSON_DATE_TIME;
    DateTime result = fmt.parseDateTime(json.getAsJsonPrimitive().getAsString());
    
    return result;
  }

  @Override
  public JsonElement serialize(DateTime src, Type typeOfSrc,
      JsonSerializationContext context) {
    DateTimeFormatter fmt = OneBusAwayDateFormats.DATETIMEPATTERN_JSON_DATE_TIME;
    return new JsonPrimitive(fmt.print(src));
  }

}
