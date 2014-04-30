package org.onebusaway.nyc.admin.comparator;

import java.util.Comparator;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.admin.model.ui.VehicleStatus;

/**
 * Compares vehicles by their observed DSC nulls last
 * @author abelsare
 *
 */
public class ObservedDSCComparator implements Comparator<VehicleStatus>{

	private String order;
	
	public ObservedDSCComparator(String order) {
		this.order = order;
	}
	
	@Override
	public int compare(VehicleStatus o1, VehicleStatus o2) {
		if(StringUtils.isBlank(o1.getObservedDSC())) {
			return 1;
		}
		if(StringUtils.isBlank(o2.getObservedDSC())) {
			return -1;
		}
		if(order.equalsIgnoreCase("desc")) {
			return new Integer(o2.getObservedDSC()).compareTo(new Integer(o1.getObservedDSC()));
		}
		return new Integer(o1.getObservedDSC()).compareTo(new Integer(o2.getObservedDSC()));
	}

}
