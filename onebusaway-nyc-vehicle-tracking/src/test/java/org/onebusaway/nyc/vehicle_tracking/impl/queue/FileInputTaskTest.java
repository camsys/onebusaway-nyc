package org.onebusaway.nyc.vehicle_tracking.impl.queue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.services.tdm.VehicleAssignmentService;
import org.onebusaway.nyc.vehicle_tracking.services.inference.VehicleLocationInferenceService;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;

@RunWith(MockitoJUnitRunner.class)
public class FileInputTaskTest {

  @Mock
  private VehicleAssignmentService vehicleAssignmentService;

  @SuppressWarnings("unused")
  @Mock
  private VehicleLocationInferenceService vehicleLocationInferenceService;

  @InjectMocks
  private FileInputTask service;
  
  @InjectMocks
  private FileInputServiceImpl inputService;

  @Before
  public void setup() throws Exception {
	
	BasicConfigurator.configure();
    
	final ArrayList<AgencyAndId> list = new ArrayList<AgencyAndId>();
    list.add(AgencyAndIdLibrary.convertFromString("MTA NYCT_8140"));
    
    when(vehicleAssignmentService.getAssignedVehicleIdsForDepot("ZZ")).thenReturn(
        list);
    when(vehicleAssignmentService.getAssignedVehicleIdsForDepot("JG")).thenReturn(
            list);
    
    inputService.setup();
    inputService.setDepotPartitionKey("JG");
    service.setInputService(inputService);
    service.setFilename("/home/dev/corruptmsg.txt");
  }

  @Test
  public void testDeserializeMessage() throws Exception {
	  final String message = "{\"RealtimeEnvelope\": {\"UUID\":\"f43530c0-ec7a-11e5-a081-22000b028187\",\"timeReceived\": 1458244853196,\"CcLocationReport\": {\"request-id\":250,\"vehicle\":{\"vehicle-id\":8140,\"agency-id\":2008,\"agencydesignator\":\"MTA NYCT\"},\"status-info\":0,\"time-reported\":\"2016-03-17T20:00:50.860-00:00\",\"latitude\":40530910,\"longitude\":-74236203,\"direction\":{\"deg\":71.50},\"speed\":36,\"manufacturer-data\":\"VFTP155-600-826\",\"operatorID\":{\"operator-id\":0,\"designator\":\"0\"},\"runID\":{\"run-id\":0,\"designator\":\"000\"},\"destSignCode\":12,\"routeID\":{\"route-id\":0,\"route-designator\":\"0\"},\"localCcLocationReport\":{\"NMEA\":{\"sentence\":[\"$GPGGA,175251.000,4031.85833,N,07414.16762,W,1,08,01.2,+00031.0,M,,M,,*46\",\"$GPRMC,175251.00,A,4031.858330,N,07414.167620,W,006.955,033.61\"]}}}}}";
	  assertNotNull(inputService.deserializeMessage(message));
  }
  
  @Test
  public void testProcessMessage() throws Exception {
	  service.setVehicleLocationService(vehicleLocationInferenceService);
	  final String message = "{\"RealtimeEnvelope\": {\"UUID\": \"foo\",\"timeReceived\": 1234567,\"CcLocationReport\": {\"request-id\" : 528271,\"vehicle\": {\"vehicle-id\": 8140,\"agency-id\": 2008,\"agencydesignator\": \"MTA NYCT\"},\"status-info\": 0,\"time-reported\": \"2011-10-15T03:26:19.000-00:00\",\"latitude\": 40612060,\"longitude\": -74035771,\"direction\": {\"deg\": 128.77},\"speed\": 0,\"manufacturer-data\": \"VFTP123456789\",\"operatorID\": {\"operator-id\": 0,\"designator\": \"\"},\"runID\": {\"run-id\": 0,\"designator\": \"\"},\"destSignCode\": 4631,\"routeID\": {\"route-id\": 0,\"route-designator\": \"\"},\"localCcLocationReport\": {\"NMEA\": {\"sentence\": [\"\",\"\"]}}}}}";
	  assertEquals(true, inputService.processMessage(null, message.getBytes()));
  }
  
  @Test
  public void testNoOuterBrackets() throws Exception {
	  service.setVehicleLocationService(vehicleLocationInferenceService);
	  final String message = "\"RealtimeEnvelope\": {\"UUID\": \"foo\",\"timeReceived\": 1234567,\"CcLocationReport\": {\"request-id\" : 528271,\"vehicle\": {\"vehicle-id\": 8140,\"agency-id\": 2008,\"agencydesignator\": \"MTA NYCT\"},\"status-info\": 0,\"time-reported\": \"2011-10-15T03:26:19.000-00:00\",\"latitude\": 40612060,\"longitude\": -74035771,\"direction\": {\"deg\": 128.77},\"speed\": 0,\"manufacturer-data\": \"VFTP123456789\",\"operatorID\": {\"operator-id\": 0,\"designator\": \"\"},\"runID\": {\"run-id\": 0,\"designator\": \"\"},\"destSignCode\": 4631,\"routeID\": {\"route-id\": 0,\"route-designator\": \"\"},\"localCcLocationReport\": {\"NMEA\": {\"sentence\": [\"\",\"\"]}}}}";
	  assertEquals(true, inputService.processMessage(null, message.getBytes()));
  }
}
