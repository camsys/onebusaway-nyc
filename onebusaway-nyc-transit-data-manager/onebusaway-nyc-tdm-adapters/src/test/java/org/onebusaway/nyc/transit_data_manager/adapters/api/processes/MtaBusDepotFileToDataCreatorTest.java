package org.onebusaway.nyc.transit_data_manager.adapters.api.processes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaBusDepotAssignment;

import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class MtaBusDepotFileToDataCreatorTest extends
    MtaBusDepotFileToDataCreator {

  @Test
  public void testLoadDepotAssignments() throws FileNotFoundException, IOException {
    setReader(new InputStreamReader(getClass().getResourceAsStream("/DOB_draft_depot_assignments.xml")));
    List<MtaBusDepotAssignment> assignments = loadDepotAssignments();
    assertNotNull(assignments);
    assertTrue(assignments.size() > 0);
    MtaBusDepotAssignment assignment = assignments.get(0);
    assertEquals(new Long(2188), assignment.getAgencyId());
    assertEquals(1009, assignment.getBusNumber());
    assertEquals("GH", assignment.getDepot());
  }

}
