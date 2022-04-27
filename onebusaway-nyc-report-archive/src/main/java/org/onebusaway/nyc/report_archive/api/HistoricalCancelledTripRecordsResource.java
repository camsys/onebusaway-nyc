/**
 * Copyright (C) 2022 Metropolitan Transportation Authority
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

package org.onebusaway.nyc.report_archive.api;

import javax.annotation.PostConstruct;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.onebusaway.nyc.report_archive.api.json.HistoricalCancelledTripRecordsMessage;
import org.onebusaway.nyc.report_archive.impl.CancelledTripDaoImpl;
import org.onebusaway.nyc.report_archive.model.NycCancelledTripRecord;
import org.onebusaway.nyc.report_archive.services.CancelledTripDao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

@Component
@Scope("request")
@Path("/cancelled-trips/{serviceDate}/list")
public class HistoricalCancelledTripRecordsResource {


    private static Logger log = LoggerFactory.getLogger(HistoricalCancelledTripRecordsResource.class);

    private static ObjectMapper _mapper = new ObjectMapper()
            .setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"))
            .registerModule(new JavaTimeModule())
            .setTimeZone(Calendar.getInstance().getTimeZone())
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    @Autowired
    private CancelledTripDao cancelledTripDao;

    @GET
    @Produces("application/json")
    public Response getHistoricalCancelledTripRecords(
            @QueryParam(value="numberOfRecords")  Integer requestedNumberOfRecords,
            @QueryParam(value="trip")  String requestedTrip,
            @QueryParam(value="block")  String requestedBlock,
            @PathParam(value="serviceDate") String requestedDate,
            @QueryParam(value="startTime")  String startTime,
            @QueryParam(value="endTime")  String endTime
    ) {
        final Integer MAX_RECORDS = 5000;
        Integer numberOfRecords = MAX_RECORDS;
        if (requestedNumberOfRecords != null){
            numberOfRecords = (requestedNumberOfRecords < MAX_RECORDS) ? requestedNumberOfRecords: MAX_RECORDS;
        }

        log.info("Starting getHistoricalRecords");
        long now = System.currentTimeMillis();

        List<NycCancelledTripRecord> historicalRecords = null;
        HistoricalCancelledTripRecordsMessage recordsMessage = new HistoricalCancelledTripRecordsMessage();

        HistoricalCancelledTripQuery query = new HistoricalCancelledTripQuery();
        query.setNumberOfRecords(numberOfRecords);
        query.setRequestedTrip(requestedTrip);
        query.setRequestedBlock(requestedBlock);
        query.setRequestedDate(requestedDate);
        query.setStartTime(startTime);
        query.setEndTime(endTime);

        try {
            historicalRecords = cancelledTripDao.getReports(query);
            log.info("HistoricalRecords= {}",historicalRecords.size());
            recordsMessage.setRecords(historicalRecords);
            recordsMessage.setStatus("OK");

        } catch (UncategorizedSQLException sql) {
            // here we make the assumption that an exception means query timeout
            recordsMessage.setRecords(null);
            recordsMessage.setStatus("QUERY_TIMEOUT");
        } catch (java.text.ParseException p){
            recordsMessage.setRecords(null);
            recordsMessage.setStatus("Unable to parse date: "+p);
        }

        String outputJson;
        try {
            outputJson = getObjectAsJsonString(recordsMessage);
        } catch (IOException e1) {
            log.error("Unable to complete request, query took " + (System.currentTimeMillis() - now)  + "ms");
            //log.error(filtersToString(filters));
            throw new WebApplicationException(e1, Response.Status.INTERNAL_SERVER_ERROR);
        }

        log.info("outputjson= "+outputJson);

        Response response = Response.ok(outputJson, "application/json").build();

        log.info("Returning response from getHistoricalRecords, query took " + (System.currentTimeMillis() - now)  + "ms");

        return response;
    }

    public void saveRecord() {
        NycCancelledTripRecord record = new NycCancelledTripRecord();
        record.setBlock("Test");
        CancelledTripDaoImpl dao = new CancelledTripDaoImpl();
        dao.saveReport(record);
    }

    public String getObjectAsJsonString(Object object) throws IOException {
        StringWriter writer = null;
        String output = null;
        try {
            writer = new StringWriter();
            _mapper.writeValue(writer, object);
            output = writer.toString();
        } catch (IOException e) {
            log.error("exception parsing json " + e, e);
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                log.error("Error closing writer", e);
            }
        }

        if (output == null) throw new IOException("Unable to parse object into json");
        return output;
    }

    /**
     *
     * @param cancelledTripDao the historicalRecordsDao to set
     */
    @Autowired
    public void setCancelledTripDao(CancelledTripDao cancelledTripDao) {
        this.cancelledTripDao = cancelledTripDao;
    }

}
