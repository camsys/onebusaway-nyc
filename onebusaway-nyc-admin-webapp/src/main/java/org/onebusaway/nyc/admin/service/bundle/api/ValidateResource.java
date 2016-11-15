package org.onebusaway.nyc.admin.service.bundle.api;

import org.onebusaway.nyc.admin.model.BundleRequest;
import org.onebusaway.nyc.admin.model.BundleResponse;
import org.onebusaway.nyc.admin.service.BundleRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

import java.io.StringWriter;

import javax.annotation.PostConstruct;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/validate")
@Component
@Scope("singleton")
/**
 * Validation endpoint. 
 *
 */
public class ValidateResource extends AuthenticatedResource {
  
	private static Logger _log = LoggerFactory.getLogger(ValidateResource.class);
  private final ObjectMapper _mapper = new ObjectMapper();

  @Autowired
  private BundleRequestService _bundleService;
  
  @PostConstruct
  public void setup(){
	  _mapper.registerModule(new AfterburnerModule());
  }

  @Path("/{bundleDirectory}/{bundleName}/create")
  @GET
  @Produces("application/json")
  public Response validate(@PathParam("bundleDirectory") String bundleDirectory,
      @PathParam("bundleName") String bundleName) {
    Response response = null;
    if (!isAuthorized()) {
      return Response.noContent().build();
    }

    BundleRequest bundleRequest = new BundleRequest();
    bundleRequest.setBundleBuildName(bundleName);
    bundleRequest.setBundleDirectory(bundleDirectory);
    BundleResponse bundleResponse = null;
    try {
      bundleResponse = _bundleService.validate(bundleRequest);
      final StringWriter sw = new StringWriter();
      final MappingJsonFactory jsonFactory = new MappingJsonFactory();
      final JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(sw);
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
    BundleResponse bundleResponse = null;
    try {
      bundleResponse = _bundleService.lookupValidationRequest(id);
      final StringWriter sw = new StringWriter();
      final MappingJsonFactory jsonFactory = new MappingJsonFactory();
      final JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(sw);
      _mapper.writeValue(jsonGenerator, bundleResponse);
      response = Response.ok(sw.toString()).build();
    } catch (Exception any) {
      response = Response.serverError().build();
    }
    return response;
  }  
}
