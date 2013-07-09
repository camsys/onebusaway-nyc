package org.onebusaway.nyc.transit_data_manager.adapters.api.processes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.Test;
import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaBusDepotAssignment;

public class MtaBusDepotFileToDataCreatorTest  {

  @Test
  public void testLoadDepotAssignments() throws FileNotFoundException, IOException, URISyntaxException {
    URI depAssignUri = getClass().getResource("/DOB_draft_depot_assignments.xml").toURI();
    
    MtaBusDepotFileToDataCreator dcreator = new MtaBusDepotFileToDataCreator(new File(depAssignUri));
    
    List<MtaBusDepotAssignment> assignments = dcreator.loadDepotAssignments();
    assertNotNull(assignments);
    assertTrue(assignments.size() > 0);
    MtaBusDepotAssignment assignment = assignments.get(0);
    assertEquals(new Long(2008), assignment.getAgencyId());
    assertEquals(1, assignment.getBusNumber());
    assertEquals("GH", assignment.getDepot());
  }

}
