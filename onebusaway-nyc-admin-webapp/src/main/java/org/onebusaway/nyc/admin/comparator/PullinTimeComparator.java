package org.onebusaway.nyc.admin.comparator;

import java.util.Comparator;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.onebusaway.nyc.admin.model.ui.VehicleStatus;

/**
 * Compares vehicles on pullout time
 * @author abelsare
 *
 */
public class PullinTimeComparator implements Comparator<VehicleStatus>{
	
	private TimeComparator timeComparator;
	private DateTimeFormatter formatter = ISODateTimeFormat.dateTimeNoMillis();
	
	public PullinTimeComparator(String order) {
		timeComparator = new TimeComparator(order);
	}
	
	@Override
	public int compare(VehicleStatus o1, VehicleStatus o2) {
		if(StringUtils.isBlank(o1.getPullinTime())) {
			return 1;
		}
		if(StringUtils.isBlank(o2.getPullinTime())) {
			return -1;
		}
		
		DateTime time1 = formatter.parseDateTime(o1.getPullinTime());
		DateTime time2 = formatter.parseDateTime(o2.getPullinTime());
		
		return timeComparator.compare(time1, time2);
	}

}
