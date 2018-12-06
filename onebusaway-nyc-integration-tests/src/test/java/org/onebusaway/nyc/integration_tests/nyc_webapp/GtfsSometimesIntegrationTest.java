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
package org.onebusaway.nyc.integration_tests.nyc_webapp;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("unchecked")
public class GtfsSometimesIntegrationTest extends SiriIntegrationTestBase {

	private static final String json = "{ \"header\": { \"gtfs_realtime_version\": 1, \"feed_version_number\": 1, \"timestamp\": 1543595420, \"incrementality\": \"FULL_DATASET\" }, \"entity\": [ { \"service_change\": { \"table\": \"STOPS\", \"service_change_type\": \"ALTER\", \"affected_entity\": [ { \"stop_id\": \"303479\" } ], \"affected_field\": [ { \"stop_name\": \"NOSTRAND AV/FARRAGUT RD namechange\", \"stop_lat\": 40.635929, \"stop_lon\": -73.948063 } ], \"affected_dates\": [ { \"from\": \"2018-11-29\", \"to\": \"2018-12-08\" } ] } } ] } ";
	private boolean _isSetUp = false;

	public GtfsSometimesIntegrationTest() {
		super(null);
	}

	@Before
	public void setUp() throws Throwable {
		if (_isSetUp == false) {
			setBundle("2018Sept_Prod_r15_b03", "2018-11-30T16:20:00EST");
			resetAll();
			_isSetUp = true;
		}
	}

	@Test
	public void testStopForId() throws Throwable {
		loadRecords("4238-export-1.csv");
		HashMap<String, Object> response = getStopForIdResponse("MTA", "303479");
		assertEquals(1, getNumberOfMonitoredVehicleJourneys(response));

		File file = new File("target/feed.json");
		PrintWriter writer = new PrintWriter(file);
		writer.write(json);
		writer.close();

		Thread.sleep(30000);

		response = getStopForIdResponse("MTA", "303479");
		assertEquals(0, getNumberOfMonitoredVehicleJourneys(response));

		loadRecords("4238-export-2.csv");
		response = getStopForIdResponse("MTA", "303479");
		assertEquals(1, getNumberOfMonitoredVehicleJourneys(response));
	}

	private int getNumberOfMonitoredVehicleJourneys(HashMap<String, Object> response) {
		HashMap<String,Object> siri = (HashMap<String, Object>) response.get("siri");
		siri = (HashMap<String, Object>) siri.get("Siri");
		HashMap<String,Object> serviceDelivery = (HashMap<String, Object>) siri.get("ServiceDelivery");
		ArrayList<Object> stopMonitoringDelivery = (ArrayList<Object>) serviceDelivery.get("StopMonitoringDelivery");
		HashMap<String,Object> monitoredStopVisit = (HashMap<String,Object>)stopMonitoringDelivery.get(0);
		ArrayList<Object> mvjs = (ArrayList<Object>) monitoredStopVisit.get("MonitoredStopVisit");
		return mvjs.size();
	}

}
