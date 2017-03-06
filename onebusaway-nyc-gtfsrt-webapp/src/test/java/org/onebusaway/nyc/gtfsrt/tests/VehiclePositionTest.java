package org.onebusaway.nyc.gtfsrt.tests;

import com.google.transit.realtime.GtfsRealtime.*;
import junit.framework.TestCase;
import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.gtfsrt.impl.TripDetailsServiceImpl;
import org.onebusaway.nyc.gtfsrt.impl.VehicleUpdateFeedBuilderImpl;
import org.onebusaway.nyc.gtfsrt.service.TripDetailsService;
import org.onebusaway.nyc.gtfsrt.service.VehicleUpdateFeedBuilder;
import org.onebusaway.nyc.gtfsrt.tds.MockTransitDataService;
import org.onebusaway.nyc.gtfsrt.util.InferredLocationReader;
import org.onebusaway.transit_data.model.TripStopTimeBean;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.onebusaway.transit_data_federation.impl.transit_graph.TransitGraphImpl;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;

import java.util.List;

public abstract class VehiclePositionTest extends TestCase {

    private static final double TOLERANCE = 0.00001;

    private String _inferenceFile;

    private VehicleUpdateFeedBuilder _feedBuilder;

    private TransitDataService _transitDataService;

    private TripDetailsService _tripDetailsService;

    public VehiclePositionTest(String gtfsFile, String defaultAgencyId, String blockTripMapFile, String inferenceFile) {

        _transitDataService = new MockTransitDataService(defaultAgencyId, gtfsFile, blockTripMapFile);
        _inferenceFile = inferenceFile;

        TripDetailsServiceImpl tripDetailsService = new TripDetailsServiceImpl();
        tripDetailsService.setTransitDataService(_transitDataService);
        _tripDetailsService = tripDetailsService;

        VehicleUpdateFeedBuilderImpl feedBuilder = new VehicleUpdateFeedBuilderImpl();
        //feedBuilder.setTripDetailsService(tripDetailsService);
        _feedBuilder = feedBuilder;

    }

    @Test
    public void test() {
        List<VehicleLocationRecordBean> records = new InferredLocationReader().getRecords(_inferenceFile);
        assertFalse(records.isEmpty());

        for (VehicleLocationRecordBean record : records) {
            _transitDataService.submitVehicleLocation(record);
            TripDetailsBean td = getTripDetails(record);
            VehiclePosition position = null;//_feedBuilder.makeVehicleUpdate(record);
            assertVehiclePositionMatches(record, td, position);
            assertStopInfoMatches(td, position);
        }
    }

    private TripDetailsBean getTripDetails(VehicleLocationRecordBean rec) {
        return null;//_tripDetailsService.getTripDetailsForVehicleStatus(rec);
    }

    private void assertVehiclePositionMatches(VehicleLocationRecordBean record, TripDetailsBean td, VehiclePosition vehiclePosition) {
        assertTripDescriptorMatches(td, vehiclePosition.getTrip());
        assertPositionMatches(record, vehiclePosition.getPosition());
        assertVehicleDescriptorMatches(record, vehiclePosition.getVehicle());
        assertEquals(record.getTimeOfRecord(), vehiclePosition.getTimestamp()*1000);
    }

    private void assertTripDescriptorMatches(TripDetailsBean bean, TripDescriptor desc) {
        assertEquals(bean.getTripId(), desc.getTripId());
        assertEquals(bean.getTrip().getRoute().getId(), desc.getRouteId());
        // TODO apparently we are using an old version of gtfs-rt that doesn't have directon (fix)
        // TODO start_time, start_date
    }

    private void assertPositionMatches(VehicleLocationRecordBean record, Position pos) {
        assertEquals(record.getCurrentLocation().getLat(), pos.getLatitude(), TOLERANCE);
        assertEquals(record.getCurrentLocation().getLon(), pos.getLongitude(), TOLERANCE);
        if (pos.hasBearing())
            assertEquals(record.getCurrentOrientation(), pos.getBearing(), TOLERANCE);
    }

    private void assertVehicleDescriptorMatches(VehicleLocationRecordBean record, VehicleDescriptor desc) {
        assertEquals(record.getVehicleId(), desc.getId());
    }
    private void assertStopInfoMatches(TripDetailsBean td, VehiclePosition vp) {
        // the relationship of the current stop (identified by stop_id and
        // current_stop_sequence) to the vehicle's position defaults to IN_TRANSIT_TO
        if (!vp.hasCurrentStatus() || vp.getCurrentStatus().equals(VehiclePosition.VehicleStopStatus.IN_TRANSIT_TO))
            assertEquals(td.getStatus().getNextStop().getId(), vp.getStopId());
        else
            throw new UnsupportedOperationException("Not implemented");

    }

    // for when we get gtfs stop sequence
    private void assertStopSequenceMatches(TripDetailsBean td, VehiclePosition vp) {
        double dist = td.getStatus().getDistanceAlongTrip();
        if (dist == 0) // trip hasn't started
            assertEquals(0, vp.getCurrentStopSequence());

        TransitGraphImpl _graph = new TransitGraphImpl(); // to mock
        TripEntry trip = _graph.getTripEntryForId(AgencyAndId.convertFromString(td.getTripId()));
        List<StopTimeEntry> stopTimeEntries = trip.getStopTimes();

        List<TripStopTimeBean> tripStopTimes = td.getSchedule().getStopTimes();

        if (stopTimeEntries.size() != tripStopTimes.size()) {
            throw new IllegalArgumentException("bad trip info");
        }

        for (int i = 0; i < tripStopTimes.size(); i++) {
            StopTimeEntry stopTimeEntry = stopTimeEntries.get(i);
            TripStopTimeBean stopTime = tripStopTimes.get(i);
            if (stopTime.getDistanceAlongTrip() >= dist) {
                assertEquals(stopTimeEntry.getSequence(), vp.getCurrentStopSequence());
                return;
            }
        }

        throw new IllegalArgumentException("Should not have reached here.");
    }

}
