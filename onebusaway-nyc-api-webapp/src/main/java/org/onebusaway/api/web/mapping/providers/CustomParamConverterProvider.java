//package org.onebusaway.api.web.mapping.providers;
//
//import org.joda.time.DateTime;
//import org.onebusaway.api.web.mapping.formatting.FieldErrorMessage;
//import org.onebusaway.api.web.mapping.formatting.NycDateFormatter;
//import org.onebusaway.api.web.mapping.formatting.NycDateTimeFormatter;
//import org.onebusaway.api.web.mapping.formatting.SupportsFieldErrorConverter;
//
//
//import javax.ws.rs.QueryParam;
//import javax.ws.rs.ext.ParamConverter;
//import javax.ws.rs.ext.ParamConverterProvider;
//import javax.ws.rs.ext.Provider;
//import java.lang.annotation.Annotation;
//import java.lang.reflect.Type;
//import java.util.*;
//
//@Provider
//public class CustomParamConverterProvider implements ParamConverterProvider {
//
//
//    Set<ParamConverterProvider> paramConverters;
//
//    public CustomParamConverterProvider(){
//        paramConverters = new HashSet<>();
//    }
//
//    @Override
//    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
//        ParamConverter<T> converter = null;
//
//        String field = null;
//        for (Annotation annotation : annotations) {
//            if (annotation.annotationType().equals(QueryParam.class)) {
//                QueryParam paramAnnotation = (QueryParam) annotation;
//                field = paramAnnotation.value();
//            }
//            if (annotation.annotationType().equals(FieldErrorMessage.class)) {
//                FieldErrorMessage paramAnnotation = (FieldErrorMessage) annotation;
//                field = paramAnnotation.value();
//                break;
//            }
//        }
//
//        if (rawType.equals(DateTime.class))
//            converter =  (ParamConverter<T>) new NycDateTimeFormatter(field);
//        if (rawType.equals(Date.class))
//            converter = (ParamConverter<T>) new NycDateFormatter(field);
//        if(rawType.equals(Integer.TYPE)) {
//            return (ParamConverter<T>) new IntegerConverter(field);
//        }
////        if(converter==null){
////            for (ParamConverterProvider provider : paramConverters) {
////                converter = provider.getConverter(rawType, genericType, annotations);
////                if (converter != null) {
////                    break;
////                }
////            }
////        }
////        if(converter==null)
////            return null;
////        for (Annotation annotation : annotations) {
////            if (annotation.annotationType().equals(OptionalParam.class)) {
////                return (ParamConverter<T>) new OptionalConverter(converter);
////            }
////        }
//
//        return converter;
//    }
//
//
//    class IntegerConverter extends SupportsFieldErrorConverter<Integer> {
//        public IntegerConverter(String field) {
//            super(field);
//        }
//
//        @Override
//        public Integer convertFromString(String value) {
//            if(value==null || value.equals("")){
//                return -1;
//            }
//            return Integer.valueOf(value);
//        }
//
//        @Override
//        public String toString(Integer value) {
//            return String.valueOf(value);
//        }
//    }
//}
