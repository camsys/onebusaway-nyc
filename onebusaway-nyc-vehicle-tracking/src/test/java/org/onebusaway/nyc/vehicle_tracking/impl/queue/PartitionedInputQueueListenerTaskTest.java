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
    ArrayList<AgencyAndId> list = new ArrayList<AgencyAndId>();
    list.add(AgencyAndIdLibrary.convertFromString("MTA NYCT_7578"));

    when(vehicleAssignmentService.getAssignedVehicleIdsForDepot("JG"))
      .thenReturn(list);
    
    service.setDepotPartitionKey("JG");
  }

  @Test
  public void testAccept() throws Exception {
    String message = "{\"RealtimeEnvelope\": {\"UUID\": \"foo\",\"timeReceived\": 1234567,\"CcLocationReport\": {\"request-id\" : 528271,\"vehicle\": {\"vehicle-id\": 7578,\"agency-id\": 2008,\"agencydesignator\": \"MTA NYCT\"},\"status-info\": 0,\"time-reported\": \"2011-10-15T03:26:19.000-00:00\",\"latitude\": 40612060,\"longitude\": -74035771,\"direction\": {\"deg\": 128.77},\"speed\": 0,\"manufacturer-data\": \"VFTP123456789\",\"operatorID\": {\"operator-id\": 0,\"designator\": \"\"},\"runID\": {\"run-id\": 0,\"designator\": \"\"},\"destSignCode\": 4631,\"routeID\": {\"route-id\": 0,\"route-designator\": \"\"},\"localCcLocationReport\": {\"NMEA\": {\"sentence\": [\"\",\"\"]}}}}}";
    assertEquals(service.processMessage(null, message), true);
  }

  @Test
  public void testWrongPartition() throws Exception {
    service.setDepotPartitionKey("XX");

    String message = "{\"RealtimeEnvelope\": {\"UUID\": \"foo\",\"timeReceived\": 1234567,\"CcLocationReport\": {\"request-id\" : 528271,\"vehicle\": {\"vehicle-id\": 7578,\"agency-id\": 2008,\"agencydesignator\": \"MTA NYCT\"},\"status-info\": 0,\"time-reported\": \"2011-10-15T03:26:19.000-00:00\",\"latitude\": 40612060,\"longitude\": -74035771,\"direction\": {\"deg\": 128.77},\"speed\": 0,\"manufacturer-data\": \"VFTP123456789\",\"operatorID\": {\"operator-id\": 0,\"designator\": \"\"},\"runID\": {\"run-id\": 0,\"designator\": \"\"},\"destSignCode\": 4631,\"routeID\": {\"route-id\": 0,\"route-designator\": \"\"},\"localCcLocationReport\": {\"NMEA\": {\"sentence\": [\"\",\"\"]}}}}}";
    assertEquals(service.processMessage(null, message), false);
  }

  @Test
  public void testWrongVehicleId() throws Exception {
    service.setDepotPartitionKey("JG");

    String message = "{\"RealtimeEnvelope\":{\"UUID\":\"foo\",\"timeReceived\":1234567\",\"CcLocationReport\": {\"request-id\" : 528271,\"vehicle\": {\"vehicle-id\": 7579,\"agency-id\": 2008,\"agencydesignator\": \"MTA NYCT\"},\"status-info\": 0,\"time-reported\": \"2011-10-15T03:26:19.000-00:00\",\"latitude\": 40612060,\"longitude\": -74035771,\"direction\": {\"deg\": 128.77},\"speed\": 0,\"manufacturer-data\": \"VFTP123456789\",\"operatorID\": {\"operator-id\": 0,\"designator\": \"\"},\"runID\": {\"run-id\": 0,\"designator\": \"\"},\"destSignCode\": 4631,\"routeID\": {\"route-id\": 0,\"route-designator\": \"\"},\"localCcLocationReport\": {\"NMEA\": {\"sentence\": [\"\",\"\"]}}}}}";
    assertEquals(service.processMessage(null, message), false);
  }

}
