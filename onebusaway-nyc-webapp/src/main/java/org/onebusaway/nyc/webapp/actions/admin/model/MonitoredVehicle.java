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

public interface MonitoredVehicle {

	public String getVehicleIdWithoutAgency();
	
	public String getVehicleId();
	
	public String getStatusClass();
		
	public String getActualPullOut();
	
	public long getActualPullOutEpoch();
	
	public String getScheduledPullIn();
	
	public long getScheduledPullInEpoch();
	
	public String getLastUpdateTime();
	
	public long getLastUpdateTimeEpoch();
	
	public String getLastCommTime();
	
	public long getLastCommTimeEpoch();
	
	public String getHeadsign();
	
	public String getInferredState();
	
	public String getLocation();
	
	public String getOrientation();
	
	public boolean isDisabled();
}