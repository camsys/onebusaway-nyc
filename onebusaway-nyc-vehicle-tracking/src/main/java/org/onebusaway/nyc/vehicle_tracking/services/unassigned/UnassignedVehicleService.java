package org.onebusaway.nyc.vehicle_tracking.services.unassigned;

import org.onebusaway.nyc.queue.model.RealtimeEnvelope;

import java.net.URL;

public interface UnassignedVehicleService {

    void setUrl(URL url);
    void setup();
    void destroy();
    // for testing only!
    void enqueueTestRecord(RealtimeEnvelope envelope);
}
