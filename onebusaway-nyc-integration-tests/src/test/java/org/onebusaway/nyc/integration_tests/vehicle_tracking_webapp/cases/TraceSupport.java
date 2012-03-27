/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
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

import org.onebusaway.csv_entities.CsvEntityReader;
import org.onebusaway.csv_entities.CsvEntityWriterFactory;
import org.onebusaway.csv_entities.EntityHandler;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestInferredLocationRecord;

public class TraceSupport {

  private boolean shiftStartTime;

  public String uploadTraceForSimulation(File path) throws IOException {
    return uploadTraceForSimulation(path.getName(), new FileInputStream(path));
  }

  public String uploadTraceForSimulation(String fileName, InputStream in)
      throws IOException {

    try {
      HttpClient client = new HttpClient();
      String url = url("/vehicle-location-simulation!upload-trace.do");
      PostMethod post = new PostMethod(url);

      ByteArrayPartSource source = getResourceAsPartSource(fileName, in);
      FilePart filePart = new FilePart("file", source);
      StringPart returnIdParam = new StringPart("returnId", "true");
      StringPart shiftStartTimeParam = new StringPart("shiftStartTime", "" + shiftStartTime);
      StringPart traceTypeParam = new StringPart("traceType", "NycTestInferredLocationRecord");
      StringPart historySize = new StringPart("historySize", "" + 0);

      post.setRequestEntity(new MultipartRequestEntity(new Part[] {
          filePart, returnIdParam, shiftStartTimeParam, traceTypeParam, historySize},
          new HttpMethodParams()));
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

  public List<NycTestInferredLocationRecord> getSimulationResults(String taskId)
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

  public List<NycTestInferredLocationRecord> readRecords(File trace) throws IOException {
    InputStream is = new FileInputStream(trace);
    if (trace.getName().endsWith(".gz"))
      is = new GZIPInputStream(is);
    return readRecords(is);
  }

  public String getRecordsAsString(List<NycTestInferredLocationRecord> actual) {
    CsvEntityWriterFactory factory = new CsvEntityWriterFactory();
    StringWriter out = new StringWriter();
    EntityHandler handler = factory.createWriter(NycTestInferredLocationRecord.class,
        out);
    for (NycTestInferredLocationRecord record : actual)
      handler.handleEntity(record);
    return out.toString();
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

  private List<NycTestInferredLocationRecord> readRecords(InputStream in)
      throws IOException {
    CsvEntityReader reader = new CsvEntityReader();
    EntityCollector<NycTestInferredLocationRecord> records = new EntityCollector<NycTestInferredLocationRecord>();
    reader.addEntityHandler(records);
    reader.readEntities(NycTestInferredLocationRecord.class, in);
    return records.getValues();
  }

  public void setShiftStartTime(boolean shiftStartTime) {
    this.shiftStartTime = shiftStartTime;
  }

  public boolean getShiftStartTime() {
    return shiftStartTime;
  }

}
