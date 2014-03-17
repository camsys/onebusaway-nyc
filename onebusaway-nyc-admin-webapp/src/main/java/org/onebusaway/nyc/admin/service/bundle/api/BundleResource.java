package org.onebusaway.nyc.admin.service.bundle.api;


import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.codehaus.jackson.map.ObjectMapper;
import org.onebusaway.nyc.admin.service.RemoteConnectionService;
import org.onebusaway.nyc.admin.service.impl.RemoteConnectionServiceLocalImpl;
import org.onebusaway.nyc.transit_data_manager.bundle.BundleProvider;
import org.onebusaway.nyc.transit_data_manager.bundle.model.BundleMetadata;
import org.onebusaway.nyc.util.configuration.ConfigurationServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

import javax.annotation.PostConstruct;
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
  private ConfigurationServiceClient _configClient;
  @Autowired
  private RemoteConnectionService _remoteConnectionService;
  @Autowired
  private BundleProvider _bundleProvider;
  
  private RemoteConnectionServiceLocalImpl _localConnectionService;
  private ObjectMapper _mapper = new ObjectMapper();
  private ObjectMapper _dateMapper = new ObjectMapper();
  /*
   * override of default TDM location:  for local testing use 
   * http://localhost:8080/onebusaway-nyc-tdm-webapp
   * This should be set in context.xml
   */
  private String tdmURL;
  
  private Boolean isTdm = null;
  
  @PostConstruct
  public void setup() {
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    _dateMapper.setDateFormat(df);
  }
  
  @Path("/deploy/list/{environment}")
  @GET
  /**
   * list the bundle(s) that are on S3, potentials to be deployed.
   */
  public Response list(@PathParam("environment") String environment) {
    try {
      _log.info("list with tdm url=" + getTDMURL() + " and isTdm()=" + isTdm());
      if (isTdm()) {
        String url = getTDMURL() + "/api/bundle/deploy/list/" + environment;
        _log.debug("requesting:" + url);
        String json = _remoteConnectionService.getContent(url);
        _log.debug("response:" + json);
        return Response.ok(json).build();
      } else {
        String json = _localConnectionService.getList(environment);
        if (json != null) {
          return Response.ok(json).build();
        }
        return Response.serverError().build();
      }
    } catch (Exception e) {
      _log.info("bundle list failed:", e);
      return Response.serverError().build();
    }
  }  
  
  private boolean isTdm() {
	  if (isTdm != null) return isTdm;
	  try {
		String useTdm = _configClient.getItem("admin", "useTdm");
		if ("false".equalsIgnoreCase(useTdm)) {
			_localConnectionService = new RemoteConnectionServiceLocalImpl();
			_localConnectionService.setConfigurationServiceClient(_configClient);
			_localConnectionService.setBundleProvider(_bundleProvider);
			isTdm = false;
		} else {
			isTdm = true;
		}
	} catch (Exception e) {
		_log.error("isTdm caugh e:", e);
	}
	  return isTdm;
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
  

@Path("/stagerequest/{bundleDir}/{bundleName}")
@GET
/**
 * request just-built bundle is staged for deployment
 * @return status object with id for querying status
 */
public Response stage(@PathParam("bundleDir") String bundleDir,
      @PathParam("bundleName") String bundleName) {
  // TODO this should follow the deployer pattern with an async response object
  String json = "{ERROR}";
  try {
    String stagingDir = _configClient.getItem("admin", "bundleStagingDir");
    this._bundleProvider.stage(stagingDir, bundleDir, bundleName);
    json = "{SUCCESS}";
  } catch (Exception any) {
    _log.error("stage failed:", any);
  }
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

  @Path("/staged")
  @GET
  public Response getStagedBundleMetadata() {
    
    if (isTdm()) {
      // not implemented
      _log.error("getStagedBundleMetadata not implemented");
      return Response.serverError().build();
    } else {
      String json = null;
      try {
        BundleMetadata meta = _localConnectionService.getStagedBundleMetadata();
        if (meta == null) {
          return Response.serverError().build();
        }

        json = _dateMapper.writeValueAsString(meta);
      } catch (Exception e) {
        _log.error("exception writing json:", e);
        return Response.serverError().build();
      }
      return Response.ok(json).build();
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
