package org.onebusaway.nyc.gtfsrt.util;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.nyc.report.model.ArchivedInferredLocationRecord;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;

import java.util.List;

public class InferredLocationReader extends RecordReader<VehicleLocationRecordBean> {

    public List<VehicleLocationRecordBean> getRecords(String filename) {
        return getRecords(filename, ArchivedInferredLocationRecord.class);
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
