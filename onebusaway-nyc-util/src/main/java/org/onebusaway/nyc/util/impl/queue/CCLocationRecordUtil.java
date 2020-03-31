package org.onebusaway.nyc.util.impl.queue;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;

public class CCLocationRecordUtil {

    // Instantaneous speed. Per SAE J1587 speed is in half mph increments with
    // an offset of -15mph.
    // For example, 002.642 knots (from RMC) is 3.04 mph, so (15*2) + (3 * 2) =
    // 36
    // Valid range is [0,255], which equates to [-15mph, +112.5mph]

    /*
     * From the TCIP documentation:
     *
     * In accordance with J-1587, this data element is an unsigned byte whose
     * value is expressed in half mile per hour increments with an offset to allow
     * backing up to be expressed. A value of 0 indicates a speed of -15mph, a
     * value of 1 indicates -14.5mph and so on. Values in excess of 112.5 mph
     * cannot be expressed.
     */

    /*
     * Basically this means the following, where 'speed' (little 's') is the J1587
     * value and 'Speed' (big 's') is the actual speed value (in mph):
     *
     * Speed = (speed - 30) / 2
     *
     * speed = (Speed * 2) + 30
     *
     * For this method, we want the first of those equations.
     * This method is also implemented in RecordValidationServiceImpl.
     */
    public static BigDecimal convertSpeed(short saeSpeed) {
        BigDecimal noOffsetSaeSpeed = new BigDecimal(saeSpeed - 30);

        return noOffsetSaeSpeed.divide(new BigDecimal(2));
    }

    public static BigDecimal convertMicrodegreesToDegrees(int latlong) {
        return new BigDecimal(latlong * Math.pow(10.0, -6));
    }

    /**
     * Timestamp of the on-board device when this message is created, in standard
     * XML timestamp format "time-reported": "2011-06-22T10:58:10.0-00:00" Package
     * private for unit tests.
     */
    public static Date convertTime(String timeString, String zoneOffset) {
        if (timeString == null) {
            return null;
        }
        // some times the date doesn't include UTC
        // 2011-08-06T10:40:38.825, we have to assume these are in
        // local, i.e. EST time and convert appropriately
        DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
        formatter.withZone(DateTimeZone.UTC);

        if (timeString.length() > 1 && timeString.length() < 20) {
            timeString = timeString + ".000";
        }

        if (timeString.length() > 20 && timeString.length() < 24) {
            // append correct offset
            timeString = timeString + zoneOffset;
        }
        return new Date(formatter.parseDateTime(timeString).getMillis());
    }
}
