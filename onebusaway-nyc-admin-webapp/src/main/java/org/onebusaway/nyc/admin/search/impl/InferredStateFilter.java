package org.onebusaway.nyc.admin.search.impl;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.admin.model.ui.VehicleStatus;
import org.onebusaway.nyc.admin.search.Filter;

/**
 * Filters vehicles by given inferred state
 * @author abelsare
 *
 */
public class InferredStateFilter implements Filter<VehicleStatus> {
	
	private String inferredState;
	
	public InferredStateFilter(String inferredState) {
		this.inferredState = inferredState;
	}

	@Override
	public boolean apply(VehicleStatus type) {
		if(StringUtils.isNotBlank(type.getInferredState())) {
			return type.getInferredState().equalsIgnoreCase(inferredState);
		}
		return false;
	}

}
