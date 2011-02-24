/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.webapp.actions.admin.model;

import java.text.DateFormat;

import org.onebusaway.nyc.transit_data.model.UtsRecordBean;

public class UtsVehicleBean implements MonitoredVehicle {
	
	private UtsRecordBean utsRecord;

	public UtsVehicleBean(UtsRecordBean utsRecord) {
		this.utsRecord = utsRecord;
	}

	public String getVehicleIdWithoutAgency() {
		return utsRecord.getBusNumber();
	}

	public String getVehicleId() {
		return utsRecord.getBusNumber();
	}

	public String getStatusClass() {
		return "status red";
	}

	public String getActualPullOut() {
		if(utsRecord == null) {
			return "Unknown";
		} else {
			return DateFormat.getDateInstance().format(utsRecord.getActualPullOut()) + "<br/>" + 
			DateFormat.getTimeInstance().format(utsRecord.getActualPullOut());
		}
	}

	// key for sorting on client-side
	public long getActualPullOutEpoch() {
		if(utsRecord == null) {
			return -1;
		} else {
			return utsRecord.getActualPullOut().getTime();
		}
	}

	public String getScheduledPullIn() {
		if(utsRecord == null) {
			return "Unknown";
		} else {
			return DateFormat.getDateInstance().format(utsRecord.getScheduledPullIn()) + "<br/>" + 
			DateFormat.getTimeInstance().format(utsRecord.getScheduledPullIn());
		}
	}

	// key for sorting on client-side
	public long getScheduledPullInEpoch() {
		return -1;
	}

	public String getLastUpdateTime() {
		return "Not Available";
	}

	// key for sorting on client-side
	public long getLastUpdateTimeEpoch() {
		return -1;
	}

	public String getLastCommTime() {
		return "Not Available";
	}

	// key for sorting on client-side
	public long getLastCommTimeEpoch() {
		return -1;
	}

	public String getHeadsign() {
		return "(" + utsRecord.getRoute() + " in unknown direction)";
	}

	public String getInferredState() {
		return "Not Available";
	}

	public String getLocation() {
		return "Not Available";
	}

	public String getOrientation() {
		return "Not Available";
	}    

	public boolean isDisabled() {
		return false;
	}
}