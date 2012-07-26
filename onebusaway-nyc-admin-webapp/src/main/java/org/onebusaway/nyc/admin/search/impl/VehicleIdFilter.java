package org.onebusaway.nyc.admin.search.impl;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.admin.model.ui.VehicleStatus;
import org.onebusaway.nyc.admin.search.Filter;

/**
 * Filters vehicles by given id
 * @author abelsare
 *
 */
public class VehicleIdFilter implements Filter<VehicleStatus> {

	private String vehicleId;
	
	public VehicleIdFilter(String vehicleId) {
		this.vehicleId = vehicleId;
	}
	
	@Override
	public boolean apply(VehicleStatus type) {
		if(StringUtils.isNotBlank(type.getVehicleId())) {
			return type.getVehicleId().equalsIgnoreCase(vehicleId);
		}
		return false;
	}

}
