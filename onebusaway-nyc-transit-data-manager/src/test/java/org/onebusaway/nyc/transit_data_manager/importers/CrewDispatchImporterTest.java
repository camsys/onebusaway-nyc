package org.onebusaway.nyc.transit_data_manager.importers;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.onebusaway.nyc.transit_data_manager.model.CrewDispatchSchedule;

import static org.junit.Assert.*;

/**
 * Demo test case.
 *
 */
public class CrewDispatchImporterTest {
	
	  @Test
	  public void importGoodFile() throws IOException {
		    ClassLoader classLoader = CrewDispatchImporterTest.class.getClassLoader();
		    InputStream in = classLoader.getResourceAsStream("good-crew-dispatch-file.txt");
		    assertNotNull("test file must be present", in);

		    CrewDispatchImporter importer = new CrewDispatchImporter();
		    importer.doImport(in);
		    CrewDispatchSchedule schedule = importer.getSchedule();
		    
		    assertNotNull(schedule);
		    
	  }
	

}
