/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onebusaway.nyc.admin.comparator;

import java.util.Comparator;

import org.apache.commons.lang.StringUtils;
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
