package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.onebusaway.gtfs.csv.CsvEntityReader;
import org.onebusaway.gtfs.csv.EntityHandler;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestLocationRecord;

public class TraceSupport {

  public String uploadTraceForSimulation(File path) throws IOException {
    return uploadTraceForSimulation(path.getName(), new FileInputStream(path));
  }

  public String uploadTraceForSimulation(String fileName, InputStream in)
      throws IOException {

    try {
      HttpClient client = new HttpClient();
      PostMethod post = new PostMethod(
          url("/vehicle-location-simulation!upload-trace.do"));

      ByteArrayPartSource source = getResourceAsPartSource(fileName, in);
      FilePart filePart = new FilePart("file", source);
      StringPart stringPart = new StringPart("returnId", "true");
      post.setRequestEntity(new MultipartRequestEntity(new Part[] {
          filePart, stringPart}, new HttpMethodParams()));
      client.executeMethod(post);

      return post.getResponseBodyAsString();

    } catch (HttpException ex) {
      throw new IllegalStateException(ex);
    }

  }

  private String url(String path) {

    String port = System.getProperty(
        "org.onebusaway.transit_data_federation_webapp.port", "9905");

    return "http://localhost:" + port
        + "/onebusaway-nyc-vehicle-tracking-webapp" + path;
  }

  public List<NycTestLocationRecord> getSimulationResults(String taskId)
      throws IOException {
    try {
      URL url = new URL(
          url("/vehicle-location-simulation!task-result-records.do?taskId="
              + taskId));
      InputStream in = url.openStream();
      return readRecords(in);
    } catch (MalformedURLException ex) {
      throw new IllegalStateException(ex);
    }
  }

  public List<NycTestLocationRecord> readRecords(File trace) throws IOException {
    InputStream is = new FileInputStream(trace);
    if (trace.getName().endsWith(".gz"))
      is = new GZIPInputStream(is);
    return readRecords(is);
  }

  /****
   * 
   ****/

  private ByteArrayPartSource getResourceAsPartSource(String fileName,
      InputStream in) throws IOException {

    ByteArrayOutputStream out = new ByteArrayOutputStream();

    byte[] buffer = new byte[1024];

    while (true) {
      int rc = in.read(buffer);
      if (rc < 0)
        break;
      out.write(buffer, 0, rc);
    }

    in.close();
    return new ByteArrayPartSource(fileName, out.toByteArray());
  }

  private static class EntityCollector<T> implements EntityHandler {

    private List<T> _values = new ArrayList<T>();

    public List<T> getValues() {
      return _values;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handleEntity(Object bean) {
      _values.add((T) bean);
    }
  }

  private List<NycTestLocationRecord> readRecords(InputStream in)
      throws IOException {
    CsvEntityReader reader = new CsvEntityReader();
    EntityCollector<NycTestLocationRecord> records = new EntityCollector<NycTestLocationRecord>();
    reader.addEntityHandler(records);
    reader.readEntities(NycTestLocationRecord.class, in);
    return records.getValues();
  }

}
