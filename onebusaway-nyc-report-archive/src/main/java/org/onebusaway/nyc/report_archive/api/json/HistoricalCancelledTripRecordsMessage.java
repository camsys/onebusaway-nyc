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

package org.onebusaway.nyc.report_archive.api.json;

import org.onebusaway.nyc.report_archive.model.NycCancelledTripRecord;

import java.io.Serializable;
import java.util.List;

public class HistoricalCancelledTripRecordsMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<NycCancelledTripRecord> records;
    private String status;

    /**
     * @param records the records to set
     */
    public void setRecords(List<NycCancelledTripRecord> records) {
        this.records = records;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

}

