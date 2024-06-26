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

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.fasterxml.jackson.databind.ser.SerializerFactory;

import java.util.Collection;
import java.util.List;

public class CustomSerializerProvider extends DefaultSerializerProvider {

    public CustomSerializerProvider() {
        super();
    }

    public CustomSerializerProvider(CustomSerializerProvider provider, SerializationConfig config,
                                    SerializerFactory jsf) {
        super(provider, config, jsf);
    }

    @Override
    public CustomSerializerProvider createInstance(SerializationConfig config, SerializerFactory jsf) {
        return new CustomSerializerProvider(this, config, jsf);
    }

    @Override
    public JsonSerializer<Object> findNullValueSerializer(BeanProperty property) throws JsonMappingException {
        if (property.getType().getRawClass().equals(String.class))
            return Serializers.EMPTY_STRING_SERIALIZER_INSTANCE;
        if (property.getType().getRawClass().equals(Long.class))
            return Serializers.NULL_LONG_SERIALIZER_INSTANCE;
        if (property.getType().getRawClass().equals(Collection.class) || property.getType().getRawClass().equals(List.class))
            return Serializers.NULL_COLLECTION_SERIALIZER_INSTANCE;
        else
            return super.findNullValueSerializer(property);
    }
}
