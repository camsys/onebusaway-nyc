package org.onebusaway.nyc.transit_data_manager.api.bundle;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.onebusaway.nyc.transit_data_manager.bundle.BundleDeployer;
import org.onebusaway.nyc.transit_data_manager.bundle.BundleProvider;
import org.onebusaway.nyc.transit_data_manager.bundle.BundlesListMessage;
import org.onebusaway.nyc.transit_data_manager.bundle.model.Bundle;
import org.onebusaway.nyc.transit_data_manager.bundle.model.BundleDeployStatus;
import org.onebusaway.nyc.transit_data_manager.json.JsonTool;
import org.onebusaway.nyc.util.configuration.ConfigurationService;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.stereotype.Component;

import com.sun.jersey.core.header.ContentDisposition;

@Path("/bundle")
@Component
@Scope("singleton")
public class BundleServiceResource {

  private static final String DEFAULT_BUNDLE_STAGING_DIRECTORY = "activebundles";

  private static Logger _log = LoggerFactory.getLogger(BundleServiceResource.class);

  private ExecutorService _executorService = null;
  @Autowired
  private BundleProvider bundleProvider;
  @Autowired
  private JsonTool jsonTool;
  @Autowired
  private BundleDeployer bundleDeployer;
  @Autowired
  private ConfigurationService configurationService;
  private Map<String, BundleDeployStatus> _deployMap = new HashMap<String, BundleDeployStatus>();
  private Integer jobCounter = 0;

  @PostConstruct
  public void setup() {
      _executorService = Executors.newFixedThreadPool(1);
  }
  
  public BundleDeployStatus lookupDeployRequest(String id) {
    return _deployMap.get(id);
  }
  
  public void setConfigurationService(ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}
  
  public void setBundleProvider(BundleProvider bundleProvider) {
    this.bundleProvider = bundleProvider;
  }

  public void setJsonTool(JsonTool jsonTool) {
    this.jsonTool = jsonTool;
  }

  @Path("/list")
  @GET
  public Response getBundleList() {
    _log.info("Starting getBundleList.");

    List<Bundle> bundles = bundleProvider.getBundles();

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

  @Path("/{bundleId}/file/{bundleFileFilename: [a-zA-Z0-9_./]+}/get")
  @GET
  public Response getBundleFile(@PathParam("bundleId") String bundleId,
      @PathParam("bundleFileFilename") String relativeFilename) {

    _log.info("starting getBundleFile for relative filename " + relativeFilename + " in bundle " + bundleId);

    boolean requestIsForValidBundleFile = bundleProvider.checkIsValidBundleFile(
        bundleId, relativeFilename);
    if (!requestIsForValidBundleFile) {
      throw new WebApplicationException(new IllegalArgumentException(
          relativeFilename + " is not listed in bundle metadata."),
          Response.Status.BAD_REQUEST);
    }

    final File requestedFile;
    try {
      requestedFile = bundleProvider.getBundleFile(bundleId, relativeFilename);

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
  
  @Path("/deploy/list/{environment}")
  @GET
  /**
   * list the bundle(s) that are on S3, potentials to be deployed.
   */
  public Response list(@PathParam("environment") String environment) {
    String s3Path = getBundleDirectory() + File.separator + environment + File.separator;
    List<String> list = bundleDeployer.listBundlesForServing(s3Path);
    try {
      // serialize the status object and send to client -- it contains an id for querying
      final StringWriter sw = new StringWriter();
      final MappingJsonFactory jsonFactory = new MappingJsonFactory();
      final JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(sw);
      ObjectMapper mapper = new ObjectMapper();
      mapper.writeValue(jsonGenerator, list);
      return Response.ok(sw.toString()).build();
    } catch (Exception e) {
      _log.error("exception serializing response:", e);
    }
    return Response.serverError().build();
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
    _log.info("Starting deploy(" + environment + ")...");
    String s3Path = getBundleDirectory() + File.separator + environment + File.separator;
    BundleDeployStatus status = new BundleDeployStatus();
    status.setId(getNextId());
    _deployMap.put(status.getId(), status);
    _executorService.execute(new DeployThread(s3Path, status));
    _log.info("deploy request complete");

    try {
      // serialize the status object and send to client -- it contains an id for querying
      final StringWriter sw = new StringWriter();
      final MappingJsonFactory jsonFactory = new MappingJsonFactory();
      final JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(sw);
      ObjectMapper mapper = new ObjectMapper();
      mapper.writeValue(jsonGenerator, status);
      return Response.ok(sw.toString()).build();
    } catch (Exception e) {
      _log.error("exception serializing response:", e);
    }
    return Response.serverError().build();
    }

  @Path("/deploy/status/{id}/list")
  @GET
  /**
   * query the status of a requested bundle deployment
   * @param id the id of a BundleDeploymentStatus
   * @return a serialized version of the requested BundleDeploymentStatus, null otherwise
   */
  public Response deployStatus(@PathParam("id") String id) {
    BundleDeployStatus status = this.lookupDeployRequest(id);
    try {
      final StringWriter sw = new StringWriter();
      final MappingJsonFactory jsonFactory = new MappingJsonFactory();
      final JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(sw);
      ObjectMapper mapper = new ObjectMapper();
      mapper.writeValue(jsonGenerator, status);
      return Response.ok(sw.toString()).build();
    } catch (Exception e) {
      _log.error("exception serializing response:", e);
    }
    return Response.serverError().build();
    }

  
  
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
  
  private String getBundleDirectory() {
    if (configurationService != null) {
      try {
        return configurationService.getConfigurationValueAsString("admin.bundle_directory", DEFAULT_BUNDLE_STAGING_DIRECTORY);
      } catch (RemoteConnectFailureException e){
        _log.error("default bundle dir lookup failed:", e);
        return DEFAULT_BUNDLE_STAGING_DIRECTORY;
      }
    }
    return DEFAULT_BUNDLE_STAGING_DIRECTORY;
  }

  /**
   * Thread to perform the actual deployment of the bundle.  Downloading from S3 and
   * unzipping can take some time....
   *
   */
  private class DeployThread implements Runnable {
    private String s3Path;
    private BundleDeployStatus status;
    public DeployThread(String s3Path, BundleDeployStatus status){
      this.s3Path = s3Path;
      this.status = status;
    }
    
    @Override
    public void run() {
      bundleDeployer.deploy(status, s3Path);
    }
  }
  
}
