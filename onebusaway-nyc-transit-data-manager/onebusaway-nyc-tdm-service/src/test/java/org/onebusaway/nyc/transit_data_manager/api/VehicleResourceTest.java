package org.onebusaway.nyc.transit_data_manager.api;

import static org.junit.Assert.*;

import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.MtaBusDepotFileToDataCreator;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;

public class VehicleResourceTest extends VehicleResource {

  @Test
  public void testGetVehicles() throws IOException {
    MtaBusDepotFileToDataCreator process = new MtaBusDepotFileToDataCreator();
    process.setReader(new InputStreamReader(getClass().getResourceAsStream("/DOB_draft_depot_assignments.xml")));
    setDataCreator(process);
    String vehicles = getVehicles();
    assertNotNull(vehicles);
    String prefix = "{\"vehicles\":[{\"agency-id\":\"2188\",\"vehicle-id\":\"1009\",\"depot-id\":\"GH\"},{\"agency-id\":\"2188\",\"vehicle-id\":\"101\",\"depot-id\":\"LAG\"},";
    assertTrue(vehicles.startsWith(prefix));
  }

}
