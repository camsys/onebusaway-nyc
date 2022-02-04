package org.onebusaway.nyc.report_archive.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.onebusaway.nyc.report_archive.api.HistoricalRecordsResource;
import org.onebusaway.nyc.report_archive.api.json.HistoricalCancelledTripRecordsMessage;
import org.onebusaway.nyc.report_archive.impl.CancelledTripDaoImpl;
import org.onebusaway.nyc.report_archive.model.NycCancelledTripRecord;
import org.onebusaway.nyc.report_archive.result.HistoricalRecord;
import org.onebusaway.nyc.report_archive.services.CancelledTripDao;
import org.onebusaway.nyc.report_archive.services.HistoricalRecordsDao;
import org.onebusaway.nyc.report.api.json.JsonTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

@Component
@Path("/jared")
public class HistoricalCancelledTripRecordsResource {


    private static Logger log = LoggerFactory.getLogger(HistoricalRecordsResource.class);
    private JsonTool jsonTool;

    @Autowired
    private CancelledTripDao cancelledTripDao;

    @Path("/list")
    @GET
    @Produces("application/json")
    public Response getHistoricalCancelledTripRecords() {
        log.info("Starting getHistoricalRecords");
        long now = System.currentTimeMillis();

        List<NycCancelledTripRecord> historicalRecords = null;
        HistoricalCancelledTripRecordsMessage recordsMessage = new HistoricalCancelledTripRecordsMessage();

        try {
            historicalRecords = cancelledTripDao.getReports();
            log.info("HistoriicalRecords= "+historicalRecords);
            recordsMessage.setRecords(historicalRecords);
            recordsMessage.setStatus("OK");

        } catch (UncategorizedSQLException sql) {
            // here we make the assumption that an exception means query timeout
            recordsMessage.setRecords(null);
            recordsMessage.setStatus("QUERY_TIMEOUT");
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
        //log.info(filtersToString(filters));

        return response;
    }

    public void saveRecord() {
        NycCancelledTripRecord record = new NycCancelledTripRecord();
        record.setBlock("Test");
        CancelledTripDaoImpl dao = new CancelledTripDaoImpl();
        dao.saveReport(record);
    }

    public List<NycCancelledTripRecord> getReports(){
        List<NycCancelledTripRecord> result = cancelledTripDao.getReports();
        log.info("Resource result="+result);
        return result;

    }

    private String getObjectAsJsonString(Object object) throws IOException {
        log.info("In getObjectAsJsonString, serializing input object as json.");

        String outputJson = null;

        StringWriter writer = null;

        try {
            writer = new StringWriter();
            jsonTool.writeJson(writer, object);
            outputJson = writer.toString();
        } catch (IOException e) {
            throw new IOException("IOException while using jsonTool to write object as json.", e);
        } finally {
            if (writer != null)
                try {
                    writer.close();
                } catch (IOException e) { }
        }

        if (outputJson == null) throw new IOException("After using jsontool to write json, output was still null.");

        return outputJson;
    }

    /**
     * Injects json tool
     * @param jsonTool the jsonTool to set
     */
    @Autowired
    public void setJsonTool(JsonTool jsonTool) {
        this.jsonTool = jsonTool;
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
