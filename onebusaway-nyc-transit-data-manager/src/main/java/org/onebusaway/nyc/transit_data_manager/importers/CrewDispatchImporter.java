package org.onebusaway.nyc.transit_data_manager.importers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.onebusaway.nyc.transit_data_manager.model.CrewDispatchSchedule;

/**
 * This is an example class only, and not necessarily meant to be long-lived or for production.
 *
 */
public class CrewDispatchImporter {
	
	public void doImport(InputStream in) throws IOException {
		// This is dummy code, we just want to read the input stream
		BufferedReader isr = new BufferedReader(new InputStreamReader(in));
		while (isr.readLine() != null) {
			
		}
	}

	public CrewDispatchSchedule getSchedule() {
		// TODO Auto-generated method stub
		return null;
	}

}
