package org.onebusaway.nyc.admin.service.bundle.api;

import java.io.IOException;
import java.io.StringWriter;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.onebusaway.nyc.admin.model.BundleBuildRequest;
import org.onebusaway.nyc.admin.model.BundleBuildResponse;
import org.onebusaway.nyc.admin.service.BundleRequestService;
import org.onebusaway.nyc.admin.service.exceptions.DateValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/build/")
@Component
@Scope("singleton")
public class BuildResource extends AuthenticatedResource {
	@Autowired
	private BundleRequestService _bundleService;
	private final ObjectMapper _mapper = new ObjectMapper();
	private static Logger _log = LoggerFactory.getLogger(BuildResource.class);

	@Path("/{bundleDirectory}/{bundleName}/{email}/{bundleStartDate}/{bundleEndDate}/create")
	@GET
	@Produces("application/json")
	public Response build(@PathParam("bundleDirectory") String bundleDirectory,
			@PathParam("bundleName") String bundleName,
			@PathParam("email") String email,
			@PathParam("bundleStartDate") String bundleStartDate,
			@PathParam("bundleEndDate") String bundleEndDate) {
		Response response = null;
		if (!isAuthorized()) {
			return Response.noContent().build();
		}
		
		BundleBuildResponse buildResponse = null;

		try {
			validateDates(bundleStartDate, bundleEndDate);
		} catch(DateValidationException e) {
			try {
				buildResponse = new BundleBuildResponse();
				buildResponse.setException(e);
				response = constructResponse(buildResponse);
			} catch (Exception any) {
				_log.error("exception in build:", any);
				response = Response.serverError().build();
			}
		}

		//Proceed only if date validation passes
		if(response == null) {
			BundleBuildRequest buildRequest = new BundleBuildRequest();
			buildRequest.setBundleDirectory(bundleDirectory);
			buildRequest.setBundleName(bundleName);
			buildRequest.setEmailAddress(email);
			buildRequest.setBundleStartDate(bundleStartDate);
			buildRequest.setBundleEndDate(bundleEndDate);
			
			
			try {
				buildResponse =_bundleService.build(buildRequest);
				buildResponse = _bundleService.buildBundleResultURL(buildResponse.getId());
				response = constructResponse(buildResponse);
			} catch (Exception any) {
				_log.error("exception in build:", any);
				response = Response.serverError().build();
			}
		}

		return response;
	}

	private Response constructResponse(BundleBuildResponse buildResponse)
			throws IOException, JsonGenerationException, JsonMappingException {
		final StringWriter sw = new StringWriter();
		final MappingJsonFactory jsonFactory = new MappingJsonFactory();
		final JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(sw);
		_mapper.writeValue(jsonGenerator, buildResponse);
		return Response.ok(sw.toString()).build();
	}

	/**
	 * Performs various validations on start and end dates
	 * @param bundleStartDateString
	 * @param bundleEndDateString
	 */
	protected void validateDates(String bundleStartDateString, String bundleEndDateString) throws DateValidationException {
		if(StringUtils.isBlank(bundleStartDateString)) {
			throw new DateValidationException(bundleStartDateString, bundleEndDateString);
		}
		if(StringUtils.isBlank(bundleEndDateString)) {
			throw new DateValidationException(bundleStartDateString, bundleEndDateString);
		}
		
		DateTime startDate = ISODateTimeFormat.date().parseDateTime(bundleStartDateString);
		DateTime endDate = ISODateTimeFormat.date().parseDateTime(bundleEndDateString);
		
		if(startDate.isAfter(endDate)) {
			throw new DateValidationException(bundleStartDateString, bundleEndDateString);
		}
	}

	@Path("/{id}/list")
	@GET
	@Produces("application/json")
	public Response list(@PathParam("id") String id) {
		Response response = null;
		if (!isAuthorized()) {
			return Response.noContent().build();
		}
		BundleBuildResponse buildResponse = _bundleService.lookupBuildRequest(id);
		try {
			final StringWriter sw = new StringWriter();
			final MappingJsonFactory jsonFactory = new MappingJsonFactory();
			final JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(sw);
			_mapper.writeValue(jsonGenerator, buildResponse);
			_log.debug("received response=" + buildResponse);
			response = Response.ok(sw.toString()).build();
		} catch (Exception any){
			_log.error("exception looking up build:", any);
			response = Response.serverError().build();
		}
		return response;
	}

	@Path("/{id}/url")
	@GET
	@Produces("application/json")
	public Response url(@PathParam("id") String id) {
		Response response = null;
		if (!isAuthorized()) {
			return Response.noContent().build();
		}
		BundleBuildResponse buildResponse = _bundleService.lookupBuildRequest(id);
		try {
			constructResponse(buildResponse);
		} catch (Exception any){
			_log.error("exception looking up build:", any);
			response = Response.serverError().build();
		}
		return response;
	}
}
