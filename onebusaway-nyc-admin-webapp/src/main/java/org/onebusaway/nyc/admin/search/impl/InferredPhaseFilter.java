package org.onebusaway.nyc.admin.search.impl;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.admin.model.ui.VehicleStatus;
import org.onebusaway.nyc.admin.search.Filter;

/**
 * Filters vehicles by given inferred phase
 * @author abelsare
 *
 */
public class InferredPhaseFilter implements Filter<VehicleStatus> {
	
	private String inferredPhase;
	
	public InferredPhaseFilter(String inferredPhase) {
		this.inferredPhase = inferredPhase;
	}

	@Override
	public boolean apply(VehicleStatus type) {
		if(StringUtils.isNotBlank(type.getInferredPhase())) {
			return type.getInferredPhase().equalsIgnoreCase(inferredPhase);
		}
		return false;
	}

}
