package org.onebusaway.api.web.mapping.converters;

import org.onebusaway.api.web.mapping.formatting.NycDateConverterWrapper;
import org.onebusaway.api.web.mapping.formatting.NycDateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToLongConverter implements Converter<String, Long> {

    @Autowired
    NycDateTimeFormatter _dateTimeFormatter;

    @Autowired
    NycDateConverterWrapper _dateFormatter;

    @Override
    public Long convert(String source) {
        try {
            return Long.parseLong(source);
        } catch (NumberFormatException e) {
            try{
                return _dateTimeFormatter.toLong(source,null);
            } catch (RuntimeException e2) {
                try{
                    return _dateFormatter.stringToLong(source);
                } catch (RuntimeException e3) {
                    return Long.valueOf(-1);
                }
            }
        }
    }
}
