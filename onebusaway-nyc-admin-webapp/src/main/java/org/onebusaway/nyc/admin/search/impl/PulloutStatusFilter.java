package org.onebusaway.nyc.admin.search.impl;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.onebusaway.nyc.admin.model.ui.VehicleStatus;
import org.onebusaway.nyc.admin.search.Filter;

/**
 * Filters vehicles with active pullout status (pullout time <= now and pullin time >=now)
 * @author abelsare
 *
 */
public class PulloutStatusFilter implements Filter<VehicleStatus>{

	@Override
	public boolean apply(VehicleStatus type) {
		if(StringUtils.isNotBlank(type.getPulloutTime()) && StringUtils.isNotBlank(type.getPullinTime())) {
			DateTimeFormatter format = ISODateTimeFormat.dateTimeNoMillis();
			DateTime pulloutTime = format.parseDateTime(type.getPulloutTime());
			DateTime pullinTime = format.parseDateTime(type.getPullinTime());
			DateTime now = new DateTime();
			boolean isActivePullout = pulloutTime.isBeforeNow() || pulloutTime.equals(now);
			boolean isActivePullin = pullinTime.isAfterNow() || pullinTime.equals(now);
			return isActivePullout && isActivePullin;
		}
		return false;
	}

}
