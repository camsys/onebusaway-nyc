/**
 * Copyright (C) 2024 Metropolitan Transportation Authority
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
package org.onebusaway.nyc.webapp.actions.api.model;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data.model.trip_mods.StopChangeDiff;
import org.onebusaway.transit_data.model.trip_mods.StopTimeSnapshot;
import org.onebusaway.transit_data.model.trip_mods.TripModificationDiff;

import java.util.List;
import java.util.Map;

/**
 * View wrapper for TripModificationDiff that replaces the shapeDiff field with
 * a ShapeModificationDiffView to omit raw shape point lists from API responses.
 */
public class TripModificationDiffView {

    private final TripModificationDiff diff;
    private final ShapeModificationDiffView shapeDiffView;
    private final String routeId;

    public TripModificationDiffView(TripModificationDiff diff, String routeId) {
        this.diff = diff;
        this.routeId = routeId;
        this.shapeDiffView = diff.getShapeDiff() != null
                ? new ShapeModificationDiffView(diff.getShapeDiff())
                : null;
    }

    public String getRouteId() { return routeId; }

    public String getEntityId() { return diff.getEntityId(); }

    public String getTripId() { return diff.getTripId(); }

    public long getEffectiveServiceDate() { return diff.getEffectiveServiceDate(); }

    public long getLastUpdated() { return diff.getLastUpdated(); }

    public Map<AgencyAndId, StopTimeSnapshot> getOriginalStopTimes() { return diff.getOriginalStopTimes(); }

    public Map<AgencyAndId, StopTimeSnapshot> getModifiedStopTimes() { return diff.getModifiedStopTimes(); }

    public List<StopChangeDiff> getChanges() { return diff.getChanges(); }

    public ShapeModificationDiffView getShapeDiff() { return shapeDiffView; }

    public Map<Integer, StopTimeSnapshot> getRemovedBySequence() { return diff.getRemovedBySequence(); }

    public Map<Integer, StopTimeSnapshot> getAddedBySequence() { return diff.getAddedBySequence(); }
}