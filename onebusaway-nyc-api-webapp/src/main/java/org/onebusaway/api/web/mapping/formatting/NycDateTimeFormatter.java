package org.onebusaway.api.web.mapping.formatting;

import com.opensymphony.xwork2.conversion.TypeConversionException;
import org.joda.time.DateTime;
import org.onebusaway.presentation.impl.conversion.DateTimeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.Formatter;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

//todo: remove this class after updating DateTimeConverter in base OBA
@Component
public class NycDateTimeFormatter implements Formatter<DateTime> {
    private static Logger _log = LoggerFactory.getLogger(NycDateTimeFormatter.class);

    SimpleDateFormat _format = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    DateTimeConverter converter = new DateTimeConverter();


    public DateTime convertFromString(String value) {
        if(value==null){
            return null;
        }

        Date date;
        try{
        if (value.matches("^(\\d+)$")) {
            long v = Long.parseLong(value);
            date = new Date(v);
        }
        date = _format.parse(value);
        } catch (ParseException e) {
            e.printStackTrace();
            throw new TypeConversionException(e);
        }
    if(date==null){
        return null;
    }
    return new DateTime(date);
    }

    public String toString(DateTime value) {
        return _format.format(value);
    }

    @Override
    public DateTime parse(String text, Locale locale) throws ParseException {
        return convertFromString(text);
    }

    @Override
    public String print(DateTime object, Locale locale) {
        return toString(object);
    }


    public long toLong(String text, Locale locale) throws ParseException{
        return parse(text,locale).getMillis();
    }


}
