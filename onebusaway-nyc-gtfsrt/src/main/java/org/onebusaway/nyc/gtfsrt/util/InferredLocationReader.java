/**
 * Copyright (C) 2017 Cambridge Systematics, Inc.
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
package org.onebusaway.nyc.gtfsrt.util;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.nyc.report.model.ArchivedInferredLocationRecord;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;

import java.util.List;

/**
 * Read {@link VehicleLocationRecordBean} objects from a TSV.
 */
public class InferredLocationReader extends RecordReader<VehicleLocationRecordBean> {

    /**
     * Get @{link VehicleLocationRecordBean} objects from a file.
     *
     * @param filename file to read from
     * @return records
     */
    public List<VehicleLocationRecordBean> getRecords(String filename) {
        return getRecords(filename, ArchivedInferredLocationRecord.class);
    }

    /**
     * Get @{link VehicleLocationRecordBean} objects from a string.
     *
     * @param csv text to read from
     * @return records
     */
    public List<VehicleLocationRecordBean> getRecordsFromText(String csv) {
        return getRecordsFromText(csv, ArchivedInferredLocationRecord.class);
    }

    @Override
    public VehicleLocationRecordBean convert(Object o) {
        NycQueuedInferredLocationBean irb = ((ArchivedInferredLocationRecord) o).toNycQueuedInferredLocationBean();
        VehicleLocationRecord vlr = irb.toVehicleLocationRecord();
        return getVehicleLocationRecordAsBean(vlr);
    }

    // taken from VehicleStatusBeanServiceImpl in appmods
    private VehicleLocationRecordBean getVehicleLocationRecordAsBean(
            VehicleLocationRecord record) {

        VehicleLocationRecordBean bean = new VehicleLocationRecordBean();
        bean.setBlockId(AgencyAndIdLibrary.convertToString(record.getBlockId()));

        if (record.getPhase() != null)
            bean.setPhase(record.getPhase().toLabel());

        if (record.isCurrentLocationSet()) {
            CoordinatePoint location = new CoordinatePoint(
                    record.getCurrentLocationLat(), record.getCurrentLocationLon());
            bean.setCurrentLocation(location);
        }
        if (record.isCurrentOrientationSet())
            bean.setCurrentOrientation(record.getCurrentOrientation());

        if (record.isDistanceAlongBlockSet())
            bean.setDistanceAlongBlock(record.getDistanceAlongBlock());

        if (record.isScheduleDeviationSet())
            bean.setScheduleDeviation(record.getScheduleDeviation());

        bean.setStatus(record.getStatus());
        bean.setServiceDate(record.getServiceDate());
        bean.setTimeOfRecord(record.getTimeOfRecord());
        bean.setTripId(AgencyAndIdLibrary.convertToString(record.getTripId()));
        bean.setVehicleId(AgencyAndIdLibrary.convertToString(record.getVehicleId()));
        return bean;
    }

}
