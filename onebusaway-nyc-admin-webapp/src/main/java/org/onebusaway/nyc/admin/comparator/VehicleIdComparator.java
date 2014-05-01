package org.onebusaway.nyc.admin.comparator;

import java.util.Comparator;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.admin.model.ui.VehicleStatus;

/**
 * Compares vehicle records by vehicle ids nulls last
 * @author abelsare
 *
 */
public class VehicleIdComparator implements Comparator<VehicleStatus>{

	private String order;
	
	public VehicleIdComparator(String order) {
		this.order = order;
	}
	
	@Override
	public int compare(VehicleStatus o1, VehicleStatus o2) {
		if(StringUtils.isBlank(o1.getVehicleId())) {
			return 1;
		}
		if(StringUtils.isBlank(o2.getVehicleId())) {
			return -1;
		}
		if(order.equalsIgnoreCase("desc")) {
			return new Integer(o2.getVehicleId()).compareTo(new Integer(o1.getVehicleId()));
		}
		return new Integer(o1.getVehicleId()).compareTo(new Integer(o2.getVehicleId()));
	}

}
