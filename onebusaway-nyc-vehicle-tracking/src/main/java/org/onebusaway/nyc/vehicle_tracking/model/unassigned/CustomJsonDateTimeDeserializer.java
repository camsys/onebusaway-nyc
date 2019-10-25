package org.onebusaway.nyc.vehicle_tracking.model.unassigned;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CustomJsonDateTimeDeserializer extends JsonDeserializer<Long>
{

    @Override
    public Long deserialize(JsonParser jsonParser,
                            DeserializationContext deserializationContext) throws IOException, JsonProcessingException {

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX'Z'");
        String date = jsonParser.getText();
        try {
            return format.parse(date).getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

    }

}
