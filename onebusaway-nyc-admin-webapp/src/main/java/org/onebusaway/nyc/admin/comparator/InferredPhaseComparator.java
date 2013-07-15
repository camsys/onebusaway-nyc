package org.onebusaway.nyc.admin.comparator;

import java.util.Comparator;

import org.apache.commons.lang.xwork.StringUtils;
import org.onebusaway.nyc.admin.model.ui.VehicleStatus;

/**
 * Compares vehicles by their inferred phase nulls last
 * @author abelsare
 *
 */
public class InferredPhaseComparator implements Comparator<VehicleStatus>{
	
	private String order;
	
	public InferredPhaseComparator(String order) {
		this.order = order;
	}

	@Override
	public int compare(VehicleStatus o1, VehicleStatus o2) {
		if(StringUtils.isBlank(o1.getInferredPhase())) {
			return 1;
		}
		if(StringUtils.isBlank(o2.getInferredPhase())) {
			return -1;
		}
		if(order.equalsIgnoreCase("desc")) {
			return o2.getInferredPhase().compareToIgnoreCase(o1.getInferredPhase());
		}
		return o1.getInferredPhase().compareToIgnoreCase(o2.getInferredPhase());
	}

}
