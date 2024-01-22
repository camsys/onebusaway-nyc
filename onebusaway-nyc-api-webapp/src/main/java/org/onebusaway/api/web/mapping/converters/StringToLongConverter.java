package org.onebusaway.api.web.mapping.converters;

import org.onebusaway.api.web.mapping.formatting.NycDateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.text.ParseException;

@Component
public class StringToLongConverter implements Converter<String, Long> {

    @Autowired
    NycDateTimeFormatter _formatter;

    @Override
    public Long convert(String source) {
        try {
            return Long.parseLong(source);
        } catch (NumberFormatException e) {
            try{
                return _formatter.toLong(source,null);
            } catch (ParseException parseException) {
                return Long.valueOf(-1);
            }
        }
    }
}
