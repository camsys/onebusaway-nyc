package org.onebusaway.nyc.integration_tests.nyc_webapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.onebusaway.gtfs.csv.CsvEntityReader;
import org.onebusaway.gtfs.csv.EntityHandler;
import org.onebusaway.gtfs.csv.exceptions.CsvEntityIOException;
import org.onebusaway.gtfs.csv.schema.DefaultEntitySchemaFactory;
import org.onebusaway.gtfs.csv.schema.EntitySchemaFactoryHelper;
import org.onebusaway.gtfs.csv.schema.beans.CsvEntityMappingBean;
import org.onebusaway.nyc.vehicle_tracking.test.impl.inference.NycTestLocationRecord;
import org.onebusaway.siri.model.FramedVehicleJourneyRef;
import org.onebusaway.siri.model.MonitoredVehicleJourney;
import org.onebusaway.siri.model.ServiceDelivery;
import org.onebusaway.siri.model.Siri;
import org.onebusaway.siri.model.VehicleActivity;
import org.onebusaway.siri.model.VehicleLocation;
import org.onebusaway.siri.model.VehicleMonitoringDelivery;

import com.thoughtworks.xstream.XStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;

public class VehicleTrackingWebappIntegrationTest extends NycWebappTestSupport {

  static class RequestSender implements EntityHandler {
    static int i = 0;
    private String _baseUrl;
    private XStream xstream;

    public RequestSender(String baseUrl) {
      _baseUrl = baseUrl;
      xstream = new XStream();
      xstream.processAnnotations(Siri.class);
      xstream.processAnnotations(ServiceDelivery.class);
    }

    @Override
    public void handleEntity(Object bean) {
      if (i++ >= 5) {
        return;
      }
      NycTestLocationRecord record = (NycTestLocationRecord) bean;
      Siri siri = new Siri();
      siri.ServiceDelivery = new ServiceDelivery();
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(record.getTimestamp());
      siri.ServiceDelivery.ResponseTimestamp = calendar;
      siri.ServiceDelivery.VehicleMonitoringDelivery = new VehicleMonitoringDelivery();

      VehicleActivity activity = new VehicleActivity();
      activity.VehicleMonitoringRef = record.getDsc();
      activity.MonitoredVehicleJourney = new MonitoredVehicleJourney();
      activity.MonitoredVehicleJourney.VehicleLocation = new VehicleLocation();
      activity.MonitoredVehicleJourney.VehicleLocation.Latitude = record.getLat();
      activity.MonitoredVehicleJourney.VehicleLocation.Longitude = record.getLon();
      activity.MonitoredVehicleJourney.FramedVehicleJourneyRef = new FramedVehicleJourneyRef();
      activity.MonitoredVehicleJourney.FramedVehicleJourneyRef.DatedVehicleJourneyRef = record.getVehicleId();

      siri.ServiceDelivery.VehicleMonitoringDelivery.deliveries = new ArrayList<VehicleActivity>();
      siri.ServiceDelivery.VehicleMonitoringDelivery.deliveries.add(activity);

      String postData = xstream.toXML(siri);

      HttpClient client = new HttpClient();
      PostMethod method = new PostMethod(_baseUrl
          + "/onebusaway-nyc-vehicle-tracking-webapp/update-location");
      RequestEntity requestEntity = new StringRequestEntity(postData);
      method.setRequestEntity(requestEntity);
      try {
        int status = client.executeMethod(method);
        assertEquals(200, status);
        assertEquals("ok", method.getResponseBodyAsString());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

  }

  @Test
  public void testTracking() throws CsvEntityIOException, IOException {
    /* set up a vehicle tracking webapp */

    String port = System.getProperty(
        "org.onebusaway.transit_data_federation_webapp.port", "9905");

    String _baseUrl = "http://localhost:" + port;

    CsvEntityReader reader = new CsvEntityReader();

    DefaultEntitySchemaFactory factory = new DefaultEntitySchemaFactory();
    EntitySchemaFactoryHelper helper = new EntitySchemaFactoryHelper(factory);

    CsvEntityMappingBean record = helper.addEntity(NycTestLocationRecord.class);

    record.setAutoGenerateSchema(false);
    reader.setEntitySchemaFactory(factory);
    record.addAdditionalFieldMapping(new NycTestLocationRecord.FieldMapping());

    reader.addEntityHandler(new RequestSender(_baseUrl));

    InputStream in = NycTestLocationRecord.getTestData();
    assertNotNull(in);
    reader.readEntities(NycTestLocationRecord.class, in);
    
    /*
     * todo: check the API so that we can see that the data came in. Since I
     * think the API is not yet hooked up, we'll just ignore this for now.
     */

  }
}
