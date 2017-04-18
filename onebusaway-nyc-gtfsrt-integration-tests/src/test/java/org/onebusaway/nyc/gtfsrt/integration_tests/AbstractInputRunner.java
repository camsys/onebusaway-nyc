package org.onebusaway.nyc.gtfsrt.integration_tests;

import com.google.transit.realtime.GtfsRealtime.*;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.gtfsrt.util.InferredLocationReader;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.onebusaway.utility.DateLibrary;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 *
 */
public class AbstractInputRunner {


    public static final String INFERENCE_TYPE = "inference";
    public static final String PROTOCOL_BUFFER_TYPE = "tripUpdate";

    private static final double MAX_LOCATION_DISTANCE = 300.0;

    public AbstractInputRunner(String datasetId, String bundleId, String date) throws Exception {
        setBundle(bundleId, date);
        loadInference(datasetId);
        loadTimePredictions(datasetId, date);
        loadServiceAlerts(datasetId);
        verifyTU(datasetId,  bundleId, date);
        verifyVP(datasetId,  bundleId, date);
    }

    private void verifyTU(String datasetId, String bundleId, String date) throws Exception {
        // load the vehicle
        List<VehicleLocationRecordBean> vlrbs = getInferenceRecords(datasetId);
        VehicleLocationRecordBean vehicleRecord = findBestLocationRecord(vlrbs, DateLibrary.getIso8601StringAsTime(date));
        String vehicleId = vehicleRecord.getVehicleId();
        // load the trip
        String tripId = deagency(vehicleRecord.getTripId());
        // load the stops
        FeedMessage predFeed = getPredictions(datasetId);
        List<TripUpdate.StopTimeUpdate> predictions = predFeed.getEntity(0).getTripUpdate().getStopTimeUpdateList();
        // query gtfs-rt
        // parse
        Map<String, String> params = new HashMap<String, String>();
        params.put("time", String.valueOf(DateLibrary.getIso8601StringAsTime(date).getTime()));
        FeedMessage feed = readTUUrl(params);
        assertNotNull(feed);
        assertEquals(1, feed.getEntityCount());
        FeedEntity entity = feed.getEntity(0);
        assertTrue(entity.hasTripUpdate());
        TripUpdate tripUpdate = entity.getTripUpdate();
        // assert vehicle
        assertEquals(vehicleId, tripUpdate.getVehicle().getId());
        // assert trip
        assertEquals(tripId, tripUpdate.getTrip().getTripId());
        // assert each prediction
        assertEquals(predictions.size(), tripUpdate.getStopTimeUpdateCount());
        for (int i = 0; i < predictions.size(); i++) {
            TripUpdate.StopTimeUpdate predictionStu = predictions.get(i);
            TripUpdate.StopTimeUpdate feedStu = tripUpdate.getStopTimeUpdate(i);
            assertEquals(deagency(predictionStu.getStopId()), feedStu.getStopId());
            assertEquals(predictionStu.hasArrival(), feedStu.hasArrival());
            // note feed from predictions engine has times in ms
            if (predictionStu.hasArrival()) {
                assertEquals(predictionStu.getArrival().getTime()/1000, feedStu.getArrival().getTime());
            }
            assertEquals(predictionStu.hasDeparture(), feedStu.hasDeparture());
            if (predictionStu.hasDeparture()) {
                assertEquals(predictionStu.getDeparture().getTime()/1000, feedStu.getDeparture().getTime());
            }
        }
    }

    private void verifyVP(String datasetId, String bundleId, String date) throws Exception {
        List<VehicleLocationRecordBean> vlrbs = getInferenceRecords(datasetId);
        VehicleLocationRecordBean vlrb = findBestLocationRecord(vlrbs, DateLibrary.getIso8601StringAsTime(date));
        Map<String, String> params = new HashMap<String, String>();
        params.put("time", String.valueOf(DateLibrary.getIso8601StringAsTime(date).getTime()));
        FeedMessage feed = readVPUrl(params);
        assertEquals(1, feed.getEntityCount());
        FeedEntity entity = feed.getEntity(0);
        assertTrue(entity.hasVehicle());
        VehiclePosition vp = entity.getVehicle();
        assertEquals(vlrb.getVehicleId(), vp.getVehicle().getId());
        CoordinatePoint rtPt = new CoordinatePoint(vp.getPosition().getLatitude(), vp.getPosition().getLongitude());
        double distance = SphericalGeometryLibrary.distance(vlrb.getCurrentLocation(), rtPt);
        assertTrue(distance < MAX_LOCATION_DISTANCE);
        assertEquals(deagency(vlrb.getTripId()), vp.getTrip().getTripId());
    }

    private VehicleLocationRecordBean findBestLocationRecord(List<VehicleLocationRecordBean> records, Date date) {
        Collections.sort(records, new Comparator<VehicleLocationRecordBean>() {
            @Override
            public int compare(VehicleLocationRecordBean o1, VehicleLocationRecordBean o2) {
                return (int) (o1.getTimeOfRecord() - o2.getTimeOfRecord());
            }
        });
        long time = date.getTime()/1000;
        for (VehicleLocationRecordBean vlrb : records)
            if (vlrb.getTimeOfRecord() > time)
                return vlrb;
        return records.get(records.size() - 1);
    }

    private List<VehicleLocationRecordBean> getInferenceRecords(String datasetId) throws Exception {
        String input = IOUtils.toString(getInferenceInput(datasetId));
        return new InferredLocationReader().getRecordsFromText(input);
    }

    private FeedMessage getPredictions(String datasetId) throws Exception {
        String resourceName = getFilenameFromPrefix(datasetId, PROTOCOL_BUFFER_TYPE);
        return FeedMessage.parseFrom(getResourceAsStream(resourceName));
    }

    private FeedMessage readTUUrl(Map<String, String> params) throws Exception {
        InputStream is = new WebController().get(params, "/tripUpdates");
        return FeedMessage.parseFrom(is);
    }

    private FeedMessage readVPUrl(Map<String, String> params) throws Exception {
        InputStream is = new WebController().get(params, "/vehiclePositions");
        return FeedMessage.parseFrom(is);
    }

    private void loadInference(String datasetId) throws Exception {
        new WebController().setVehicleLocationRecords(getInferenceInput(datasetId));
    }

    private void loadTimePredictions(String datasetId, String date) throws Exception {
        String resourceName = getFilenameFromPrefix(datasetId, PROTOCOL_BUFFER_TYPE);
        WebController wc = new WebController();

        wc.setTimePredictionRecordsTime(DateLibrary.getIso8601StringAsTime(date));
        wc.setTimePredictionRecords(getResourceAsStream(resourceName));
    }

    private InputStream getInferenceInput(String prefix) {
        String resourceName = getInferenceFilename(prefix);
        return getResourceAsStream(resourceName);
    }
    private InputStream getResourceAsStream(String resourceName) {
        InputStream is = this.getClass().getResourceAsStream(resourceName);
        if (is == null) {
            throw new NullPointerException(
                    "resource '" + resourceName
                    + "' did not match data on class path");
        }
        return this.getClass().getResourceAsStream(resourceName);

    }

    private String getInferenceFilename(String prefix) {
        return getFilenameFromPrefix(prefix, INFERENCE_TYPE);
    }

    private String getFilenameFromPrefix(String prefix, String type) {
        return File.separator
                + "data"
                + File.separator
                + prefix
                + File.separator
                + type
                + getExtensionForType(type);

    }
    private String getExtensionForType(String type) {
        if (INFERENCE_TYPE.equals(type)) {
            return ".tsv";
        } else if (PROTOCOL_BUFFER_TYPE.equals(type)) {
            return ".pb";
        }
        return ".txt";
    }


    private void loadServiceAlerts(String datasetId) {

    }

    public void setBundle(String bundleId, String date) throws Exception {
        new WebController().setBundle(bundleId, DateLibrary.getIso8601StringAsTime(date));
    }



    @Test
    public void testRun() throws Throwable {
        // Subclasses do the work
    }

    // take out agency component from AgencyAndId string
    private static String deagency(String id) {
        return AgencyAndId.convertFromString(id).getId();
    }
}
