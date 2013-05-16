package org.onebusaway.nyc.vehicle_tracking.impl.queue;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.services.tdm.VehicleAssignmentService;
import org.onebusaway.nyc.vehicle_tracking.services.inference.VehicleLocationInferenceService;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;

@RunWith(MockitoJUnitRunner.class)
public class PartitionedInputQueueListenerTaskTest {

  @Mock
  private VehicleAssignmentService vehicleAssignmentService;

  @SuppressWarnings("unused")
  @Mock
  private VehicleLocationInferenceService vehicleLocationInferenceService;

  @InjectMocks
  private PartitionedInputQueueListenerTask service;

  @Before
  public void setup() throws Exception {
    final ArrayList<AgencyAndId> list = new ArrayList<AgencyAndId>();
    list.add(AgencyAndIdLibrary.convertFromString("MTA NYCT_7578"));
    list.add(AgencyAndIdLibrary.convertFromString("MTA NYCT_3905"));
    
    when(vehicleAssignmentService.getAssignedVehicleIdsForDepot("JG")).thenReturn(
        list);

    service.setDepotPartitionKey("JG");
  }

  @Test
  public void testAccept() throws Exception {
    final String message = "{\"RealtimeEnvelope\": {\"UUID\": \"foo\",\"timeReceived\": 1234567,\"CcLocationReport\": {\"request-id\" : 528271,\"vehicle\": {\"vehicle-id\": 7578,\"agency-id\": 2008,\"agencydesignator\": \"MTA NYCT\"},\"status-info\": 0,\"time-reported\": \"2011-10-15T03:26:19.000-00:00\",\"latitude\": 40612060,\"longitude\": -74035771,\"direction\": {\"deg\": 128.77},\"speed\": 0,\"manufacturer-data\": \"VFTP123456789\",\"operatorID\": {\"operator-id\": 0,\"designator\": \"\"},\"runID\": {\"run-id\": 0,\"designator\": \"\"},\"destSignCode\": 4631,\"routeID\": {\"route-id\": 0,\"route-designator\": \"\"},\"localCcLocationReport\": {\"NMEA\": {\"sentence\": [\"\",\"\"]}}}}}";
    assertEquals(service.processMessage(null, message.getBytes()), true);
  }

  @Test
  public void testWrongPartition() throws Exception {
    service.setDepotPartitionKey("XX");

    final String message = "{\"RealtimeEnvelope\": {\"UUID\": \"foo\",\"timeReceived\": 1234567,\"CcLocationReport\": {\"request-id\" : 528271,\"vehicle\": {\"vehicle-id\": 7578,\"agency-id\": 2008,\"agencydesignator\": \"MTA NYCT\"},\"status-info\": 0,\"time-reported\": \"2011-10-15T03:26:19.000-00:00\",\"latitude\": 40612060,\"longitude\": -74035771,\"direction\": {\"deg\": 128.77},\"speed\": 0,\"manufacturer-data\": \"VFTP123456789\",\"operatorID\": {\"operator-id\": 0,\"designator\": \"\"},\"runID\": {\"run-id\": 0,\"designator\": \"\"},\"destSignCode\": 4631,\"routeID\": {\"route-id\": 0,\"route-designator\": \"\"},\"localCcLocationReport\": {\"NMEA\": {\"sentence\": [\"\",\"\"]}}}}}";
    assertEquals(service.processMessage(null, message.getBytes()), false);
  }

  @Test
  public void testWrongVehicleId() throws Exception {
    service.setDepotPartitionKey("JG");

    final String message = "{\"RealtimeEnvelope\":{\"UUID\":\"foo\",\"timeReceived\":1234567,\"CcLocationReport\": {\"request-id\" : 528271,\"vehicle\": {\"vehicle-id\": 7579,\"agency-id\": 2008,\"agencydesignator\": \"MTA NYCT\"},\"status-info\": 0,\"time-reported\": \"2011-10-15T03:26:19.000-00:00\",\"latitude\": 40612060,\"longitude\": -74035771,\"direction\": {\"deg\": 128.77},\"speed\": 0,\"manufacturer-data\": \"VFTP123456789\",\"operatorID\": {\"operator-id\": 0,\"designator\": \"\"},\"runID\": {\"run-id\": 0,\"designator\": \"\"},\"destSignCode\": 4631,\"routeID\": {\"route-id\": 0,\"route-designator\": \"\"},\"localCcLocationReport\": {\"NMEA\": {\"sentence\": [\"\",\"\"]}}}}}";
    assertEquals(service.processMessage(null, message.getBytes()), false);
  }

  @Test
  public void testNoNMEA() throws Exception {
    final String message = "{\"RealtimeEnvelope\": {\"UUID\":\"e0f9f990-95ee-11e2-a2f1-1231391c6b0a\",\"timeReceived\": 1364286398377,\"CcLocationReport\":{\"request-id\":2,\"vehicle\":{\"vehicle-id\":3905,\"agency-id\":2008,\"agencydesignator\":\"MTA NYCT\"},\"status-info\":0,\"time-reported\":\"2013-03-26T08:26:28.0-00:00\",\"latitude\":0,\"longitude\":0,\"direction\":{\"deg\":0.0},\"speed\":0,\"data-quality\":{\"qualitative-indicator\":4},\"manufacturer-data\":\"BMV30101\",\"operatorID\":{\"operator-id\":0,\"designator\":\"0\"},\"runID\":{\"run-id\":0,\"designator\":\"0\"},\"destSignCode\":6,\"routeID\":{\"route-id\":0,\"route-designator\":\"0\"},\"localCcLocationReport\":{\"vehiclePowerState\":1}}}}" ;
    assertEquals(service.processMessage(null, message.getBytes()), true);
  }


}
