/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
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
package org.onebusaway.api.actions.api.where;

import org.apache.struts2.rest.DefaultHttpHeaders;
import org.onebusaway.api.actions.api.ApiActionSupport;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data.model.trip_mods.TripModificationDiff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;

public class TripModificationDiffsAction extends ApiActionSupport {

    private static final long serialVersionUID = 1L;
    private static final Logger _log = LoggerFactory.getLogger(TripModificationDiffsAction.class);
    private static final int V2 = 2;

    private NycTransitDataService _service;
    private String _serviceDate;

    public TripModificationDiffsAction() {
        super(V2);
    }

    @Autowired
    public void setNycTransitDataService(NycTransitDataService service) {
        _service = service;
    }

    public void setServiceDate(String serviceDate) {
        _serviceDate = serviceDate;
    }

    public String getServiceDate() {
        return _serviceDate;
    }

    public DefaultHttpHeaders index() {
        if (!isVersion(V2))
            return setUnknownVersionResponse();

        if (_service == null) {
            _log.error("_service is null");
            return setExceptionResponse();
        }

        try {
            Collection<TripModificationDiff> diffs = getTripModDiffs();

            if (diffs.isEmpty())
                return setResourceNotFoundResponse();

            return setOkResponse(diffs);

        } catch (Exception e) {
            _log.error("Error retrieving trip modification diffs", e);
            return setExceptionResponse();
        }
    }

    Collection<TripModificationDiff> getTripModDiffs() {
        return _service.getAllTripModificationDiffs(_serviceDate);
    }
}