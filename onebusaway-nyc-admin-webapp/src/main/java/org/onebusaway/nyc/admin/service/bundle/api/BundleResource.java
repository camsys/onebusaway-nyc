package org.onebusaway.nyc.admin.service.bundle.api;

import org.onebusaway.nyc.admin.service.RemoteConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("/bundle")
@Component
public class BundleResource implements ServletContextAware {

  private static final String DEFAULT_TDM_URL = "http://tdm";
  private static Logger _log = LoggerFactory.getLogger(BundleResource.class);
  @Autowired
  private RemoteConnectionService _remoteConnectionService;

  /*
   * override of default TDM location:  for local testing use 
   * http://localhost:8080/onebusaway-nyc-tdm-webapp
   * This should be set in context.xml
   */
  private String tdmURL;
  @Path("/deploy/list/{environment}")
  @GET
  /**
   * list the bundle(s) that are on S3, potentials to be deployed.
   */
  public Response list(@PathParam("environment") String environment) {
    try {
    String url = getTDMURL() + "/api/bundle/deploy/list/" + environment;
    _log.debug("requesting:" + url);
    String json = _remoteConnectionService.getContent(url);
    _log.debug("response:" + json);
    return Response.ok(json).build();
    } catch (Exception e) {
      return Response.serverError().build();
    }
  }  
  
  @Path("/deploy/from/{environment}")
  @GET
  /**
   * request bundles at s3://obanyc-bundle-data/activebundes/{environment} be deployed
   * on the TDM (and hence the rest of the environment)
   * @param environment string representing environment (dev/staging/prod/qa)
   * @return status object with id for querying status
   */
  public Response deploy(@PathParam("environment") String environment) {
    String url = getTDMURL() + "/api/bundle/deploy/from/" + environment;
    _log.debug("requesting:" + url);
    String json = _remoteConnectionService.getContent(url);
    _log.debug("response:" + json);
    return Response.ok(json).build();
  }
  
  private String getTDMURL() {
    if (tdmURL != null && tdmURL.length() > 0) {
      return tdmURL;
    }
    return DEFAULT_TDM_URL;
  }

  @Path("/deploy/status/{id}/list")
  @GET
  /**
   * query the status of a requested bundle deployment
   * @param id the id of a BundleDeploymentStatus
   * @return a serialized version of the requested BundleDeploymentStatus, null otherwise
   */
  public Response deployStatus(@PathParam("id") String id) {
    try {
      String url = getTDMURL() + "/api/bundle/deploy/status/" + id + "/list";
      _log.debug("requesting:" + url);
      String json = _remoteConnectionService.getContent(url);
      _log.debug("response:" + json);
      return Response.ok(json).build();
    } catch (Exception e) {
      return Response.serverError().build();
    }
  }

  @Override
  public void setServletContext(ServletContext context) {
      if (context != null) {
        String url = context.getInitParameter("tdm.host");
        if (url != null && url.length() > 0) {
          tdmURL = url;
          _log.error("tdmURL="+ tdmURL);
        }
      }
  }
}
