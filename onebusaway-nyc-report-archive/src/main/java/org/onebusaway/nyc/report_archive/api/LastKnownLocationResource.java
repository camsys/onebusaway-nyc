package org.onebusaway.nyc.report_archive.api;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.onebusaway.nyc.report_archive.api.json.JsonTool;
import org.onebusaway.nyc.report_archive.api.json.LastKnownRecordMessage;
import org.onebusaway.nyc.report_archive.impl.CcAndInferredLocationFilter;
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
			List<CcAndInferredLocationRecord> records = getLastKnownRecordForVehicleFromDao(vehicleId);
			
			LastKnownRecordMessage message = new LastKnownRecordMessage();
			message.setRecords(records);
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
	public Response getAllLastLocationRecords(@QueryParam(value="depot-id") final String depotId,
			@QueryParam(value="inferred-route-id") final String inferredRouteId,
			@QueryParam(value="inferred-phase") final String inferredPhase,
			@QueryParam(value="bbox") final String boundingBox) {

		_log.info("Starting getAllLastLocationRecords");
		
		Map<CcAndInferredLocationFilter, String> filter = addFilterParameters(depotId, 
				inferredRouteId, inferredPhase, boundingBox);
		
		List<CcAndInferredLocationRecord> lastKnownRecords = getLastKnownRecordsFromDao(filter);
		
		LastKnownRecordMessage message = new LastKnownRecordMessage();
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
	
	private Map<CcAndInferredLocationFilter, String> addFilterParameters(String depotId, String inferredRouteId,
			String inferredPhase, String boundingBox) {
		
		Map<CcAndInferredLocationFilter, String> filter = 
				new HashMap<CcAndInferredLocationFilter, String>();
		
		filter.put(CcAndInferredLocationFilter.DEPOT_ID, depotId);
		filter.put(CcAndInferredLocationFilter.INFERRED_ROUTEID, inferredRouteId);
		filter.put(CcAndInferredLocationFilter.INFERRED_PHASE, inferredPhase);
		filter.put(CcAndInferredLocationFilter.BOUNDING_BOX, boundingBox);
		
		return filter;
	}

	private List<CcAndInferredLocationRecord> getLastKnownRecordsFromDao(Map<CcAndInferredLocationFilter, String> filter) {
		_log.info("Fetching all last known vehicle location records from dao.");
		
		List<CcAndInferredLocationRecord> lastKnownRecords = _locationDao.getAllLastKnownRecords(filter);

		return lastKnownRecords;
	}


	private List<CcAndInferredLocationRecord> getLastKnownRecordForVehicleFromDao(
			Integer vehicleId) throws Exception {
		_log.info("Fetching last known record for vehicle " + String.valueOf(vehicleId));
		CcAndInferredLocationRecord record = _locationDao.getLastKnownRecordForVehicle(vehicleId);
		
		List<CcAndInferredLocationRecord> records = new ArrayList<CcAndInferredLocationRecord>();
		records.add(record);
		
		return records;
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
