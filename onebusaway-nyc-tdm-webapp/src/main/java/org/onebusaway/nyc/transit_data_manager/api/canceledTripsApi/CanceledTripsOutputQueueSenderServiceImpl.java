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
package org.onebusaway.nyc.transit_data_manager.api.canceledTripsApi;

import org.onebusaway.nyc.transit_data.model.NycCancledTripBean;


public class CanceledTripsOutputQueueSenderServiceImpl {

    /**
     * enqueues NycCanceledTripBeans,
     * if refactor, near duplicate of onebusaway-nyc-vehicle-tracking/src/main/java/org/onebusaway/nyc/vehicle_tracking/impl/queue/OutputQueueSenderServiceImpl.java
     *
     * @author caylasavitzky
     *
     */

    public void enqueue(NycCancledTripBean r){
        return;
    }

}
