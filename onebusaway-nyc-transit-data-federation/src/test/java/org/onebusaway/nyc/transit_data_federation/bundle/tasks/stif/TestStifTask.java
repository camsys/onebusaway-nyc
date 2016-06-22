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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.MultiCSVLogger;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.impl.StifAggregatorImpl;

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
	public void testDscToTripMap() throws IOException{
		assignAgency("MTA_NYCT", 0, 3);
		assignAgency("MTABC", 4, 98);
		assignDSC("7042", 0, 3);
		assignDSC("7041", 4, 52);
		assignDSC("7040", 53, 98);
		
		
		String[] testSetZero = {"dsc","agency_id","number_of_trips_in_stif","number_of_distinct_route_ids_in_gtfs"};
		String[] testSetOne =  {"7040","MTABC","46"};
		String[] testSetTwo =  {"7041","MTABC","49"};
		String[] testSetThree =  {"7042","MTA_NYCT","4"};
		ArrayList<String[]> expectedTestSet = new ArrayList<String[]>();
		expectedTestSet.add(testSetThree);
		expectedTestSet.add(testSetTwo);
		expectedTestSet.add(testSetOne);
		expectedTestSet.add(testSetZero);
		
		task.logDSCStatistics(dscToTripMap, tripToDscMap);
		
		String line;
		int count = 0;
		int expectedCount = 4;
		while((line = reader.readLine()) != null ){
			String[] cols = line.split(",");
			assertTrue("The line split length is incorrect / unexpected", cols.length == 4);
			boolean visited = false;
			for(String[] testcol : expectedTestSet){
				if(testcol[0].equals(cols[0])){
					visited = true;
					for(int i=1; i<testcol.length; i++){
						assertTrue("The column is unequal at pos in "+i+"in "+line +"expected "+ testcol[i], testcol[i].equals(cols[i]));
					}
				}
			}
			assertTrue("doesn't contain the line"+line, visited);
			count++;
		}
		assertEquals("Unexpected line count from outgoing file", String.valueOf(count), String.valueOf(expectedCount));

	}
	

	
	@Test
	public void testTruncateId() {
	  assertNull(StifAggregatorImpl.truncateId(null));
	  assertEquals("", StifAggregatorImpl.truncateId(""));
	  assertEquals("", StifAggregatorImpl.truncateId(" aeiouy"));
	  assertEquals("MTABC_CPPA4-CP_A4-Wkd-10-SDn_E_450082_61200_QM5_805_rphn_39918", StifAggregatorImpl.truncateId("MTABC_CPPA4-CP_A4-Weekday-10-SDon_E_ 450082_61200_QM5_805_orphan_39918"));
	  assertTrue(StifAggregatorImpl.truncateId("MTABC_CPPA4-CP_A4-Weekday-10-SDon_E_ 450082_61200_QM5_805_orphan_39918").length() < 64);
	}
	
	//@Test
	public void compareAgainstOldMethod() throws IOException{
		assignAgency("MTA_NYCT", 0, 49);
		assignAgency("MTABC", 50, 98);
		assignDSC("7042", 0, 3);
		assignDSC("7041", 4, 55);
		assignDSC("7040", 56, 98);
		task.logDSCStatistics(dscToTripMap, tripToDscMap);

		initialize();
		assignAgency("MTA_NYCT", 0, 49);
		assignAgency("MTABC", 50, 98);
		assignDSC("7042", 0, 3);
		assignDSC("7041", 4, 55);
		assignDSC("7040", 56, 98);
		oldMethod(dscToTripMap, tripToDscMap);

	}
	
	// assign dscs to mock trips. 
	private void assignDSC(String dsc, int start, int end){
		//start is the starting index of the mock trip with said dsc
		//end is the last index of the mock trip
		for(int i=start; i<=end; i++){
			tripToDscMap.put(hash.get(i), dsc);
			if (!dscToTripMap.containsKey(dsc))
				dscToTripMap.put(dsc, new LinkedList<AgencyAndId>());
			dscToTripMap.get(dsc).add(hash.get(i));
		}
	}

	//mock agency and id 
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