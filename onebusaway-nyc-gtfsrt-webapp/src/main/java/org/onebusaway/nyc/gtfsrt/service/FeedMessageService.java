package org.onebusaway.nyc.gtfsrt.service;

import com.google.transit.realtime.GtfsRealtime;

public interface FeedMessageService {
    GtfsRealtime.FeedMessage getFeedMessage();
}
