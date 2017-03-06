package org.onebusaway.nyc.gtfsrt.service;

import com.google.transit.realtime.GtfsRealtime;
import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.transit_data.model.VehicleStatusBean;

import java.util.List;

public interface TripUpdateFeedBuilder {
    GtfsRealtime.TripUpdate makeTripUpdate(VehicleStatusBean vehicle,
                                           List<TimepointPredictionRecord> records);
}
