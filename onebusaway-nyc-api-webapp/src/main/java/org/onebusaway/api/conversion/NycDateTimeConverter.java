package org.onebusaway.api.conversion;

import com.opensymphony.xwork2.conversion.TypeConversionException;
import org.joda.time.DateTime;
import org.onebusaway.presentation.impl.conversion.DateTimeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

//todo: remove this class after updating DateTimeConverter in base OBA
public class NycDateTimeConverter extends  SupportsFieldErrorConverter<DateTime> {
    private static Logger _log = LoggerFactory.getLogger(NycDateTimeConverter.class);

    SimpleDateFormat _format = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    DateTimeConverter converter = new DateTimeConverter();

    public NycDateTimeConverter(String field) {
        super(field);
    }

    @Override
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

    @Override
    public String toString(DateTime value) {
        return _format.format(value);
    }

}
