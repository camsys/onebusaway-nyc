package org.onebusaway.nyc.transit_data_federation.impl.nyc;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import java.io.IOException;
import org.onebusaway.gtfs.model.AgencyAndId;

public class AgencyAndIdDeserializer extends KeyDeserializer {
    @Override
    public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
        return AgencyAndId.convertFromString(key);
    }
}

