package org.onebusaway.nyc.admin.service.bundle.api;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.onebusaway.nyc.admin.service.BundleDeployerService;
import org.onebusaway.nyc.admin.service.RemoteConnectionService;
import org.onebusaway.nyc.admin.service.bundle.BundleDeployer;
import org.onebusaway.nyc.admin.service.impl.RemoteConnectionServiceLocalImpl;
import org.onebusaway.nyc.transit_data_manager.bundle.AwsBundleDeployer;
import org.onebusaway.nyc.transit_data_manager.bundle.BundleProvider;
import org.onebusaway.nyc.transit_data_manager.bundle.BundlesListMessage;
import org.onebusaway.nyc.transit_data_manager.bundle.StagingBundleProvider;
import org.onebusaway.nyc.transit_data_manager.bundle.model.Bundle;
import org.onebusaway.nyc.transit_data_manager.bundle.model.BundleDeployStatus;
import org.onebusaway.nyc.transit_data_manager.bundle.model.BundleMetadata;
import org.onebusaway.nyc.transit_data_manager.json.JsonTool;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.util.configuration.ConfigurationServiceClient;
import org.onebusaway.transit_data_federation.model.bundle.BundleItem;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

import com.sun.jersey.core.header.ContentDisposition;

@Path("/bundle")
@Component
public class BundleResource implements ServletContextAware{
  
  private static final String DEFAULT_TDM_URL = "http://tdm";
  
  private static Logger _log = LoggerFactory.getLogger(BundleResource.class);
  @Autowired
  private ConfigurationServiceClient _configClient;
  @Autowired
  RemoteConnectionService _remoteConnectionService;
  @Autowired
  private StagingBundleProvider _stagingBundleProvider;
  @Autowired
  @Qualifier("localBundleDeployerImpl")
  private BundleDeployerService _localBundleDeployer;
  @Autowired
  @Qualifier("tdmRemoteBundleDeployerImpl")
  private BundleDeployerService _tdmBundleDeployer;
  
  private Map<String, BundleDeployStatus> _deployMap = new HashMap<String, BundleDeployStatus>();
  
  private String tdmURL;

  private Boolean isTdm = null;
  
  public BundleDeployStatus lookupDeployRequest(String id) {
    return _deployMap.get(id);
  }
  
  
  @Path("/stagerequest/{bundleDir}/{bundleName}")
  @GET
  /**
   * request just-built bundle is staged for deployment
   * @return status object with id for querying status
   */
  public Response stage(@PathParam("bundleDir")
    String bundleDir, @PathParam("bundleName")
    String bundleName) {
      // TODO this should follow the deployer pattern with an async response
      // object
      String json = "{ERROR}";
      try {
        String stagingDir = _configClient.getItem("admin", "bundleStagingDir");
        _stagingBundleProvider.stage(stagingDir, bundleDir, bundleName);
        notifyOTP();
        json = "{SUCCESS}";
      } catch (Exception any) {
        _log.error("stage failed:", any);
      }
      return Response.ok(json).build();
  }

  @Path("/deploy/list/{environment}")
  @GET
  /**
   * list the bundle(s) that are on S3, potentials to be deployed.
   */
  public Response list(@PathParam("environment")
  String environment) {
      if (isTdm()) {
        return _tdmBundleDeployer.list(environment);
      } 
      
      return _localBundleDeployer.list(environment); 
  }

  @Path("/deploy/from/{environment}")
  @GET
  /**
   * request bundles at s3://obanyc-bundle-data/activebundes/{environment} be deployed
   * on the TDM (and hence the rest of the environment)
   * @param environment string representing environment (dev/staging/prod/qa)
   * @return status object with id for querying status
   */
  public Response deploy(@PathParam("environment")
  String environment) {
    if(isTdm()){
      return _tdmBundleDeployer.deploy(environment);
    }
    
    return _localBundleDeployer.deploy(environment);
  }
  
  @Path("/deploy/status/{id}/list")
  @GET
  /**
   * query the status of a requested bundle deployment
   * @param id the id of a BundleDeploymentStatus
   * @return a serialized version of the requested BundleDeploymentStatus, null otherwise
   */
  public Response deployStatus(@PathParam("id")
  String id) {
    if (isTdm()) {
      return _tdmBundleDeployer.deployStatus(id);
    }
    
    return _localBundleDeployer.deployStatus(id);
  }
  
  /*
   * methods exclusive to none TDM  
   */
  
  @Path("/list")
  @GET
  public Response getBundleList() {
    
    if (isTdm()) {
      return _tdmBundleDeployer.getBundleList();
    }
    
    return _localBundleDeployer.getBundleList();
  }
  
  
  @Path("/deploy/{bundleId}/file/{bundleFileFilename: [a-zA-Z0-9_./]+}/get")
  @GET
  public Response getBundleFile(@PathParam("bundleId") String bundleId,
      @PathParam("bundleFileFilename") String relativeFilename) {
    
    if (isTdm()) {
      return _tdmBundleDeployer.getBundleFile(bundleId, relativeFilename);
    }

    return _localBundleDeployer.getBundleFile(bundleId, relativeFilename);
  }
  
  
  /*
   * Private Methods
   */

  private void notifyOTP() throws Exception {
    String otpNotificationUrl = _configClient.getItem("admin", "otpNotificationUrl");
    if (otpNotificationUrl == null) return;
    BundleMetadata meta = getStagedBundleMetadata();
    otpNotificationUrl = otpNotificationUrl.replaceAll(":uuid", (meta==null?"":meta.getId()));
    _remoteConnectionService.getContent(otpNotificationUrl);
  }

  private String getTDMURL() {
    if (tdmURL != null && tdmURL.length() > 0) {
      return tdmURL;
    }
    return DEFAULT_TDM_URL;
  }
  

  private boolean isTdm() {
    if (isTdm != null)
      return isTdm;
    try {
      String useTdm = _configClient.getItem("admin", "useTdm");
      isTdm = "true".equalsIgnoreCase(useTdm);
    } catch (Exception e) {
      _log.error("isTdm caugh e:", e);
    }
    return isTdm;
  }

  private BundleMetadata getStagedBundleMetadata() throws Exception {
    String bundleStagingProp = null;
    try {
      bundleStagingProp = _configClient.getItem("admin", "bundleStagingDir");
    } catch (Exception e) {
      _log.error("error looking up bundleStagingDir:", e);
    }
    if (bundleStagingProp == null) {
      _log.error("expecting bundleStagingDir property from config, Failing");
      return null;
    }
    File stagingDirectory = new File(bundleStagingProp);
    if (!stagingDirectory.exists() || !stagingDirectory.isDirectory()) {
      _log.error("expecting bundleStagingDir directory to exist: " + stagingDirectory);
      return null;
    }
    
    return _stagingBundleProvider.getMetadata(stagingDirectory.toString());
  }
  


  @Override
  public void setServletContext(ServletContext context) {
    if (context != null) {
      String url = context.getInitParameter("tdm.host");
      if (url != null && url.length() > 0) {
        tdmURL = url;
        _log.debug("tdmURL=" + tdmURL);
      }
    }
  }
}
