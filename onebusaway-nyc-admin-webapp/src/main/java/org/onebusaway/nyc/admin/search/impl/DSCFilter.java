package org.onebusaway.nyc.admin.search.impl;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.admin.model.ui.VehicleStatus;
import org.onebusaway.nyc.admin.search.Filter;

/**
 * Filters vehicles by given observed DSC
 * @author abelsare
 *
 */
public class DSCFilter implements Filter<VehicleStatus> {

	private String dsc;
	
	
	public DSCFilter(String dsc) {
		this.dsc = dsc;
	}


	@Override
	public boolean apply(VehicleStatus type) {
		if(StringUtils.isNotBlank(type.getObservedDSC())) {
			return type.getObservedDSC().startsWith(dsc);
		}
		return false;
	}

}
