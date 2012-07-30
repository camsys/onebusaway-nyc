package org.onebusaway.nyc.admin.search.impl;

import java.math.BigDecimal;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.onebusaway.nyc.admin.model.ui.VehicleStatus;
import org.onebusaway.nyc.admin.search.Filter;

/**
 * Filters vehicles whose last received time falls within the given time window (in seconds)
 * @author abelsare
 *
 */
public class TimeWindowFilter implements Filter<VehicleStatus>{

	private int timeWindow;
	
	public TimeWindowFilter(int timeWindow) {
		this.timeWindow = timeWindow;
	}
	
	@Override
	public boolean apply(VehicleStatus type) {
		if(StringUtils.isNotBlank(type.getTimeReceived())) {
			BigDecimal timeDifference = getTimeDifference(type.getTimeReceived());
			return timeDifference.compareTo(new BigDecimal(timeWindow * 60)) <= 0;
		}
		return false;
	}
	
	private BigDecimal getTimeDifference(String timeReceived) {
		DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
		DateTime lastReportedTime = formatter.parseDateTime(timeReceived);
		DateTime now = new DateTime();
		int seconds = Seconds.secondsBetween(lastReportedTime, now).getSeconds();
		BigDecimal difference = new BigDecimal(seconds);
		return difference;
	}

}
