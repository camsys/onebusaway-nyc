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

	private boolean hasAgenciesWithCoverage() {
		try {
			// Hard stop after 2 seconds to prevent Ping from hanging Tomcat
			long start = System.currentTimeMillis();

			List<AgencyWithCoverageBean> agencies = _tds.getAgenciesWithCoverage();

			long elapsed = System.currentTimeMillis() - start;
			if (elapsed > 2000) {
				_log.warn("PingAction: getAgenciesWithCoverage took {} ms", elapsed);
			}

			if (agencies == null || agencies.isEmpty()) {
				_log.error("PingAction: no agencies with coverage");
				return false;
			}

			return true;
		} catch (Exception ex) {
			_log.error("PingAction: Hessian call to TransitDataService FAILED", ex);
			return false;   // degrade gracefully
		}
	}

}
