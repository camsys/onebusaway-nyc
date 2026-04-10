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

import org.onebusaway.transit_data.model.trip_mods.ShapeModificationDiff;

/**
 * View wrapper for ShapeModificationDiff that omits raw shape point lists
 * and exposes only encoded polylines and stop boundary IDs.
 */
public class ShapeModificationDiffView {

    private final ShapeModificationDiff shapeDiff;

    public ShapeModificationDiffView(ShapeModificationDiff shapeDiff) {
        this.shapeDiff = shapeDiff;
    }

    public String getOriginalShapePolyline() { return shapeDiff.getOriginalShapePolyline(); }

    public String getModifiedShapePolyline() { return shapeDiff.getModifiedShapePolyline(); }

    public String getOriginalSegmentPolyline() { return shapeDiff.getOriginalSegmentPolyline(); }

    public String getReplacementSegmentPolyline() { return shapeDiff.getReplacementSegmentPolyline(); }

    public String getPrefixSegmentPolyline() { return shapeDiff.getPrefixSegmentPolyline(); }

    public String getSuffixSegmentPolyline() { return shapeDiff.getSuffixSegmentPolyline(); }

    public String getStartStopId() { return shapeDiff.getStartStopId(); }

    public String getEndStopId() { return shapeDiff.getEndStopId(); }
}