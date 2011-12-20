package org.onebusaway.nyc.report_archive.api;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.onebusaway.nyc.report_archive.api.json.JsonTool;
import org.onebusaway.nyc.report_archive.api.json.LastKnownRecordMessage;
import org.onebusaway.nyc.report_archive.api.json.LastKnownRecordsMessage;
import org.onebusaway.nyc.report_archive.model.CcAndInferredLocationRecord;
import org.onebusaway.nyc.report_archive.services.NycQueuedInferredLocationDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Path("/record/last-known")
@Component
public class LastKnownLocationResource {

	private static Logger _log = LoggerFactory
			.getLogger(LastKnownLocationResource.class);

	@Autowired
	private NycQueuedInferredLocationDao _locationDao;

	@Autowired
	private JsonTool _jsonTool;

	public void set_locationDao(NycQueuedInferredLocationDao _locationDao) {
		this._locationDao = _locationDao;
	}

	public void set_jsonTool(JsonTool _jsonTool) {
		this._jsonTool = _jsonTool;
	}
	
	@Path("/vehicle/{vehicleId}")
	@GET
	@Produces("application/json")
	public Response getlastLocationRecordForVehicle(@PathParam("vehicleId") Integer vehicleId) {
		_log.info("Starting getlastLocationRecordForVehicle.");
		
		String outputJson;
		
		try {
			CcAndInferredLocationRecord record = getLastKnownRecordForVehicleFromDao(vehicleId);
			
			LastKnownRecordMessage message = new LastKnownRecordMessage();
			message.setRecord(record);
			message.setStatus("OK");
			
			outputJson = getObjectAsJsonString(message);
		} catch (Exception e) {
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
		
		Response response = Response.ok(outputJson, "application/json").build();
		
		_log.info("Returning ok response from getlastLocationRecordForVehicle");
		return response;
	}

	@Path("/list")
	@GET
	@Produces("application/json")
	public Response getAllLastLocationRecords() {

		_log.info("Starting getAllLastLocationRecords");
		List<CcAndInferredLocationRecord> lastKnownRecords = getLastKnownRecordsFromDao();
		
		LastKnownRecordsMessage message = new LastKnownRecordsMessage();
		message.setRecords(lastKnownRecords);
		message.setStatus("OK");

		String outputJson;
		try {
			outputJson = getObjectAsJsonString(message);
		} catch (IOException e1) {
			throw new WebApplicationException(e1, Response.Status.INTERNAL_SERVER_ERROR);
		}
		
		Response response = Response.ok(outputJson, "application/json").build();

		_log.info("Returning ok response in getAllLastLocationRecords");
		return response;
	}
	
	private List<CcAndInferredLocationRecord> getLastKnownRecordsFromDao() {
		_log.info("Fetching all last known vehicle location records from dao.");
		List<CcAndInferredLocationRecord> lastKnownRecords = _locationDao
				.getAllLastKnownRecords();

		return lastKnownRecords;
	}


	private CcAndInferredLocationRecord getLastKnownRecordForVehicleFromDao(
			Integer vehicleId) throws Exception {
		_log.info("Fetching last known record for vehicle " + String.valueOf(vehicleId));
		CcAndInferredLocationRecord record = _locationDao.getLastKnownRecordForVehicle(vehicleId);
		
		return record;
	}
	
	private String getObjectAsJsonString(Object object) throws IOException {
		_log.info("In getObjectAsJsonString, serializing input object as json.");
		
		String outputJson = null;
		
		StringWriter writer = null;
		
		try {
			writer = new StringWriter();
			_jsonTool.writeJson(writer, object);
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
}
