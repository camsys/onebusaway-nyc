package org.onebusaway.nyc.transit_data_manager.importers;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaBusDepotAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.BusDepotAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.XMLBusDepotAssignsInputConverter;

public class XMLBusDepotAssignsInputConverterTest {

  BusDepotAssignsInputConverter inConverter = null;

  @Before
  public void setup() {
    ClassLoader classLoader = XMLBusDepotAssignsInputConverterTest.class.getClassLoader();
    InputStream in = classLoader.getResourceAsStream("BusDepotAssignXMLExample.xml");

    Reader csvInputReader = new InputStreamReader(in);

    inConverter = new XMLBusDepotAssignsInputConverter(csvInputReader);
  }

  @Test
  public void testGetBusDepotAssignments() {

    List<MtaBusDepotAssignment> assignments = inConverter.getBusDepotAssignments();

    // Test that we get four results.
    assertEquals(3, assignments.size());
  }
}
