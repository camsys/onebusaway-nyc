package org.onebusaway.nyc.transit_data_manager.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides utility methods for dates
 * @author abelsare
 *
 */
public class DateUtility {

	private static final Logger log = LoggerFactory.getLogger(DateUtility.class);

	/**
	 * Returns today's date in 'yyyy-MM-dd' format
	 * @return today's date in 'yyyy-MM-dd' format
	 */
	public static Date getTodaysDate() {
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		String dateString = formatter.format(new Date());
		Date formattedDate = null;

		try {
			formattedDate = formatter.parse(dateString);
		} catch (ParseException e) {
			log.error("Error parsing today's date");
			e.printStackTrace();
		}

		return formattedDate;
	}
	
	public static Date getTodaysDateTime() {
		DateTimeFormatter formatter = ISODateTimeFormat.dateTimeNoMillis();
		DateTime dateTime = new DateTime();
		
		String dateTimeString = dateTime.toString(formatter);
		Date now = formatter.parseDateTime(dateTimeString).toDate();
		
		return now;
	}

}
