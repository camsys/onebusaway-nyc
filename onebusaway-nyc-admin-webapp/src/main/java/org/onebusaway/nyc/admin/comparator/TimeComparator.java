package org.onebusaway.nyc.admin.comparator;

import java.util.Comparator;

import org.joda.time.DateTime;

/**
 * Compares vehicle records based on given times
 * @author abelsare
 *
 */
public class TimeComparator implements Comparator<DateTime>{

	private String order;
	
	public TimeComparator(String order) {
		this.order = order;
	}
	
	@Override
	public int compare(DateTime time1, DateTime time2) {
		if(order.equalsIgnoreCase("desc")) {
			return time2.compareTo(time1);
		}
		return time1.compareTo(time2);
	}

	

	

}
