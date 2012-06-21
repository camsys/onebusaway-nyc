package org.onebusaway.nyc.admin.service.bundle.api;

import org.onebusaway.nyc.admin.model.BundleBuildRequest;
import org.onebusaway.nyc.admin.model.BundleBuildResponse;
import org.onebusaway.nyc.admin.service.BundleRequestService;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.StringWriter;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

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
    
    BundleBuildRequest buildRequest = new BundleBuildRequest();
    buildRequest.setBundleDirectory(bundleDirectory);
    buildRequest.setBundleName(bundleName);
    buildRequest.setEmailAddress(email);
    buildRequest.setBundleStartDate(bundleStartDate);
    buildRequest.setBundleEndDate(bundleEndDate);
    
    BundleBuildResponse buildResponse = null;
    
    try {
      buildResponse =_bundleService.build(buildRequest);
      buildResponse = _bundleService.buildBundleResultURL(buildResponse.getId());
      final StringWriter sw = new StringWriter();
      final MappingJsonFactory jsonFactory = new MappingJsonFactory();
      final JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(sw);
      _mapper.writeValue(jsonGenerator, buildResponse);
      response = Response.ok(sw.toString()).build();
    } catch (Exception any) {
      _log.error("exception in build:", any);
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
    BundleBuildResponse buildResponse = _bundleService.lookupBuildRequest(id);
    try {
      final StringWriter sw = new StringWriter();
      final MappingJsonFactory jsonFactory = new MappingJsonFactory();
      final JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(sw);
      _mapper.writeValue(jsonGenerator, buildResponse);
      _log.info("received response=" + buildResponse);
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
      final StringWriter sw = new StringWriter();
      final MappingJsonFactory jsonFactory = new MappingJsonFactory();
      final JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(sw);
      _mapper.writeValue(jsonGenerator, buildResponse);
      response = Response.ok(sw.toString()).build();
    } catch (Exception any){
      _log.error("exception looking up build:", any);
      response = Response.serverError().build();
    }
    return response;
    }
}
