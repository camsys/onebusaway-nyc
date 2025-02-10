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

package org.onebusaway.nyc.webapp.actions.api;

import org.json.JSONObject;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ActiveBundleAction extends OneBusAwayNYCActionSupport {

    private TransitDataService _tds;

    @Autowired
    HttpServletResponse servletResponse;

    @Autowired
    public void setTransitDataService(TransitDataService tds) {
        _tds = tds;
    }

    @Override
    public String execute() {
        try {
            JSONObject json = new JSONObject();
            json.put("bundleid", _tds.getActiveBundleId());
            servletResponse.setContentType("application/json");
            servletResponse.getWriter().write(json.toString());
        } catch (Throwable t) {
            servletResponse.setStatus(500);
        }
        return null;
    }
}
