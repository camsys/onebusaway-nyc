package org.onebusaway.nyc.transit_data_manager.importers;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaUtsCrewAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.CSVCrewAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.CrewAssignsInputConverter;

public class CSVCrewAssignsInputConverterTest {

  CrewAssignsInputConverter inConverter = null;

  @Before
  public void setup() throws URISyntaxException {
    ClassLoader classLoader = CSVCrewAssignsInputConverterTest.class.getClassLoader();
    //InputStream in = classLoader.getResourceAsStream("CrewAssignCSVSampleForTests.csv");
    File inputFile = new File(classLoader.getResource("CrewAssignCSVSampleForTests.csv").toURI());

    //Reader csvInputReader = new InputStreamReader(in);

    inConverter = new CSVCrewAssignsInputConverter(inputFile);
  }

  @Test
  public void testGetCrewAssignments() throws FileNotFoundException {

    List<MtaUtsCrewAssignment> crewAssignments = inConverter.getCrewAssignments();

    // Test that we get four results.
    assertEquals(4, crewAssignments.size());
  }

}
