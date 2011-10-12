package org.onebusaway.nyc.transit_data_federation.impl.tdm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.onebusaway.gtfs.model.AgencyAndId;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class VehicleAssignmentServiceImplTest {

  @Mock
  private TransitDataManagerApiLibrary mockApiLibrary;

  @InjectMocks
  private VehicleAssignmentServiceImpl service = new VehicleAssignmentServiceImpl();

  @Before
  public void setupApiLibrary() throws Exception {
    List<Map<String, String>> value = new ArrayList<Map<String, String>>();
    Map<String, String> map = new HashMap<String, String>();
    map.put("agency-id", "MTA");
    map.put("vehicle-id", "45");
    value.add(map);
    when(mockApiLibrary.getItems("depot", "JG", "vehicles", "list")).thenReturn(
        value);

    List<Map<String, String>> value2 = new ArrayList<Map<String, String>>();
    Map<String, String> map2 = new HashMap<String, String>();
    map2.put("agency-id", "MTA");
    map2.put("vehicle-id", "45");
    map2.put("depot-id", "JG");
    value2.add(map2);
    when(mockApiLibrary.getItems("vehicles", "list")).thenReturn(value2);

    // This call is just to prime the service; see the comment in
    // VehicleAssignmentServiceImpl
    @SuppressWarnings("unused")
    ArrayList<AgencyAndId> vehicles = service.getAssignedVehicleIdsForDepot("JG");

  }

  @Test
  public void testGetAssignedVehicleIdsForDepot() throws Exception {
    ArrayList<AgencyAndId> vehicles = service.getAssignedVehicleIdsForDepot("JG");
    assertEquals(1, vehicles.size());
    AgencyAndId agencyAndId = vehicles.get(0);
    String agencyId = agencyAndId.getAgencyId();
    String vehicleId = agencyAndId.getId();
    assertEquals("MTA", agencyId);
    assertEquals("45", vehicleId);
  }

  @Test
  public void testGetAssignedDepotForVehicleId() throws Exception {
    String depot = service.getAssignedDepotForVehicle(new AgencyAndId("MTA",
        "45"));
    assertEquals("JG", depot);
  }

}
