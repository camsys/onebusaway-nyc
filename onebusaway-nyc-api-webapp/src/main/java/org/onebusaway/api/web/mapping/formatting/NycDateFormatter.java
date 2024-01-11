package org.onebusaway.api.web.mapping.formatting;

import org.onebusaway.presentation.impl.conversion.DateConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.Formatter;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

@Component
public class NycDateFormatter implements Formatter<Date> {
    private static Logger _log = LoggerFactory.getLogger(NycDateTimeFormatter.class);

    DateConverter converter = new DateConverter();

    public Date convertFromString(String value) {
        String[] strings = new String[1];
        strings[0] = value;
        return (Date) converter.convertFromString(null,strings,Date.class);
    }

    public String toString(Date value) {
        return converter.convertToString(null,value);
    }


    @Override
    public Date parse(String text, Locale locale) throws ParseException {
        return convertFromString(text);
    }

    @Override
    public String print(Date object, Locale locale) {
        return toString(object);
    }
}
