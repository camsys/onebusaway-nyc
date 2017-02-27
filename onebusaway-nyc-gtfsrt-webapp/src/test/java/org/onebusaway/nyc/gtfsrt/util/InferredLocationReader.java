package org.onebusaway.nyc.gtfsrt.util;

import org.onebusaway.nyc.report.model.ArchivedInferredLocationRecord;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.realtime.api.VehicleLocationRecord;

import java.util.List;

public class InferredLocationReader extends RecordReader<VehicleLocationRecord> {

    public List<VehicleLocationRecord> getRecords(String filename) {
        return getRecords(filename, ArchivedInferredLocationRecord.class);
    }

    @Override
    public VehicleLocationRecord convert(Object o) {
        NycQueuedInferredLocationBean irb = ((ArchivedInferredLocationRecord) o).toNycQueuedInferredLocationBean();
        return irb.toVehicleLocationRecord();
    }

}
