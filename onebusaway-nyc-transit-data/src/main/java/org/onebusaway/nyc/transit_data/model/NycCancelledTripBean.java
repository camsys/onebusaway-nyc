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

package org.onebusaway.nyc.transit_data.model;

/**
 * Serializable model bean for cancled trips
 *
 * @author caylasavitzky
 *
 */

import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.Date;

public class NycCancelledTripBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private String block;

    private String trip;

    private String status;

    private String actor;

    private Date timestamp;

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTrip(String trip) {
        this.trip = trip;
    }

    public String getTrip() {
        return trip;
    }

    public void setBlock(String block) {
        this.block = block;
    }

    public String getBlock() {
        return block;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
