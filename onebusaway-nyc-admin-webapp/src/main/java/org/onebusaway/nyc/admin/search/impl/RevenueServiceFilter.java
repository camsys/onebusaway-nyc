package org.onebusaway.nyc.admin.search.impl;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.admin.model.ui.VehicleStatus;
import org.onebusaway.nyc.admin.search.Filter;

/**
 * Filters vehicles that are inferred in revenue service i.e whose inferred state is either
 * IN PROGRESS or LAYOVER_*
 * @author abelsare
 *
 */
public class RevenueServiceFilter implements Filter<VehicleStatus> {

	@Override
	public boolean apply(VehicleStatus type) {
		if(StringUtils.isNotBlank(type.getInferredState())) {
			if(type.getInferredState().equalsIgnoreCase("IN PROGRESS") ||
					type.getInferredState().startsWith("LAY")) {
				return true;
			}
		}
		return false;
	}

}
