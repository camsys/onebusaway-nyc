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

import org.onebusaway.transit_data.model.StopBean;

/**
 * A stop on a route with an additional detour status field indicating whether
 * the stop is part of the normal route, a detour, or has been removed by a
 * trip modification.
 *
 * detourStatus values:
 *   "canonical"  - stop is served normally
 *   "detour"     - stop was added by a trip modification
 *   "removed"    - stop is skipped due to a trip modification
 */
public class StopOnRouteV2 extends StopOnRoute {

    private final String detourStatus;

    public StopOnRouteV2(StopBean stop, String detourStatus) {
        super(stop);
        this.detourStatus = detourStatus;
    }

    public String getDetourStatus() {
        return detourStatus;
    }
}
