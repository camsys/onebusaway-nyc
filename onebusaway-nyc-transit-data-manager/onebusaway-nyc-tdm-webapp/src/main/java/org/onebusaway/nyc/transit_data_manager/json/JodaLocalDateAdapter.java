package org.onebusaway.nyc.transit_data_manager.json;

import java.lang.reflect.Type;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.onebusaway.nyc.transit_data_manager.util.OneBusAwayDateFormats;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class JodaLocalDateAdapter implements JsonSerializer<LocalDate>,
    JsonDeserializer<LocalDate> {

  @Override
  public LocalDate deserialize(JsonElement json, Type typeOfT,
      JsonDeserializationContext context) throws JsonParseException {
    
    DateTimeFormatter fmt = DateTimeFormat.forPattern(OneBusAwayDateFormats.DATETIMEPATTERN_DATE);
    LocalDate result = fmt.parseLocalDate(json.getAsJsonPrimitive().getAsString());
    
    return result;
  }

  @Override
  public JsonElement serialize(LocalDate src, Type typeOfSrc,
      JsonSerializationContext context) {
    DateTimeFormatter fmt = DateTimeFormat.forPattern(OneBusAwayDateFormats.DATETIMEPATTERN_DATE);
    return new JsonPrimitive(fmt.print(src));
  }

}
