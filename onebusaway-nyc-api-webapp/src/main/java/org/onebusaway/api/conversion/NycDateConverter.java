package org.onebusaway.api.conversion;

import org.onebusaway.presentation.impl.conversion.DateConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class NycDateConverter extends  SupportsFieldErrorConverter<Date> {
    private static Logger _log = LoggerFactory.getLogger(NycDateTimeConverter.class);

    DateConverter converter = new DateConverter();

    public NycDateConverter(String field) {
        super(field);
    }

    @Override
    public Date convertFromString(String value) {
        String[] strings = new String[1];
        strings[0] = value;
        return (Date) converter.convertFromString(null,strings,Date.class);
    }

    @Override
    public String toString(Date value) {
        return converter.convertToString(null,value);
    }


}
