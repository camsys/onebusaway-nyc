package org.onebusaway.nyc.gtfsrt.tests;

import com.google.transit.realtime.GtfsRealtime;
import junit.framework.TestCase;
import org.junit.Test;
import org.onebusaway.nyc.gtfsrt.impl.VehicleUpdateFeedBuilderImpl;
import org.onebusaway.nyc.gtfsrt.service.VehicleUpdateFeedBuilder;
import org.onebusaway.nyc.gtfsrt.util.InferredLocationReader;
import org.onebusaway.realtime.api.VehicleLocationRecord;

import java.util.List;

public abstract class VehiclePositionTest extends TestCase {

    private String inferenceFile;

    private VehicleUpdateFeedBuilder feedBuilder = new VehicleUpdateFeedBuilderImpl();

    public VehiclePositionTest(String inferenceFile) {
        this.inferenceFile = inferenceFile;
    }

    @Test
    public void test() {
        List<VehicleLocationRecord> records = new InferredLocationReader().getRecords(inferenceFile);
        assertFalse(records.isEmpty());

        for (VehicleLocationRecord record : records) {
            GtfsRealtime.VehiclePositionOrBuilder vp = feedBuilder.getVehicleUpdateFromInferredLocation(record);
            GtfsRealtime.PositionOrBuilder pos = vp.getPositionOrBuilder();
            assertEquals(pos.getLatitude(), (float) record.getCurrentLocationLat());
            assertEquals(pos.getLongitude(), (float) record.getCurrentLocationLon());
            assertEquals(vp.getVehicle().getId(), record.getVehicleId().getId());
        }
    }

}
