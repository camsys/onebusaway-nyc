package org.onebusaway.nyc.admin.search.impl;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.admin.model.ui.VehicleStatus;
import org.onebusaway.nyc.admin.search.Filter;

/**
 * Filters vehicles by emergency status
 * @author abelsare
 *
 */
public class EmergencyStatusFilter implements Filter<VehicleStatus>{
	
	
	@Override
	public boolean apply(VehicleStatus type) {
		return StringUtils.isNotBlank(type.getEmergencyStatus());
	}
}
