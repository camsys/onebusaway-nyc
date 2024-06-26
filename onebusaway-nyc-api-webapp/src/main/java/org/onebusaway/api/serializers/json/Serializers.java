/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
