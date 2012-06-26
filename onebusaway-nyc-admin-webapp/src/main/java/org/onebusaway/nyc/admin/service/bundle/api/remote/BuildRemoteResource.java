package org.onebusaway.nyc.admin.service.bundle.api.remote;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.onebusaway.nyc.admin.model.BundleBuildRequest;
import org.onebusaway.nyc.admin.model.BundleBuildResponse;
import org.onebusaway.nyc.admin.service.bundle.BundleBuildingService;
import org.onebusaway.nyc.admin.service.bundle.api.AuthenticatedResource;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/build/remote")
@Component
@Scope("singleton")
/**
 * Build endpoint on remote server. 
 *
 */
public class BuildRemoteResource extends AuthenticatedResource {

  private final ObjectMapper _mapper = new ObjectMapper();

	private static Logger _log = LoggerFactory.getLogger(BuildRemoteResource.class);
  private Map<String, BundleBuildResponse> _buildMap = new HashMap<String, BundleBuildResponse>();
  private ExecutorService _executorService = null;
	
  @Autowired
  private BundleBuildingService _bundleService;
  
  @PostConstruct
  public void setup() {
        _executorService = Executors.newFixedThreadPool(1);
  }

  @Path("/{bundleDirectory}/{bundleName}/{email}/{id}/{bundleStartDate}/{bundleEndDate}/create")
  @GET
  @Produces("application/json")
  public Response build(@PathParam("bundleDirectory") String bundleDirectory,
      @PathParam("bundleName") String bundleName,
      @PathParam("email") String email,
      @PathParam("id") String id,
      @PathParam("bundleStartDate") String bundleStartDate,
      @PathParam("bundleEndDate") String bundleEndDate) {
    Response response = null;
    if (!isAuthorized()) {
      return Response.noContent().build();
    }
    _log.info("in build(local)");

    validateDates(bundleStartDate, bundleEndDate);
    
    BundleBuildRequest bundleRequest = new BundleBuildRequest();
    bundleRequest.setBundleDirectory(bundleDirectory);
    bundleRequest.setBundleName(bundleName);
    bundleRequest.setBundleDirectory(bundleDirectory);
    bundleRequest.setEmailAddress(email);
    bundleRequest.setId(id);
    bundleRequest.setBundleStartDate(bundleStartDate);
    bundleRequest.setBundleEndDate(bundleEndDate);
    
    BundleBuildResponse bundleResponse = new BundleBuildResponse(id);
    
    try {
      bundleResponse.addStatusMessage("server started");
      bundleResponse.addStatusMessage("queueing");
      // place execution in its own thread
      _executorService.execute(new BuildThread(bundleRequest, bundleResponse));
      // place handle to response in map
      _buildMap.put(id, bundleResponse);
      final StringWriter sw = new StringWriter();
      final MappingJsonFactory jsonFactory = new MappingJsonFactory();
      final JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(sw);
      // write back response
      _log.debug("returning id=" + bundleResponse.getId() + " for bundleResponse=" + bundleResponse);
      _mapper.writeValue(jsonGenerator, bundleResponse);
      response = Response.ok(sw.toString()).build();
    } catch (Exception any) {
      _log.error("execption in build:", any);
      response = Response.serverError().build();
    }
    
    return response;
  }

  @Path("/{id}/list")
  @GET
  @Produces("application/json")
  public Response list(@PathParam("id") String id) {
    Response response = null;
    if (!isAuthorized()) {
      return Response.noContent().build();
    }

    try {
      final StringWriter sw = new StringWriter();
      final MappingJsonFactory jsonFactory = new MappingJsonFactory();
      final JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(sw);
      _mapper.writeValue(jsonGenerator, _buildMap.get(id));
      response = Response.ok(sw.toString()).build();
    } catch (Exception any) {
      response = Response.serverError().build();
    }
    return response;
  }
  

  // TODO
  @Override
  protected boolean isAuthorized() {
    return true;
  }
  
  /**
	 * Performs various validations on start and end dates
	 * @param bundleStartDateString
	 * @param bundleEndDateString
	 */
	protected void validateDates(String bundleStartDateString, String bundleEndDateString) {
		if(StringUtils.isBlank(bundleStartDateString)) {
			throw new RuntimeException("Bundle start date cannot be empty");
		}
		if(StringUtils.isBlank(bundleEndDateString)) {
			throw new RuntimeException("Bundle end date cannot be empty");
		}
		
		DateTime startDate = ISODateTimeFormat.date().parseDateTime(bundleStartDateString);
		DateTime endDate = ISODateTimeFormat.date().parseDateTime(bundleEndDateString);
		
		if(startDate.isAfter(endDate)) {
			throw new RuntimeException("Start date cannot be after end date");
		}
	}

    private class BuildThread implements Runnable {
      
      private BundleBuildRequest _request;
      private BundleBuildResponse _response;
  
      public BuildThread(BundleBuildRequest request, BundleBuildResponse response) {
        _request = request;
        _response = response;
      }
  
      @Override
      public void run() {
        try {
        _response.addStatusMessage("in run queue");
        _bundleService.doBuild(_request, _response);
        } finally {
          _response.setComplete(true);
        }
      }
    }

}
