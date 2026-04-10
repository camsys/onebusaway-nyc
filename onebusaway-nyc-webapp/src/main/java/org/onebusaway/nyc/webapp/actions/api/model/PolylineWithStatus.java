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

/**
 * An encoded polyline segment annotated with its detour status relative to
 * the canonical route shape.
 *
 * detourStatus values:
 *   "canonical"  - part of the normal, unmodified route shape
 *   "detour"     - replacement segment introduced by a trip modification
 *   "removed"    - segment of the original shape that is being bypassed
 */
public class PolylineWithStatus {

    private final String line;
    private final String detourStatus;

    public PolylineWithStatus(String line, String detourStatus) {
        this.line = line;
        this.detourStatus = detourStatus;
    }

    public String getLine() {
        return line;
    }

    public String getDetourStatus() {
        return detourStatus;
    }
}
