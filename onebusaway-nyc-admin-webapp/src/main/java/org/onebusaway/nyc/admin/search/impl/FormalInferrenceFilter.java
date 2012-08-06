package org.onebusaway.nyc.admin.search.impl;

import org.onebusaway.nyc.admin.model.ui.VehicleStatus;
import org.onebusaway.nyc.admin.search.Filter;

/**
 * Filters vehicles that have formal inferrence
 * @author abelsare
 *
 */
public class FormalInferrenceFilter implements Filter<VehicleStatus>{

	@Override
	public boolean apply(VehicleStatus type) {
		return type.isInferrenceFormal();
	}

}
