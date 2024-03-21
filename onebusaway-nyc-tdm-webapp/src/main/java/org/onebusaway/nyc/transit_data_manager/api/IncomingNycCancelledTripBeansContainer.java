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

package org.onebusaway.nyc.transit_data_manager.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.onebusaway.transit_data.model.trips.CancelledTripBean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IncomingNycCancelledTripBeansContainer implements Serializable {

    private static final long serialVersionUID = 1L;

    ArrayList<CancelledTripBean> beans;
    Date timestamp;

    public void setTimestamp( Date timestamp) {
        this.timestamp = timestamp;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    @JsonProperty("impacted")
    public void setBeans(ArrayList<CancelledTripBean> beans) {
        this.beans = beans;
    }

    public ArrayList<CancelledTripBean> getBeans() {
        return beans;
    }
}
