package org.onebusaway.nyc.presentation.impl.conversion;

import org.joda.time.DateTime;
import org.onebusaway.presentation.impl.conversion.DateTimeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ext.ParamConverter;
import java.util.Date;

public class NycDateTimeConverter extends DateTimeConverter  implements ParamConverter<DateTime> {
    private static Logger _log = LoggerFactory.getLogger(NycDateTimeConverter.class);

    public DateTime convertDateFromString(String value){
        try {
            return (DateTime) convertFromString(value, DateTime.class);
        }
        catch (ClassCastException exception){
            _log.error("Recieved unexpected exception, please confirm DateTimeConverter still returns Date.class, input value = " +value,exception);
            return null;
        }
    }

    public Long convertLongFromString(String value){
        try {
            return (Long) convertFromString(value, Date.class);
        }
        catch (ClassCastException exception){
            _log.error("Recieved unexpected exception, please confirm DateTimeConverter still returns Long.class, input value = " +value,exception);
            return null;
        }
    }

    public Object convertFromString(String value, Class toClass) {
        String[] strings = new String[1];
        strings[0] = value;
        return super.convertFromString(null,strings,toClass);
    }


    public String convertToString(Object o) {
        return super.convertToString(null,o);
    }

    @Override
    public DateTime fromString(String value) {
        return convertDateFromString(value);
    }

    @Override
    public String toString(DateTime value) {
        return convertToString(value);
    }

}
