package org.onebusaway.api.providers;

import org.eclipse.emf.ecore.util.EcoreValidator;
import org.glassfish.hk2.utilities.reflection.ParameterizedTypeImpl;
import org.joda.time.DateTime;
import org.onebusaway.nyc.presentation.impl.conversion.NycDateConverter;
import org.onebusaway.nyc.presentation.impl.conversion.NycDateTimeConverter;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.Optional;

//@Provider
public class CustomParamConverterProvider implements ParamConverterProvider {


    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (rawType.equals(DateTime.class))
            return (ParamConverter<T>) new NycDateTimeConverter();
        if (rawType.equals(Date.class))
            return (ParamConverter<T>) new NycDateConverter();
        return null;
    }
}
