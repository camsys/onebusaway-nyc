package org.onebusaway.api.web.mapping.converters;

import com.opensymphony.xwork2.conversion.TypeConversionException;
import org.onebusaway.api.web.mapping.formatting.NycDateFormatter;
import org.onebusaway.api.web.mapping.formatting.NycDateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.text.ParseException;

@Component
public class StringToLongConverter implements Converter<String, Long> {

    @Autowired
    NycDateTimeFormatter _dateTimeFormatter;

    @Autowired
    NycDateFormatter _dateFormatter;

    @Override
    public Long convert(String source) {
        try {
            return Long.parseLong(source);
        } catch (NumberFormatException e) {
            try{
                return _dateTimeFormatter.toLong(source,null);
            } catch (TypeConversionException e2) {
                try{
                    return _dateFormatter.stringToLong(source);
                } catch (TypeConversionException e3) {
                    return Long.valueOf(-1);
                }
            }
        }
    }
}
