package org.onebusaway.nyc.transit_data_manager.importers;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
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

    File inputFile = new File(classLoader.getResource("CrewAssignCSVSampleForTests.csv").toURI());

    inConverter = new CSVCrewAssignsInputConverter(inputFile);
  }

  @Test
  public void testGetCrewAssignments() throws FileNotFoundException {
    
    List<MtaUtsCrewAssignment> crewAssignments = inConverter.getCrewAssignments();

    if (5 != crewAssignments.size()) {
      for (MtaUtsCrewAssignment muca : crewAssignments) {
        System.out.println(muca.getPassNumberNumericPortion() + ":" + muca.getTimestamp());
      }
      
    }
    // Test that we get four results.
    assertEquals(5, crewAssignments.size());
  }

}
