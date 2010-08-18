package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.onebusaway.gtfs.csv.CsvEntityReader;
import org.onebusaway.gtfs.csv.EntityHandler;
import org.onebusaway.gtfs.csv.exceptions.CsvEntityIOException;
import org.onebusaway.gtfs.csv.schema.DefaultEntitySchemaFactory;
import org.onebusaway.gtfs.csv.schema.EntitySchemaFactoryHelper;
import org.onebusaway.gtfs.csv.schema.beans.CsvEntityMappingBean;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.realtime.api.VehicleLocationListener;
import org.onebusaway.realtime.api.VehicleLocationRecord;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class VehicleLocationInferenceServiceImplTest {
  class MockVehicleLocationListener implements VehicleLocationListener {
    public ArrayList<VehicleLocationRecord> storedRecords;

    MockVehicleLocationListener() {
      storedRecords = new ArrayList<VehicleLocationRecord>();
    }

    @Override
    public void handleVehicleLocationRecord(VehicleLocationRecord record) {
      storedRecords.add(record);
    }

    @Override
    public void handleVehicleLocationRecords(List<VehicleLocationRecord> records) {
      storedRecords.addAll(records);
    }

  }

  @Test
  public void testInference() throws CsvEntityIOException, IOException,
      InterruptedException {
    final VehicleLocationInferenceServiceImpl service;
    service = new VehicleLocationInferenceServiceImpl();
    MockVehicleLocationListener mockVehicleLocationListener = new MockVehicleLocationListener();
    service.setVehicleLocationListener(mockVehicleLocationListener);
    service.start();
    CsvEntityReader reader = new CsvEntityReader();

    DefaultEntitySchemaFactory factory = new DefaultEntitySchemaFactory();
    EntitySchemaFactoryHelper helper = new EntitySchemaFactoryHelper(factory);

    CsvEntityMappingBean record = helper.addEntity(NycTestLocationRecord.class);
    record.setAutoGenerateSchema(false);
    reader.setEntitySchemaFactory(factory);
    record.addAdditionalFieldMapping(new NycTestLocationRecord.FieldMapping());

    reader.addEntityHandler(new EntityHandler() {

      @Override
      public void handleEntity(Object bean) {
        NycTestLocationRecord record = (NycTestLocationRecord) bean;

        NycVehicleLocationRecord location = new NycVehicleLocationRecord();
        location.setTime(record.getTimestamp());
        assertNotNull(record.getVehicleId());
        location.setVehicleId(new AgencyAndId("MTA NYCT", record.getVehicleId()));
        location.setDestinationSignCode(record.getDsc());
        location.setLatitude(record.getLat());
        location.setLongitude(record.getLon());

        service.handleVehicleLocation(location);
      }
    });

    InputStream in = getClass().getResourceAsStream("ivn-dsc.csv");
    assertNotNull(in);
    reader.readEntities(NycTestLocationRecord.class, in);

    /* wait a max of two seconds for processing to finish */
    VehicleLocationRecord lastLocation = null;
    for (int i = 0; i < 20; ++i) {
      Thread.sleep(100); // wait for processing to finish (but not very long)
      List<VehicleLocationRecord> records = mockVehicleLocationListener.storedRecords;
      lastLocation = records.get(records.size() - 1);
      if (Math.abs(40.717632 - lastLocation.getCurrentLocationLat()) < 0.0000001) {
        break;
      }
    }
    assertEquals(40.717632, lastLocation.getCurrentLocationLat(), 0.0000001);
    assertEquals(-73.920038, lastLocation.getCurrentLocationLon(), 0.0000001);
  }

}
