package org.onebusaway.nyc.transit_data_manager.importers;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.onebusaway.nyc.transit_data_manager.model.MtaUtsCrewAssignment;

public class CSVCrewAssignsInputConverterTest {

  CrewAssignsInputConverter inConverter = null;

  @Before
  public void setup() {
    ClassLoader classLoader = CSVCrewAssignsInputConverterTest.class.getClassLoader();
    InputStream in = classLoader.getResourceAsStream("CrewAssignCSVSampleForTests.csv");

    Reader csvInputReader = new InputStreamReader(in);

    inConverter = new CSVCrewAssignsInputConverter(csvInputReader);
  }

  @Test
  public void testGetCrewAssignments() {

    List<MtaUtsCrewAssignment> crewAssignments = inConverter.getCrewAssignments();

    // Test that we get four results.
    assertEquals(4, crewAssignments.size());
  }

}
