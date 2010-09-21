package org.onebusaway.nyc.integration_tests.nyc_webapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.gtfs.csv.CsvEntityReader;
import org.onebusaway.gtfs.csv.EntityHandler;
import org.onebusaway.gtfs.csv.exceptions.CsvEntityIOException;
import org.onebusaway.nyc.integration_tests.DataTestSupport;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestLocationRecord;
import org.onebusaway.siri.model.FramedVehicleJourneyRef;
import org.onebusaway.siri.model.MonitoredVehicleJourney;
import org.onebusaway.siri.model.ServiceDelivery;
import org.onebusaway.siri.model.Siri;
import org.onebusaway.siri.model.VehicleActivity;
import org.onebusaway.siri.model.VehicleLocation;
import org.onebusaway.siri.model.VehicleMonitoringDelivery;

import com.thoughtworks.xstream.XStream;

public class VehicleTrackingWebappIntegrationTest {

  private String _baseUrl;

  private XStream _xstream;

  private HttpClient _client = new HttpClient();

  private int _recordCount = 0;

  private NycTestLocationRecord _previousRecord;

  @Before
  public void setup() {
    String port = System.getProperty(
        "org.onebusaway.transit_data_federation_webapp.port", "9905");
    _baseUrl = "http://localhost:" + port;
    _xstream = new XStream();
    _xstream.processAnnotations(Siri.class);
    _xstream.processAnnotations(ServiceDelivery.class);
  }

  @Test
  public void testTracking() throws CsvEntityIOException, IOException {

    // Make sure we've reset any previous data that might be in the system
    GetMethod get = new GetMethod(
        url("/vehicle-location!reset.do?vehicleId=4514"));
    _client.executeMethod(get);
    get.releaseConnection();

    CsvEntityReader reader = new CsvEntityReader();

    reader.addEntityHandler(new RequestSender());

    InputStream in = DataTestSupport.getTestDataAsInputStream();
    assertNotNull(in);
    reader.readEntities(NycTestLocationRecord.class, in);

    /*
     * todo: check the API so that we can see that the data came in. In the
     * meantime, we query the location data directly from the server.
     */

  }

  private void handleRecord(NycTestLocationRecord record) throws Exception {

    if (_recordCount > 20)
      return;

    // Skip duplicate gps
    if (_previousRecord != null && _previousRecord.getLat() == record.getLat()
        && _previousRecord.getLon() == record.getLon())
      return;

    _previousRecord = record;
    _recordCount++;

    String postData = getRecordAsSerializedSiri(record);

    String url = url("/update-location");
    PostMethod method = new PostMethod(url);
    RequestEntity requestEntity = new StringRequestEntity(postData);
    method.setRequestEntity(requestEntity);

    int status = _client.executeMethod(method);
    assertEquals(200, status);
    assertEquals("ok\n", getResponseAsString(method));

    // Wait so that the record can process
    Thread.sleep(1000);

    GetMethod get = new GetMethod(url("/vehicle-location.do?vehicleId=4514"));
    status = _client.executeMethod(get);
    assertEquals(200, status);

    JSONObject json = new JSONObject(getResponseAsString(get));
    JSONObject r = json.getJSONObject("record");
    double lat = r.getDouble("currentLocationLat");
    double lon = r.getDouble("currentLocationLon");

    double d = SphericalGeometryLibrary.distance(record.getLat(),
        record.getLon(), lat, lon);
    assertTrue(d < 100);

  }

  private String getRecordAsSerializedSiri(NycTestLocationRecord record) {
    Siri siri = new Siri();
    siri.ServiceDelivery = new ServiceDelivery();
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(record.getTimestamp());
    siri.ServiceDelivery.ResponseTimestamp = calendar;
    siri.ServiceDelivery.VehicleMonitoringDelivery = new VehicleMonitoringDelivery();

    VehicleActivity activity = new VehicleActivity();
    activity.VehicleMonitoringRef = record.getDsc();
    activity.MonitoredVehicleJourney = new MonitoredVehicleJourney();
    activity.MonitoredVehicleJourney.VehicleRef = record.getVehicleId();
    activity.MonitoredVehicleJourney.VehicleLocation = new VehicleLocation();
    activity.MonitoredVehicleJourney.VehicleLocation.Latitude = record.getLat();
    activity.MonitoredVehicleJourney.VehicleLocation.Longitude = record.getLon();
    activity.MonitoredVehicleJourney.FramedVehicleJourneyRef = new FramedVehicleJourneyRef();
    activity.MonitoredVehicleJourney.FramedVehicleJourneyRef.DatedVehicleJourneyRef = record.getVehicleId();

    siri.ServiceDelivery.VehicleMonitoringDelivery.deliveries = new ArrayList<VehicleActivity>();
    siri.ServiceDelivery.VehicleMonitoringDelivery.deliveries.add(activity);

    return _xstream.toXML(siri);
  }

  private String url(String url) {
    return _baseUrl + "/onebusaway-nyc-vehicle-tracking-webapp" + url;
  }

  private class RequestSender implements EntityHandler {
    @Override
    public void handleEntity(Object bean) {
      NycTestLocationRecord record = (NycTestLocationRecord) bean;
      try {
        handleRecord(record);
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }
  }

  private String getResponseAsString(HttpMethod method) throws IOException {
    
    StringBuilder b = new StringBuilder();
    String line = null;

    BufferedReader reader = new BufferedReader(new InputStreamReader(
        method.getResponseBodyAsStream()));

    while ((line = reader.readLine()) != null)
      b.append(line).append("\n");

    reader.close();

    return b.toString();

  }
}
