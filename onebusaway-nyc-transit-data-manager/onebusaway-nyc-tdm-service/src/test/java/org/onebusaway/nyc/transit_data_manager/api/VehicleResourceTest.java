package org.onebusaway.nyc.transit_data_manager.api;

import static org.junit.Assert.*;

import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.MtaBusDepotFileToDataCreator;

import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class VehicleResourceTest extends VehicleResource {

  /*
   * TODO This test is a no-op at the moment; we really need to wire VehicleResource (and similar
   * classes) in the right way; the problem is the MtaBusDepotFileToDataCreator wants to re-read
   * the reader every time; so VehicleResource just instantiates a new one, but that makes it a pain in 
   * the butt to  
   */
  @Test
  public void testGetVehicles() throws IOException {
    String vehicles = getVehicles();
    assertNotNull(vehicles);
    String prefix = "{\"vehicles\":[{\"agency-id\":\"2188\",\"vehicle-id\":\"1009\",\"depot-id\":\"GH\"},{\"agency-id\":\"2188\",\"vehicle-id\":\"101\",\"depot-id\":\"LAG\"},";
    assertTrue(vehicles.startsWith(prefix));
  }
  
  @Override
  public MtaBusDepotFileToDataCreator getDataCreator()
      throws FileNotFoundException {
    MtaBusDepotFileToDataCreator process = new MtaBusDepotFileToDataCreator();
    process.setReader(new InputStreamReader(getClass().getResourceAsStream("/DOB_draft_depot_assignments.xml")));
    return process;
  }

}
