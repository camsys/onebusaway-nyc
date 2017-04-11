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
package org.onebusaway.nyc.gtfsrt.impl;

import com.google.transit.realtime.GtfsRealtime.*;
import org.onebusaway.nyc.gtfsrt.service.FeedMessageService;

import java.util.List;

public abstract class AbstractFeedMessageService implements FeedMessageService {

    public static final int DEFAULT_CACHE_EXPIRY_SECONDS = 10;

    private String _agencyId;
    private long _generatedTime = 0l;
    private int _cacheExpirySeconds = DEFAULT_CACHE_EXPIRY_SECONDS;
    private FeedMessage _cache = null;

    public void setCacheExpirySeconds(int seconds) {
        _cacheExpirySeconds = seconds;
    }

    @Override
    public synchronized FeedMessage getFeedMessage() {
        long time = System.currentTimeMillis();

        if (isCacheExpired(time)) {
            FeedMessage.Builder builder = FeedMessage.newBuilder();

            for (FeedEntity.Builder entity : getEntities(time))
                if (entity != null)
                    builder.addEntity(entity);

            FeedHeader.Builder header = FeedHeader.newBuilder();
            header.setGtfsRealtimeVersion("1.0");
            header.setTimestamp(time / 1000);
            header.setIncrementality(FeedHeader.Incrementality.FULL_DATASET);
            builder.setHeader(header);
            FeedMessage message = builder.build();
            _cache = message;
            _generatedTime = time;
        }
        return _cache;
    }

    public String getAgencyId() {
        return _agencyId;
    }

    public void setAgencyId(String agencyId) {
        _agencyId = agencyId;
    }

    private boolean isCacheExpired(long now) {
        if (_cache == null) return true;
        return (now - _generatedTime) > _cacheExpirySeconds * 1000;
    }

    protected abstract List<FeedEntity.Builder> getEntities(long time);
}
