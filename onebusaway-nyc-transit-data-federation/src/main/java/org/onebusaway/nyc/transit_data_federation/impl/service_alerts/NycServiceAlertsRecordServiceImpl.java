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

package org.onebusaway.nyc.transit_data_federation.impl.service_alerts;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data.model.service_alerts.SituationQueryBean;
import org.onebusaway.transit_data_federation.impl.service_alerts.ServiceAlertRecord;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.BlockTripInstance;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlertsRecordService;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NycServiceAlertsRecordServiceImpl implements ServiceAlertsRecordService {

    @Override
    public void loadServiceAlerts() {

    }

    @Override
    public ServiceAlertRecord createOrUpdateServiceAlert(ServiceAlertRecord serviceAlertRecord) {
        return null;
    }

    @Override
    public void removeServiceAlert(AgencyAndId agencyAndId) {

    }

    @Override
    public ServiceAlertRecord copyServiceAlert(ServiceAlertRecord serviceAlertRecord) {
        return null;
    }

    @Override
    public void removeServiceAlerts(List<AgencyAndId> list) {

    }

    @Override
    public void removeAllServiceAlertsForFederatedAgencyId(String s) {

    }

    @Override
    public ServiceAlertRecord getServiceAlertForId(AgencyAndId agencyAndId) {
        return null;
    }

    @Override
    public List<ServiceAlertRecord> getAllServiceAlerts() {
        return null;
    }

    @Override
    public List<ServiceAlertRecord> getServiceAlertsForFederatedAgencyId(String s) {
        return null;
    }

    @Override
    public List<ServiceAlertRecord> getServiceAlertsForAgencyId(long l, String s) {
        return null;
    }

    @Override
    public List<ServiceAlertRecord> getServiceAlertsForStopId(long l, AgencyAndId agencyAndId) {
        return null;
    }

    @Override
    public List<ServiceAlertRecord> getServiceAlertsForStopCall(long l, BlockInstance blockInstance, BlockStopTimeEntry blockStopTimeEntry, AgencyAndId agencyAndId) {
        return null;
    }

    @Override
    public List<ServiceAlertRecord> getServiceAlertsForVehicleJourney(long l, BlockTripInstance blockTripInstance, AgencyAndId agencyAndId) {
        return null;
    }

    @Override
    public List<ServiceAlertRecord> getServiceAlerts(SituationQueryBean situationQueryBean) {
        return null;
    }
}
