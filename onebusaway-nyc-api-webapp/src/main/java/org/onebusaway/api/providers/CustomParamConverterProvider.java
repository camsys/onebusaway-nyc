package org.onebusaway.api.providers;

import org.joda.time.DateTime;
import org.onebusaway.nyc.presentation.impl.conversion.NycDateConverter;
import org.onebusaway.nyc.presentation.impl.conversion.NycDateTimeConverter;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;

@Provider
public class CustomParamConverterProvider implements ParamConverterProvider {


    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        ParamConverter<T> converter = null;
        if (rawType.equals(DateTime.class))
            converter =  (ParamConverter<T>) new NycDateTimeConverter();
        if (rawType.equals(Date.class))
            converter = (ParamConverter<T>) new NycDateConverter();
        if(rawType.equals(Integer.TYPE)) {
            return (ParamConverter<T>) new IntegerConverter();
        }

        return converter;
    }


    class IntegerConverter implements ParamConverter<Integer> {
        @Override
        public Integer fromString(String value) {
            if(value==null || value.equals("")){
                return -1;
            }
            return Integer.valueOf(value);
        }

        @Override
        public String toString(Integer value) {
            return String.valueOf(value);
        }
    }
}
