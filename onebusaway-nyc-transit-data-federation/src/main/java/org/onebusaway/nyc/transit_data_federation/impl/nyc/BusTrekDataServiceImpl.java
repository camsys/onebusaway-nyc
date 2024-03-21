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

package org.onebusaway.nyc.transit_data_federation.impl.nyc;


import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.transit_data_federation.bundle.model.NycFederatedTransitDataBundle;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract.model.BustrekDatum;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract.model.Remark;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract.model.TimePoint;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract.model.TripInfo;
import org.onebusaway.nyc.transit_data_federation.impl.bundle.NycRefreshableResources;
import org.onebusaway.nyc.transit_data_federation.services.nyc.BusTrekDataService;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.utility.ObjectSerializationLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class BusTrekDataServiceImpl implements BusTrekDataService {

    private Logger _log = LoggerFactory.getLogger(BusTrekDataServiceImpl.class);

    @Autowired
    private NycFederatedTransitDataBundle _bundle;

    List<BustrekDatum> _remarks;
    List<BustrekDatum> _tripInfo;
    List<BustrekDatum> _timePoints;


    @Autowired
    public void setBundle(NycFederatedTransitDataBundle bundle) {
        _bundle = bundle;
    }

    //todo: not sure this refreshable dependsOn works
    @PostConstruct
    @Refreshable(dependsOn = {NycRefreshableResources.BUSTREKDATA_REMARK,
            NycRefreshableResources.BUSTREKDATA_TRIP_INFO,
            NycRefreshableResources.BUSTREKDATA_TIME_POINT})
    public void setup() throws IOException, ClassNotFoundException {
        _remarks = getBustrekData(_bundle.getRemarksObjPath());
        _tripInfo = getBustrekData(_bundle.getTripInfoObjPath());
        _timePoints = getBustrekData(_bundle.getTimePointsObjPath());
    }

    private List<BustrekDatum> getBustrekData(File path) throws IOException, ClassNotFoundException {
        if (path.exists()) {
            _log.info("loading data for BusTrek");
            return ObjectSerializationLibrary.readObject(path);
        } else
            return null;
    }

    @Override
    public ListBean<BustrekDatum> getStifRemarks() {
        //todo: not sure how this works, look into limitExceeded for this method
        return(new ListBean(_remarks, false));
    }

    @Override
    public ListBean<BustrekDatum> getStifTripInfos() {
        return(new ListBean(_tripInfo, false));
    }

    @Override
    public ListBean<BustrekDatum> getStifTimePoints() {
        return(new ListBean(_timePoints, false));
    }
}
