package org.onebusaway.nyc.report_archive.api;

import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.onebusaway.nyc.report_archive.api.json.HistoricalRecordsMessage;
import org.onebusaway.nyc.report_archive.api.json.JsonTool;
import org.onebusaway.nyc.report_archive.impl.CcAndInferredLocationFilter;
import org.onebusaway.nyc.report_archive.result.HistoricalRecord;
import org.onebusaway.nyc.report_archive.services.HistoricalRecordsDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.stereotype.Component;

@Path("/history/record/last-known")
@Component
public class HistoricalRecordsResource {

	private static Logger log = LoggerFactory.getLogger(HistoricalRecordsResource.class);
	private static DateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private JsonTool jsonTool;
	private HistoricalRecordsDao historicalRecordsDao;
	
	@Path("/list")
	@GET
	@Produces("application/json")
	public Response getHistoricalRecords(@QueryParam(value="depot-id") final String depotId,
			@QueryParam(value="inferred-route-id") final String inferredRouteId,
			@QueryParam(value="inferred-phase") final String inferredPhase,
			@QueryParam(value="vehicle-id") final Integer vehicleId,
			@QueryParam(value="vehicle-agency-id") final String vehicleAgencyId,
			@QueryParam(value="bbox") final String boundingBox,
			@QueryParam(value="start-date") final String startDate, 
			@QueryParam(value="end-date") final String endDate,
			@QueryParam(value="records") final Integer records,
			@QueryParam(value="timeout") final Integer timeout) {
		
		log.info("Starting getHistoricalRecords");
		
		Map<CcAndInferredLocationFilter, Object> filters = addFilterParameters(depotId, 
				inferredRouteId, inferredPhase, vehicleId, vehicleAgencyId, boundingBox, 
				startDate, endDate, records, timeout);
		
		List<HistoricalRecord> historicalRecords = null; 
		HistoricalRecordsMessage historicalRecordMessage = new HistoricalRecordsMessage();
		try {
		  historicalRecords = historicalRecordsDao.getHistoricalRecords(filters);
	    historicalRecordMessage.setRecords(historicalRecords);
	    historicalRecordMessage.setStatus("OK");

		} catch (UncategorizedSQLException sql) {
		  // here we make the assumption that an exception means query timeout
      historicalRecordMessage.setRecords(null);
      historicalRecordMessage.setStatus("QUERY_TIMEOUT");
		  
		}
		
		
		String outputJson;
		try {
			outputJson = getObjectAsJsonString(historicalRecordMessage);
		} catch (IOException e1) {
			throw new WebApplicationException(e1, Response.Status.INTERNAL_SERVER_ERROR);
		}
		
		Response response = Response.ok(outputJson, "application/json").build();
		
		log.info("Returning response from getHistoricalRecords");
		return response;
	}
	
	private Map<CcAndInferredLocationFilter, Object> addFilterParameters(String depotId, 
			String inferredRouteId, String inferredPhase, Integer vehicleId, String vehicleAgencyId,
			String boundingBox, String startDate, String endDate, Integer records, Integer timeout) {
		
		Map<CcAndInferredLocationFilter, Object> filter = 
				new HashMap<CcAndInferredLocationFilter, Object>();
		boolean nonEmptyFilter = false;
		nonEmptyFilter |= addToFilter(filter, CcAndInferredLocationFilter.DEPOT_ID, depotId);
		nonEmptyFilter |= addToFilter(filter, CcAndInferredLocationFilter.INFERRED_ROUTEID, inferredRouteId);
		nonEmptyFilter |= addToFilter(filter, CcAndInferredLocationFilter.INFERRED_PHASE, inferredPhase);
		nonEmptyFilter |= addToFilter(filter, CcAndInferredLocationFilter.BOUNDING_BOX, boundingBox);
		nonEmptyFilter |= addToFilter(filter, CcAndInferredLocationFilter.VEHICLE_ID, vehicleId);
		nonEmptyFilter |= addToFilter(filter, CcAndInferredLocationFilter.VEHICLE_AGENCY_ID, vehicleAgencyId);
		nonEmptyFilter |= addToFilter(filter, CcAndInferredLocationFilter.START_DATE, startDate);
		nonEmptyFilter |= addToFilter(filter, CcAndInferredLocationFilter.END_DATE, endDate);
		nonEmptyFilter |= addToFilter(filter, CcAndInferredLocationFilter.TIMEOUT, timeout);
		if (!nonEmptyFilter) {
		  log.debug("empty filter");
		}
		filter.put(CcAndInferredLocationFilter.RECORDS, records);
		
		return filter;
	}
	
  private boolean addToFilter(Map<CcAndInferredLocationFilter, Object> filter, CcAndInferredLocationFilter key, Object value) {
	  if (key == null || value == null) return false;
	  filter.put(key, value);
	  return true;
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
	 * @param historicalRecordsDao the historicalRecordsDao to set
	 */
	@Autowired
	public void setHistoricalRecordsDao(HistoricalRecordsDao historicalRecordsDao) {
		this.historicalRecordsDao = historicalRecordsDao;
	}
}
