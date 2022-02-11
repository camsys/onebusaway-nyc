/*
 * Copyright (C)  2011 Metropolitan Transportation Authority
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onebusaway.nyc.transit_data_federation.impl.capi;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.model.NycCancelledTripBean;
import org.onebusaway.nyc.transit_data_federation.services.capi.CancelledTripService;
import org.onebusaway.transit_data.model.ListBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CancelledTripServiceImpl implements CancelledTripService {

    protected static Logger _log = LoggerFactory.getLogger(CancelledTripServiceImpl.class);

    private Map<AgencyAndId, NycCancelledTripBean> _cancelledTripsCache = new ConcurrentHashMap<AgencyAndId, NycCancelledTripBean>();

    @Override
    public boolean isTripCancelled(String tripId) {
        NycCancelledTripBean cancelledTripBean = _cancelledTripsCache.get(AgencyAndId.convertFromString(tripId));

        if (cancelledTripBean != null) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isTripCancelled(AgencyAndId tripId) {
        NycCancelledTripBean cancelledTripBean = _cancelledTripsCache.get(tripId);
        if (cancelledTripBean != null) {
            return true;
        }
        return false;
    }

    @Override
    public Set<AgencyAndId> getCancelledTripIds() {
        return _cancelledTripsCache.keySet();
    }

    @Override
    public ListBean<NycCancelledTripBean> getAllCancelledTrips() {
        List<NycCancelledTripBean> serializedList = new ArrayList<>();
        for (NycCancelledTripBean bean : _cancelledTripsCache.values()) {
            serializedList.add(bean);
        }
        return new ListBean(serializedList, false);
    }

    @Override
    public void updateCancelledTrips(Map<AgencyAndId, NycCancelledTripBean> cancelledTripsCache){
        _cancelledTripsCache = cancelledTripsCache;
    }
}
