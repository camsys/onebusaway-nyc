package org.onebusaway.nyc.admin.service.bundle.api.remote;

import org.onebusaway.nyc.admin.model.BundleRequest;
import org.onebusaway.nyc.admin.model.BundleResponse;
import org.onebusaway.nyc.admin.service.bundle.BundleValidationService;
import org.onebusaway.nyc.admin.service.bundle.api.AuthenticatedResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

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

@Path("/validate/remote")
@Component
@Scope("singleton")
/**
 * Validation endpoint on remote server. 
 *
 */
public class ValidateRemoteResource extends AuthenticatedResource {

	private static Logger _log = LoggerFactory.getLogger(ValidateRemoteResource.class);
  private final ObjectMapper _mapper = new ObjectMapper();
  private Map<String, BundleResponse> _validationMap = new HashMap<String, BundleResponse>();
  private ExecutorService _executorService = null;

  @Autowired
  private BundleValidationService _validateService;

  @PostConstruct
  public void setup() {
        _executorService = Executors.newFixedThreadPool(1);
  }

  @Path("/{bundleDirectory}/{bundleName}/{id}/create")
  @GET
  @Produces("application/json")
  public Response validate(@PathParam("bundleDirectory") String bundleDirectory,
      @PathParam("bundleName") String bundleName,
      @PathParam("id") String id) {
    Response response = null;
    if (!isAuthorized()) {
      return Response.noContent().build();
    }

    BundleRequest bundleRequest = new BundleRequest();
    bundleRequest.setBundleBuildName(bundleName);
    bundleRequest.setBundleDirectory(bundleDirectory);
    bundleRequest.setId(id);
    BundleResponse bundleResponse = new BundleResponse(id);
    
    try {
      bundleResponse.addStatusMessage("server started");
      bundleResponse.addStatusMessage("queueing");
      // place execution in its own thread
      _executorService.execute(new ValidateThread(bundleRequest, bundleResponse));
      // place handle to response in map
      _validationMap.put(id, bundleResponse);
      
      final StringWriter sw = new StringWriter();
      final MappingJsonFactory jsonFactory = new MappingJsonFactory();
      final JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(sw);
      // write back response
      _log.debug("returning id=" + bundleResponse.getId() + " for bundleResponse=" + bundleResponse);
      _mapper.writeValue(jsonGenerator, bundleResponse);
      response = Response.ok(sw.toString()).build();
    } catch (Exception any) {
      _log.error("validate resource caught exception:" + any);
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
      
      BundleResponse bundleResponse = _validationMap.get(id);
      _log.debug("found bundleResponse=" + bundleResponse + " for id=" + id);
      _mapper.writeValue(jsonGenerator, bundleResponse);
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
  
    private class ValidateThread implements Runnable {
      
      private BundleRequest _request;
      private BundleResponse _response;
  
      public ValidateThread(BundleRequest request, BundleResponse response) {
        _request = request;
        _response = response;
      }
  
      @Override
      public void run() {
        try {
          _response.addStatusMessage("in run queue");
         _validateService.downloadAndValidate(_request, _response);
        } catch (Exception any) {
          _log.error("run failed:", any);
          _response.setException(any);
        } finally {
         _response.setComplete(true);
        }
      }
    }
}
