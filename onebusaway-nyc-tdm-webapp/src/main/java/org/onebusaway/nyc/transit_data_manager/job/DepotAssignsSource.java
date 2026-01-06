/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
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

package org.onebusaway.nyc.transit_data_manager.job;

import java.io.InputStream;
import java.io.IOException;

/**
 * Interface for fetching depot assignments from different sources
 */
public interface DepotAssignsSource {

    /**
     * Fetch depot assignments XML data from the source
     *
     * @return InputStream containing the XML data
     * @throws IOException if there's an error fetching the data
     */
    InputStream fetchDepotAssignments() throws IOException;

    /**
     * Get the source type identifier
     *
     * @return String identifying the source type (e.g., "HTTP", "S3")
     */
    String getSourceType();

    /**
     * Check if the source is properly configured and available
     *
     * @return true if the source is available, false otherwise
     */
    boolean isAvailable();
}