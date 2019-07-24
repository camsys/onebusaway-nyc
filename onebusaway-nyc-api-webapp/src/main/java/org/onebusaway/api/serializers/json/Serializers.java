package org.onebusaway.api.serializers.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class Serializers extends JsonSerializer<Object> {
    public static final JsonSerializer<Object> EMPTY_STRING_SERIALIZER_INSTANCE = new EmptyStringSerializer();
    public static final JsonSerializer<Object> NULL_LONG_SERIALIZER_INSTANCE = new NullLongSerializer();
    public static final JsonSerializer<Object> NULL_COLLECTION_SERIALIZER_INSTANCE = new NullCollectionSerializer();


    public Serializers() {}

    @Override
    public void serialize(Object o, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException, JsonProcessingException {
        jsonGenerator.writeString("");
    }

    private static class EmptyStringSerializer extends JsonSerializer<Object> {
        public EmptyStringSerializer() {}

        @Override
        public void serialize(Object o, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
                throws IOException, JsonProcessingException {
            jsonGenerator.writeString("");
        }
    }

    private static class NullLongSerializer extends JsonSerializer<Object> {
        public NullLongSerializer() {}

        @Override
        public void serialize(Object o, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
                throws IOException, JsonProcessingException {
            jsonGenerator.writeNumber(0);
        }
    }

    private static class NullCollectionSerializer extends JsonSerializer<Object> {
        public NullCollectionSerializer() {}

        @Override
        public void serialize(Object o, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
                throws IOException, JsonProcessingException {
            jsonGenerator.writeStartArray(0);
            jsonGenerator.writeEndArray();
        }
    }
}
