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

package org.onebusaway.api.actions.api;

import java.util.List;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.rest.DefaultHttpHeaders;
import org.onebusaway.transit_data.model.AgencyWithCoverageBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.JsonObject;
import com.opensymphony.xwork2.ActionSupport;

@ParentPackage("struts-default")
public class PingAction extends ActionSupport {

	private static final long serialVersionUID = 1L;

	private static Logger _log = LoggerFactory.getLogger(PingAction.class);

	private TransitDataService _tds;

	private String _response = null;

	@Autowired
	public void setTransitDataService(TransitDataService tds) {
		_tds = tds;
	}

	public DefaultHttpHeaders index() throws Exception {
		JsonObject jsonObject = new JsonObject();
		
		if(!hasAgenciesWithCoverage()){
			jsonObject.addProperty("success", "false");
		} 
		else {
			jsonObject.addProperty("success", "true");
		}

		_response = jsonObject.toString();

		ServletActionContext.getResponse().getWriter().write(_response);

		return null;
		
	}
	
	private boolean hasAgenciesWithCoverage(){
		List<AgencyWithCoverageBean> count = _tds.getAgenciesWithCoverage();
		if (count == null || count.isEmpty()) {
			_log.error("No agencies with coverage");
			return false;
		}
		_log.debug(count.size() + "agencies with coverage found");
		return true;
	}
}
