package org.onebusaway.api.serializers.json;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.fasterxml.jackson.databind.ser.SerializerFactory;

import java.util.Collection;

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
        if (property.getType().getRawClass().equals(Collection.class))
            return Serializers.NULL_COLLECTION_SERIALIZER_INSTANCE;
        else
            return super.findNullValueSerializer(property);
    }
}
