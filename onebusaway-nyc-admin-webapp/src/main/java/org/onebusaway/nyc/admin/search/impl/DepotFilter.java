package org.onebusaway.nyc.admin.search.impl;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.admin.model.ui.VehicleStatus;
import org.onebusaway.nyc.admin.search.Filter;

/**
 * Filters vehicles by given depot
 * @author abelsare
 *
 */
public class DepotFilter implements Filter<VehicleStatus>{

	private String depotId;
	
	public DepotFilter(String depotId) {
		this.depotId = depotId;
	}

	@Override
	public boolean apply(VehicleStatus type) {
		if(StringUtils.isNotBlank(type.getDepot())) {
			return type.getDepot().equalsIgnoreCase(depotId);
		}
		return false;
	}
}
