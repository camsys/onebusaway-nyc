package org.onebusaway.nyc.transit_data_manager.importers;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaBusDepotAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.BusDepotAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.XMLBusDepotAssignsInputConverter;

public class XMLBusDepotAssignsInputConverterTest {

  BusDepotAssignsInputConverter inConverter = null;

  @Before
  public void setup() throws URISyntaxException {
    ClassLoader classLoader = XMLBusDepotAssignsInputConverterTest.class.getClassLoader();
    
    File inputFile = new File(classLoader.getResource("BusDepotAssignXMLExample.xml").toURI());

    inConverter = new XMLBusDepotAssignsInputConverter(inputFile);
  }

  @Test
  public void testGetBusDepotAssignments() throws IOException {

    List<MtaBusDepotAssignment> assignments = inConverter.getBusDepotAssignments();

    // Test that we get four results.
    assertEquals(3, assignments.size());
  }
}
