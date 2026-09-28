/**
 * Copyright (C) 2026 Metropolitan Transportation Authority
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
package org.onebusaway.nyc.transit_data_manager.api.service;

/**
 * Describes a single upstream Trip Modifications feed that this TDM node fetches, caches, and
 * republishes at /api/trip-modifications/{feedId}/feed.
 */
public class TripModificationsFeedConfig {

    private final String feedId;
    private final String url;
    private final boolean enabled;
    private final long updateIntervalMs;

    public TripModificationsFeedConfig(String feedId, String url, boolean enabled, long updateIntervalMs) {
        this.feedId = feedId;
        this.url = url;
        this.enabled = enabled;
        this.updateIntervalMs = updateIntervalMs;
    }

    public String getFeedId() {
        return feedId;
    }

    public String getUrl() {
        return url;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getUpdateIntervalMs() {
        return updateIntervalMs;
    }
}
