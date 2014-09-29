package org.onebusaway.nyc.admin.service.bundle.api;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.onebusaway.nyc.admin.service.BundleDeployerService;
import org.onebusaway.nyc.admin.service.BundleStagerService;
import org.onebusaway.nyc.admin.service.RemoteConnectionService;
import org.onebusaway.nyc.admin.service.bundle.BundleStager;
import org.onebusaway.nyc.transit_data_manager.bundle.model.BundleStatus;
import org.onebusaway.nyc.util.configuration.ConfigurationServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

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
  private BundleStagerService _localBundleStager;
  @Autowired
  @Qualifier("localBundleDeployerImpl")
  private BundleDeployerService _localBundleDeployer;
  @Autowired
  @Qualifier("tdmRemoteBundleDeployerImpl")
  private BundleDeployerService _tdmBundleDeployer;
  
  private String tdmURL;

  private Boolean isTdm = null;
  
  
  @Path("/stagerequest/{environment}/{bundleDir}/{bundleName}")
  @GET
  /**
   * request just-built bundle is staged for deployment
   * @return status object with id for querying status
   */
  public Response stage(@PathParam("environment")
    String environment, @PathParam("bundleDir")
    String bundleDir, @PathParam("bundleName")
    String bundleName) {
      // TODO this should follow the deployer pattern with an async response
      // object
      
    return _localBundleStager.stage(environment, bundleDir, bundleName);
    
    /*String json = "{ERROR}";
      try {
        _bundleStager.stage(environment, bundleDir, bundleName);
        _bundleStager.notifyOTP(bundleName);
        json = "{SUCCESS}";
      } catch (Exception any) {
        _log.error("stage failed:", any);
      }
      return Response.ok(json).build();*/
  }
  
  @Path("/stage/status/{id}/list")
  @GET
  public Response stageStatus(@PathParam("id")
  String id) {  
    return _localBundleStager.stageStatus(id);
  }
  
  @Path("/staged/list")
  @GET
  public Response getStagedBundleList() {
    return _localBundleStager.getBundleList();

  }

  @Path("/staged/file/{bundleFilename: [a-zA-Z0-9_./]+}/get")
  @GET
  public Response getStagedFile(@PathParam("bundleId") String bundleId,
      @PathParam("bundleFileFilename") String relativeFilename) {

    return _localBundleStager.getBundleFile(bundleId, relativeFilename);

  }
  

  @Path("/deploy/list/{environment}")
  @GET
  public Response listStagedBundles(@PathParam("environment")
  String environment) {
      if (isTdm()) {
        return _tdmBundleDeployer.listStagedBundles(environment);
      } 
      
      return _localBundleDeployer.listStagedBundles(environment); 
  }

  @Path("/deploy/from/{environment}")
  @GET
  public Response deploy(@PathParam("environment")
  String environment) {
    if(isTdm()){
      return _tdmBundleDeployer.deploy(environment);
    }
    
    return _localBundleDeployer.deploy(environment);
  }
  
  @Path("/deploy/status/{id}/list")
  @GET
  public Response deployStatus(@PathParam("id")
  String id) {
    if (isTdm()) {
      return _tdmBundleDeployer.deployStatus(id);
    }
    
    return _localBundleDeployer.deployStatus(id);
  }
  

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
