package org.onebusaway.nyc.admin.comparator;

import java.util.Comparator;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.onebusaway.nyc.admin.model.ui.VehicleStatus;

/**
 * Compares vehicle records on last reported time
 * @author abelsare
 *
 */
public class LastUpdateComparator implements Comparator<VehicleStatus>{

	private TimeComparator timeComparator;
	private DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
			
	public LastUpdateComparator(String order) {
		timeComparator = new TimeComparator(order);
	}

	@Override
	public int compare(VehicleStatus o1, VehicleStatus o2) {
		if(StringUtils.isBlank(o1.getTimeReported())) {
			return 1;
		}
		if(StringUtils.isBlank(o2.getTimeReported())) {
			return -1;
		}
		
		DateTime time1 = formatter.parseDateTime(o1.getTimeReported());
		DateTime time2 = formatter.parseDateTime(o2.getTimeReported());
		return timeComparator.compare(time2, time1);
	}
}
