package org.onebusaway.nyc.admin.comparator;

import java.util.Comparator;

import org.apache.commons.lang.xwork.StringUtils;
import org.onebusaway.nyc.admin.model.ui.VehicleStatus;

/**
 * Compares vehicles by their inferred state nulls last
 * @author abelsare
 *
 */
public class InferredStateComparator implements Comparator<VehicleStatus>{
	
	private String order;
	
	public InferredStateComparator(String order) {
		this.order = order;
	}

	@Override
	public int compare(VehicleStatus o1, VehicleStatus o2) {
		if(StringUtils.isBlank(o1.getInferredState())) {
			return 1;
		}
		if(StringUtils.isBlank(o2.getInferredState())) {
			return -1;
		}
		if(order.equalsIgnoreCase("desc")) {
			return o2.getInferredState().compareToIgnoreCase(o1.getInferredState());
		}
		return o1.getInferredState().compareToIgnoreCase(o2.getInferredState());
	}

}
