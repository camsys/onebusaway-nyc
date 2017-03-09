package org.onebusaway.nyc.gtfsrt.impl;

import com.google.transit.realtime.GtfsRealtime.*;
import org.onebusaway.nyc.gtfsrt.service.FeedMessageService;

import java.util.List;

public abstract class AbstractFeedMessageService implements FeedMessageService {

    private static final int REFRESH_INTERVAL_MS = 30000;

    private FeedMessage message;
    long timestamp = -1;
    private String _agencyId;

    @Override
    public FeedMessage getFeedMessage() {
        long time = System.currentTimeMillis();
        if (timestamp + REFRESH_INTERVAL_MS >= time && message != null)
            return message;

        timestamp = time;
        FeedMessage.Builder builder = FeedMessage.newBuilder();

        for (FeedEntity.Builder entity : getEntities())
            if (entity != null)
                builder.addEntity(entity);

        FeedHeader.Builder header = FeedHeader.newBuilder();
        header.setGtfsRealtimeVersion("1.0");
        header.setTimestamp(time/1000);
        header.setIncrementality(FeedHeader.Incrementality.FULL_DATASET);
        builder.setHeader(header);
        message = builder.build();
        return message;
    }

    protected long getTime() {
        return timestamp;
    }

    public String getAgencyId() {
        return _agencyId;
    }

    public void setAgencyId(String agencyId) {
        _agencyId = agencyId;
    }

    protected abstract List<FeedEntity.Builder> getEntities();
}
