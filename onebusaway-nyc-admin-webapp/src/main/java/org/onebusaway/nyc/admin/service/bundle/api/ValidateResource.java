/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onebusaway.nyc.admin.service.bundle.api;

import org.onebusaway.nyc.admin.model.BundleRequest;
import org.onebusaway.nyc.admin.model.BundleResponse;
import org.onebusaway.nyc.admin.service.BundleRequestService;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.*;

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

  @Path("/{id}/getValidationResults")
  @GET
  public String getValidationResults(@PathParam("id") String id) {
    Response response = null;
    if (!isAuthorized()) {
      return "Not Authorized";
    }
    try {
      BundleResponse bundleResponse = null;
      bundleResponse = _bundleService.lookupValidationRequest(id);
      String validationWarnings = "";
      for(String fileName : bundleResponse.getValidationFiles()){
        if(fileName.contains("filtered")){
          InputStream in = new FileInputStream(bundleResponse.getTmpDirectory()+ "/" +fileName);
          BufferedReader reader = new BufferedReader(new InputStreamReader(in));
          String line = null;
          boolean prevLineIndicatesImport = false;
          boolean firstIssueToReportInFile = true;
          while ((line = reader.readLine()) != null) {
            if(line.contains("###")){
              prevLineIndicatesImport = true;
            } else{
              if (prevLineIndicatesImport){
                if(firstIssueToReportInFile){
                  validationWarnings +="\n" + fileName;
                }
                validationWarnings +="\n" + line;
              }
            }
          }
        }
      }

      if(validationWarnings == ""){
        validationWarnings = "No Critical issues";
      }
      return validationWarnings;
    }catch (FileNotFoundException exception){
      _log.error(exception.toString());
      return exception.toString();
    } catch (IOException e) {
      _log.error(e.toString());
      return e.toString();
    }
  }
}
