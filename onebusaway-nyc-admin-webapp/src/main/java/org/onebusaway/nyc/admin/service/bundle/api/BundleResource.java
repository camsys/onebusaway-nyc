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

import org.onebusaway.nyc.admin.service.RemoteConnectionService;
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
import org.springframework.context.annotation.Scope;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

import com.sun.jersey.core.header.ContentDisposition;

@Path("/bundle")
@Component
public class BundleResource implements ServletContextAware{
  
  private static final String DEFAULT_TDM_URL = "http://tdm";
  private static final String DEFAULT_BUNDLE_STAGING_DIRECTORY = "activebundles";
  private static Logger _log = LoggerFactory.getLogger(BundleResource.class);
  @Autowired
  private ConfigurationServiceClient _configClient;
  @Autowired
  private RemoteConnectionService _remoteConnectionService;
  @Autowired
  private BundleProvider _bundleProvider;
  @Autowired
  private StagingBundleProvider _stagingBundleProvider;
  @Autowired
  private AwsBundleDeployer bundleDeployer;
  @Autowired
  private JsonTool jsonTool;
  
  private RemoteConnectionServiceLocalImpl _localConnectionService;
  
  private ExecutorService _executorService = null;
  private Map<String, BundleDeployStatus> _deployMap = new HashMap<String, BundleDeployStatus>();
  private Integer jobCounter = 0;
  //private RemoteConnectionServiceLocalImpl _localConnectionService;
  private ObjectMapper _dateMapper = new ObjectMapper();
  /*
   * override of default TDM location: for local testing use
   * http://localhost:8080/onebusaway-nyc-tdm-webapp This should be set in
   * context.xml
   */
  private String tdmURL;

  private Boolean isTdm = null;

  @PostConstruct
  public void setup() {
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    _dateMapper.setDateFormat(df);
    _executorService = Executors.newFixedThreadPool(1);
  }
  
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
    try {
      _log.info("list with tdm url=" + getTDMURL() + " and isTdm()=" + isTdm());
      if (isTdm()) {
        String url = getTDMURL() + "/api/bundle/deploy/list/" + environment;
        _log.debug("requesting:" + url);
        String json = _remoteConnectionService.getContent(url);
        _log.debug("response:" + json);
        return Response.ok(json).build();
      } else {
        String path = getBundleDirectory() + File.separator + environment + File.separator;
        List<String> list = bundleDeployer.listBundlesForServing(path);
        try {
          String jsonList = jsonSerializer(list);
          Response.ok(jsonList).build();
        } catch (Exception e) {
          _log.error("exception serializing response:", e);
        }
        return Response.serverError().build();
      }
    } catch (Exception e) {
      _log.error("bundle list failed:", e);
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
  public Response deploy(@PathParam("environment")
  String environment) {
    if(isTdm()){
      String url = getTDMURL() + "/api/bundle/deploy/from/" + environment;
      _log.debug("requesting:" + url);
      String json = _remoteConnectionService.getContent(url);
      _log.debug("response:" + json);
      return Response.ok(json).build();
    }
    else{
      _log.info("Starting deploy(" + environment + ")...");
      String s3Path = getBundleDirectory() + File.separator + environment + File.separator;
      BundleDeployStatus status = new BundleDeployStatus();
      status.setId(getNextId());
      _deployMap.put(status.getId(), status);
      _executorService.execute(new DeployThread(s3Path, status));
      _log.info("deploy request complete");
  
      try {
        String jsonStatus = jsonSerializer(status);
        Response.ok(jsonStatus).build();
      } catch (Exception e) {
        _log.error("exception serializing response:", e);
      }
      return Response.serverError().build();
      }
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
    else{
      BundleDeployStatus status = this.lookupDeployRequest(id);
      try {
        String jsonStatus = jsonSerializer(status);
        Response.ok(jsonStatus).build();
      } catch (Exception e) {
        _log.error("exception serializing response:", e);
      }
      return Response.serverError().build();
    } 
  }
  
  /*
   * methods exclusive to none TDM  
   */
  
  @Path("/list")
  @GET
  public Response getBundleList() {
    
    if (isTdm()) {
      // not implemented
      _log.error("getBundleList not implemented");
      return Response.serverError().build();
    } 

    _log.info("Starting getBundleList.");

    List<Bundle> bundles = _bundleProvider.getBundles();

    Response response;

    if (bundles != null) {
      BundlesListMessage bundlesMessage = new BundlesListMessage();
      bundlesMessage.setBundles(bundles);
      bundlesMessage.setStatus("OK");

      final BundlesListMessage bundlesMessageToWrite = bundlesMessage;

      StreamingOutput output = new StreamingOutput() {

        @Override
        public void write(OutputStream out) throws IOException,
            WebApplicationException {
          BufferedWriter writer = new BufferedWriter(
              new OutputStreamWriter(out));

          jsonTool.writeJson(writer, bundlesMessageToWrite);

          writer.close();
          out.close();

        }
      };
      response = Response.ok(output, "application/json").build();
    } else {
      response = Response.serverError().build();
    }

    _log.info("Returning Response in getBundleList.");
    return response;
  }
  
  @Path("/deploy/{bundleId}/file/{bundleFileFilename: [a-zA-Z0-9_./]+}/get")
  @GET
  public Response getBundleFile(@PathParam("bundleId") String bundleId,
      @PathParam("bundleFileFilename") String relativeFilename) {
    
    if (isTdm()) {
      // not implemented
      _log.error("getStagedFile not implemented");
      return Response.serverError().build();
    }

    _log.info("starting getBundleFile for relative filename " + relativeFilename + " in bundle " + bundleId);

    boolean requestIsForValidBundleFile = _bundleProvider.checkIsValidBundleFile(
        bundleId, relativeFilename);
    if (!requestIsForValidBundleFile) {
      throw new WebApplicationException(new IllegalArgumentException(
          relativeFilename + " is not listed in bundle metadata."),
          Response.Status.BAD_REQUEST);
    }

    final File requestedFile;
    try {
      requestedFile = _bundleProvider.getBundleFile(bundleId, relativeFilename);

    } catch (FileNotFoundException e) {
      _log.info("FileNotFoundException loading " + relativeFilename + " in "
          + bundleId + " bundle.");
      throw new WebApplicationException(e,
          Response.Status.INTERNAL_SERVER_ERROR);
    }

    long fileLength = requestedFile.length();

    StreamingOutput output = new StreamingOutput() {

      @Override
      public void write(OutputStream os) throws IOException,
          WebApplicationException {

        FileChannel inChannel = null;
        WritableByteChannel outChannel = null;

        try {
          inChannel = new FileInputStream(requestedFile).getChannel();
          outChannel = Channels.newChannel(os);

          inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
          if (outChannel != null)
            outChannel.close();
          if (inChannel != null)
            inChannel.close();
        }

      }
    };

    ContentDisposition cd = ContentDisposition.type("file").fileName(
        requestedFile.getName()).build();

    Response response = Response.ok(output, MediaType.APPLICATION_OCTET_STREAM).header(
        "Content-Disposition", cd).header("Content-Length", fileLength).build();

    _log.info("Returning Response in getBundleFile");

    return response;
  }
  
  
  /*
   * Private Methods
   */
  
  /**
   * Trivial implementation of creating unique Ids. Security is not a
   * requirement here.
   */
  private String getNextId() {
    return "" + inc();
  }

  private Integer inc() {
    synchronized (jobCounter) {
      jobCounter++;
    }
    return jobCounter;
  }

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
      if ("false".equalsIgnoreCase(useTdm)) {
        _localConnectionService = new RemoteConnectionServiceLocalImpl();
        _localConnectionService.setConfigurationServiceClient(_configClient);
        _localConnectionService.setBundleProvider(_bundleProvider);
        _localConnectionService.setStagingBundleProvider(_stagingBundleProvider);
        isTdm = false;
      } else {
        isTdm = true;
      }
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
  
  private String getBundleDirectory() {
    if (_configClient != null) {
      try {
        return _configClient.getItem("admin.bundle_directory", DEFAULT_BUNDLE_STAGING_DIRECTORY);
      } catch (RemoteConnectFailureException e){
        _log.error("default bundle dir lookup failed:", e);
        return DEFAULT_BUNDLE_STAGING_DIRECTORY;
      } catch (Exception e) {
        _log.error("stage failed:", e);
      }
    }
    return DEFAULT_BUNDLE_STAGING_DIRECTORY;
  }
  
  private String jsonSerializer(Object object) throws IOException{
    //serialize the status object and send to client -- it contains an id for querying
    final StringWriter sw = new StringWriter();
    final MappingJsonFactory jsonFactory = new MappingJsonFactory();
    final JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(sw);
    ObjectMapper mapper = new ObjectMapper();
    mapper.writeValue(jsonGenerator, object);
    return sw.toString();
  }
  
  /**
   * Thread to perform the actual deployment of the bundle. 
   *
   */
  private class DeployThread implements Runnable {
    private String stagedBundlesPath;
    private BundleDeployStatus status;
    public DeployThread(String stagedBundlesPath, BundleDeployStatus status){
      this.stagedBundlesPath = stagedBundlesPath;
      this.status = status;
    }
    
    @Override
    public void run() {
      bundleDeployer.deploy(status, stagedBundlesPath);
    }
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
