/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.MultiCSVLogger;
import static org.junit.Assert.*;

public class TestStifTask {

	StifTask task = new StifTask();
	MultiCSVLogger csvLogger;
	File file = new File("dsc_statistics.csv"); 
	BufferedReader reader;
	Map<String, List<AgencyAndId>> dscToTripMap;
	Map<AgencyAndId, String> tripToDscMap;
	HashMap<Integer, AgencyAndId> hash;
	String currentLine;

	@Before
	public void initialize() throws IOException{
		hash = new HashMap<Integer, AgencyAndId>();
		csvLogger = new MultiCSVLogger();
		file.delete();
		file.createNewFile();
		task.setLogger(csvLogger);
		reader = new BufferedReader(new FileReader(file));
		dscToTripMap = new HashMap<String, List<AgencyAndId>>();
		tripToDscMap = new HashMap<AgencyAndId, String>();
	}

	@Test
	public void testStifTask1() throws IOException{
		assignAgency("MTA_NYCT", 0, 3);
		assignAgency("MTABC", 4, 98);
		assignDSC("7042", 0, 3);
		assignDSC("7041", 4, 55);
		assignDSC("7040", 56, 98);
		task.logDSCStatistics(dscToTripMap, tripToDscMap);
		
		assertEquals(reader.readLine() , ( "dsc,agency_id,number_of_trips_in_stif,number_of_distinct_route_ids_in_gtfs" ));
		String[] column;
		column = reader.readLine().split(",");
		assertEquals("7042", column[0]);
		assertEquals("MTA_NYCT", column[1]);
		assertEquals("4", column[2]);
		column = reader.readLine().split(",");
		assertEquals("7041", column[0]);
		assertEquals("MTABC", column[1]);
		assertEquals("52", column[2]);
		column = reader.readLine().split(",");
		assertEquals("7040", column[0]);
		assertEquals("MTABC", column[1]);
		assertEquals("43", column[2]);
	}
	
	@Test
	public void testStifTask2() throws IOException{
		assignAgency("MTA_NYCT", 0, 55);
		assignAgency("MTABC", 56, 98);
		assignDSC("7042", 0, 3);
		assignDSC("7041", 4, 55);
		assignDSC("7040", 56, 98);
		task.logDSCStatistics(dscToTripMap, tripToDscMap);

		assertEquals(reader.readLine() , ( "dsc,agency_id,number_of_trips_in_stif,number_of_distinct_route_ids_in_gtfs" ));
		String[] column;
		column = reader.readLine().split(",");
		assertEquals("7042", column[0]);
		assertEquals("MTA_NYCT", column[1]);
		assertEquals("4", column[2]);
		column = reader.readLine().split(",");
		assertEquals("7041", column[0]);
		assertEquals("MTA_NYCT", column[1]);
		assertEquals("52", column[2]);
		column = reader.readLine().split(",");
		assertEquals("7040", column[0]);
		assertEquals("MTABC", column[1]);
		assertEquals("43", column[2]);
	}
	
	@Test
	public void testStifTask3() throws IOException{
		assignAgency("MTA_NYCT", 0, 98);
		assignDSC("7042", 0, 3);
		assignDSC("7041", 4, 55);
		assignDSC("7040", 56, 98);
		task.logDSCStatistics(dscToTripMap, tripToDscMap);
		
		assertEquals(reader.readLine() , ( "dsc,agency_id,number_of_trips_in_stif,number_of_distinct_route_ids_in_gtfs" ));
		String[] column;
		column = reader.readLine().split(",");
		assertEquals("7042", column[0]);
		assertEquals("MTA_NYCT", column[1]);
		assertEquals("4", column[2]);
		column = reader.readLine().split(",");
		assertEquals("7041", column[0]);
		assertEquals("MTA_NYCT", column[1]);
		assertEquals("52", column[2]);
		column = reader.readLine().split(",");
		assertEquals("7040", column[0]);
		assertEquals("MTA_NYCT", column[1]);
		assertEquals("43", column[2]);
	}
	
	@Test
	public void testStifTask4() throws IOException{
		assignAgency("MTA_NYCT", 0, 49);
		assignAgency("MTABC", 5, 98);
		assignDSC("7042", 0, 3);
		assignDSC("7041", 4, 55);
		assignDSC("7040", 56, 98);
		task.logDSCStatistics(dscToTripMap, tripToDscMap);

		assertEquals(reader.readLine() , ( "dsc,agency_id,number_of_trips_in_stif,number_of_distinct_route_ids_in_gtfs" ));
		String[] column;
		column = reader.readLine().split(",");
		assertEquals("7042", column[0]);
		assertEquals("MTA_NYCT", column[1]);
		assertEquals("4", column[2]);
		column = reader.readLine().split(",");
		assertEquals("7041", column[0]);
		assertEquals("MTA_NYCT", column[1]);
		assertEquals("52", column[2]);
		column = reader.readLine().split(",");
		assertEquals("7041", column[0]);
		assertEquals("MTABC", column[1]);
		assertEquals("52", column[2]);
		column = reader.readLine().split(",");
		assertEquals("7040", column[0]);
		assertEquals("MTABC", column[1]);
		assertEquals("43", column[2]);
	}
	
	//@Test
	public void compareAgainstOldMethod() throws IOException{
		assignAgency("MTA_NYCT", 0, 49);
		assignAgency("MTABC", 50, 98);
		assignDSC("7042", 0, 3);
		assignDSC("7041", 4, 55);
		assignDSC("7040", 56, 98);
		task.logDSCStatistics(dscToTripMap, tripToDscMap);
		
		while ((currentLine = reader.readLine()) != null){
			System.out.println(currentLine);
		}
		
		initialize();
		assignAgency("MTA_NYCT", 0, 49);
		assignAgency("MTABC", 50, 98);
		assignDSC("7042", 0, 3);
		assignDSC("7041", 4, 55);
		assignDSC("7040", 56, 98);
		oldMethod(dscToTripMap, tripToDscMap);
		System.out.println("\noldMethod: ");
		while ((currentLine = reader.readLine()) != null){
			System.out.println(currentLine);
		}
	}

	private void assignDSC(String dsc, int start, int end){
		for(int i=start; i<=end; i++){
			tripToDscMap.put(hash.get(i), dsc);
			if (!dscToTripMap.containsKey(dsc))
				dscToTripMap.put(dsc, new LinkedList<AgencyAndId>());
			dscToTripMap.get(dsc).add(hash.get(i));
		}
	}
	private void assignAgency(String agency, int start, int end){
		for (int i=start; i<end; i++){
			hash.put(i,new AgencyAndId(agency, Integer.toString(i)));
		}
	}

	private void oldMethod(Map<String, List<AgencyAndId>> dscToTripMap, Map<AgencyAndId, String> tripToDscMap) {
		csvLogger.header("dsc_statistics.csv","dsc,number_of_trips_in_stif,number_of_distinct_route_ids_in_gtfs");
		for (Map.Entry<String, List<AgencyAndId>> entry : dscToTripMap.entrySet()) {
			String destinationSignCode = entry.getKey();
			List<AgencyAndId> tripIds = entry.getValue();
			csvLogger.log("dsc_statistics.csv", destinationSignCode, tripIds.size(), 0);
		} 
	}
}