package org.onebusaway.nyc.transit_data_manager.api.bundle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.onebusaway.nyc.transit_data_manager.bundle.BundleProvider;
import org.onebusaway.nyc.transit_data_manager.bundle.BundlesListMessage;
import org.onebusaway.nyc.transit_data_manager.bundle.model.Bundle;
import org.onebusaway.nyc.transit_data_manager.json.JsonTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sun.jersey.core.header.ContentDisposition;

@Path("/bundle")
@Component
public class BundleServiceResource {

  private static Logger _log = LoggerFactory.getLogger(BundleServiceResource.class);

  @Autowired
  BundleProvider bundleProvider;
  @Autowired
  JsonTool jsonTool;

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
}
