/*
 * Copyright (C)  2011 Metropolitan Transportation Authority
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onebusaway.nyc.report_archive.api;

import java.time.LocalDate;
import java.time.LocalTime;

public class HistoricalCancelledTripQuery {

    LocalDate requestedDate;
    Integer numberOfRecords;
    String requestedTrip;
    String requestedBlock;
    LocalTime startTime;
    LocalTime endTime;


    public LocalDate getRequestedDate() {
        return requestedDate;
    }

    public void setRequestedDate(String requestedDate) {
        this.requestedDate = LocalDate.parse(requestedDate);
    }

    public Integer getNumberOfRecords() {
        return numberOfRecords;
    }

    public void setNumberOfRecords(Integer numberOfRecords) {
        this.numberOfRecords = numberOfRecords;
    }

    public String getRequestedTrip() {
        return requestedTrip;
    }

    public void setRequestedTrip(String requestedTrip) {
        this.requestedTrip = requestedTrip;
    }

    public String getRequestedBlock() {
        return requestedBlock;
    }

    public void setRequestedBlock(String requestedBlock) {
        this.requestedBlock = requestedBlock;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        try {
            this.startTime = LocalTime.parse(startTime);
        }catch (Exception e){
            this.startTime = null;
        }
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        try {
            this.endTime = LocalTime.parse(endTime);
        } catch(Exception e){
            this.endTime = null;
        }
    }
}
