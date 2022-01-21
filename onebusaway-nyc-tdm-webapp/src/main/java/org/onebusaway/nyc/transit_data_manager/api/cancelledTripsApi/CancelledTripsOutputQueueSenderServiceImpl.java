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
package org.onebusaway.nyc.transit_data_manager.api.cancelledTripsApi;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.onebusaway.nyc.transit_data.model.NycCancelledTripBean;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ArrayBlockingQueue;


public class CancelledTripsOutputQueueSenderServiceImpl {

    private static Logger _log = LoggerFactory.getLogger(CancelledTripsOutputQueueSenderServiceImpl.class);
    private final ArrayBlockingQueue<String> _outputBuffer = new ArrayBlockingQueue<String>(
            100);

    /**
     * enqueues NycCanceledTripBeans,
     * if refactor, near duplicate of onebusaway-nyc-vehicle-tracking/src/main/java/org/onebusaway/nyc/vehicle_tracking/impl/queue/OutputQueueSenderServiceImpl.java
     *
     * @author caylasavitzky
     *
     */

//    todo write the damn thing

    private int port;
    private String location;

    public void setLocation(String location) {
        this.location = location;
    }

    public void setPort(int port) {
        this.port = port;
    }


    public void enqueue(NycCancelledTripBean r){
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        objectMapper.setTimeZone(Calendar.getInstance().getTimeZone());
        _log.info("pretending to enqueue a cancelled trip: " + r.getTrip() + " to " + location + "/" + port);
        try {

            final StringWriter sw = new StringWriter();
            final MappingJsonFactory jsonFactory = new MappingJsonFactory();
            final JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(sw);
            objectMapper.writeValue(jsonGenerator, r);
            sw.close();

            _log.info("pretend i just sent: " + sw.toString());

            _outputBuffer.put(sw.toString());
        } catch (final IOException e) {
            _log.info("Could not serialize inferred location record: "
                    + e.getMessage(), e);
        } catch (final InterruptedException e) {
            // discard if thread is interrupted or serialization fails
            return;
        }
    }
}
