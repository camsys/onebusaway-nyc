package org.onebusaway.nyc.report_archive.api;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.onebusaway.nyc.report_archive.api.json.JsonTool;
import org.onebusaway.nyc.report_archive.api.json.LastKnownRecordsMessage;
import org.onebusaway.nyc.report_archive.model.ArchivedInferredLocationRecord;
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

	@Path("/list")
	@GET
	@Produces("application/json")
	public Response getAllLastLocationRecords() {

		List<CcAndInferredLocationRecord> lastKnownRecords = getLastKnownRecordsFromDao();
		
		LastKnownRecordsMessage message = new LastKnownRecordsMessage();
		message.setRecords(lastKnownRecords);
		message.setStatus("OK");

		String outputJson = null;

		try {
			StringWriter writer = new StringWriter();

			_jsonTool.writeJson(writer, message);

			outputJson = writer.toString();

			writer.close();

			if (outputJson == null)
				throw new IOException(
						"After calling writeJson, outputJson is still null.");
		} catch (IOException e) {
			throw new WebApplicationException(e,
					Response.Status.INTERNAL_SERVER_ERROR);
		}

		Response response = Response.ok(outputJson, "application/json").build();

		return response;
	}
	
	private List<CcAndInferredLocationRecord> getLastKnownRecordsFromDao() {
		List<CcAndInferredLocationRecord> lastKnownRecords = _locationDao
				.getAllLastKnownRecords();

		return lastKnownRecords;
	}
}
