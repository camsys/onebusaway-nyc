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

package org.onebusaway.nyc.admin.model;

import java.util.Map;

/**
 * Response object with data for admin parameters UI
 * @author abelsare
 *
 */
public class ParametersResponse {

	private Map<String, String> configParameters;
	private boolean saveSuccess;

	/**
	 * @return the configParameters
	 */
	public Map<String, String> getConfigParameters() {
		return configParameters;
	}

	/**
	 * @param configParameters the configParameters to set
	 */
	public void setConfigParameters(Map<String, String> configParameters) {
		this.configParameters = configParameters;
	}

	/**
	 * Returns true if all parameters are saved successfully
	 * @return the saveSuccess
	 */
	public boolean isSaveSuccess() {
		return saveSuccess;
	}

	/**
	 * Set to true if all parameters are saved successfully
	 * @param saveSuccess the saveSuccess to set
	 */
	public void setSaveSuccess(boolean saveSuccess) {
		this.saveSuccess = saveSuccess;
	}
	
	
}
