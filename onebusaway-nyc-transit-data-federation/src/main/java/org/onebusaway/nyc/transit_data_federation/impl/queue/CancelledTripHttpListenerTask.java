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

package org.onebusaway.nyc.transit_data_federation.impl.queue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.services.capi.CapiDao;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.transit_data.model.trips.CancelledTripBean;
import org.onebusaway.transit_data_federation.services.CancelledTripService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CancelledTripHttpListenerTask extends HTTPListenerTask{

    protected static Logger _log = LoggerFactory.getLogger(CancelledTripHttpListenerTask.class);


    @Override
    boolean getIsEnabled() {
        return getConfig().getConfigurationValueAsBoolean("tds.enableCapi", Boolean.FALSE);
    }
    @Override
    Integer getRefreshInterval() {
        return getConfig().getConfigurationValueAsInteger( "tds.capiRefreshInterval", DEFAULT_REFRESH_INTERVAL);
    }

    @Override
    @Refreshable(dependsOn = {"tds.enableCapi","tds.capiRefreshInterval"})
    protected void refreshConfig() {
        super.refreshConfig();    }

    @Override
    public String getLocation() {
        return _capiDao.getLocation();
    }

    private CapiDao _capiDao;

    @Autowired
    public void setCapiDao(CapiDao capiDao) {
        _capiDao = capiDao;
    }

    private NycTransitDataService _tds;
    @Autowired
    public void setNycTransitDataService(NycTransitDataService tds){
        _tds = tds;
    }

    private CancelledTripService _cancelledTripService;
    @Autowired
    public void setCancelledTripService(CancelledTripService cancelledTripService){
        _cancelledTripService = cancelledTripService;
    }

    private ObjectMapper _objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setDateFormat(new SimpleDateFormat("yyyy-MM-dd"))
            .setTimeZone(Calendar.getInstance().getTimeZone());

    @Override
    public void updateData()  {
        try {
            InputStream inputStream = getCancelledTripData();
            List<CancelledTripBean> cancelledTripBeans = convertResponseToCancelledTripBeans(inputStream);
            Map<AgencyAndId, CancelledTripBean> cancelledTripsMap = convertCancelledTripsToMap(cancelledTripBeans);
            _cancelledTripService.updateCancelledTrips(cancelledTripsMap);
        } catch (Exception e){
            _log.error("Unable to process cancelled trip beans from {}", getLocation());
        }
    }

    private InputStream getCancelledTripData(){
        return _capiDao.getCancelledTripData();
    }

    private List<CancelledTripBean> convertResponseToCancelledTripBeans(InputStream input) {
        try {
            List<CancelledTripBean> list = _objectMapper.readValue(input,
                    new TypeReference<List<CancelledTripBean>>() {});
            return list;
        } catch (Exception any) {
            _log.error("issue parsing json: " + any, any);
        }
        return Collections.EMPTY_LIST;
    }


    private Map<AgencyAndId, CancelledTripBean> convertCancelledTripsToMap(List<CancelledTripBean> cancelledTripBeans) {
        Map<AgencyAndId, CancelledTripBean> results = new ConcurrentHashMap<>();
        for (CancelledTripBean cancelledTrip : cancelledTripBeans) {
            if (isValidCancelledTrip(cancelledTrip)) {
                results.put(AgencyAndId.convertFromString(cancelledTrip.getTrip()), cancelledTrip);
            }
        }
        return results;
    }

    private boolean isValidCancelledTrip(CancelledTripBean cancelledTripBean) {
        try {
            if ((cancelledTripBean.getStatus().equalsIgnoreCase("canceled") ||
                    cancelledTripBean.getStatus().equalsIgnoreCase("cancelled"))) {
                if (_tds.getTrip(cancelledTripBean.getTrip()) == null) {
                    _log.debug("unknown tripId=" + cancelledTripBean.getTrip());
                    return false;
                } else {
                    return true;
                }
            }
        } catch (ServiceException e) {
            _log.warn("Error retrieving cancelled trip", e);
        }
        return false;
    }
}
