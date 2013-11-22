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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.MultiCSVLogger;

public class TestStifTripLoaderTest {
  @Test
  public void testLoader() throws IOException {
    InputStream in = getClass().getResourceAsStream("stif.m_0014__.210186.sun");
    String gtfs = getClass().getResource("m14.zip").getFile();

    GtfsReader reader = new GtfsReader();
    GtfsRelationalDaoImpl dao = new GtfsRelationalDaoImpl();
    reader.setEntityStore(dao);
    reader.setInputLocation(new File(gtfs));
    reader.run();

    StifTripLoader loader = new StifTripLoader();
    loader.setLogger(new MultiCSVLogger());
    loader.setGtfsDao(dao);
    loader.run(in, new File("stif.m_0014__.210186.sun"));
    Map<String, List<AgencyAndId>> mapping = loader.getTripMapping();
    assertTrue(mapping.containsKey("140"));
    List<AgencyAndId> trips = mapping.get("140");
    AgencyAndId tripId = trips.get(0);
    Trip trip = dao.getTripForId(tripId);
    assertEquals(new AgencyAndId("MTA NYCT",
        "20100627DA_003000_M14AD_0001_M14AD_1"), trip.getId());
  }
  
  @Test
  public void testNonRevStopParsing() throws IOException {
    InputStream in = getClass().getResourceAsStream("stif.q_0004__.513032.wkd.open.modified");
    String gtfs = getClass().getResource("GTFS_SURFACE_Q_20130106_REV201303111037").getFile();

    GtfsReader reader = new GtfsReader();
    GtfsRelationalDaoImpl dao = new GtfsRelationalDaoImpl();
    reader.setEntityStore(dao);
    reader.setInputLocation(new File(gtfs));
    reader.run();

    StifTripLoader loader = new StifTripLoader();
    loader.setLogger(new MultiCSVLogger());
    loader.setGtfsDao(dao);
    loader.run(in, new File("stif.q_0004__.513032.wkd.open.modified"));
    
    // After parsing the STIF trip containing a non-revenue stop,
    // it should result in the following GTFS trip ids being mapped:
    //MTA NYCT_20130106EA_001800_Q04_0146_Q4_1
    //MTA NYCT_20130106EE_001800_Q04_0146_Q4_1
    assertTrue(
        loader.getNonRevenueStopDataByTripId().containsKey(AgencyAndId.convertFromString("MTA NYCT_20130106EA_001800_Q04_0146_Q4_1"))
        && loader.getNonRevenueStopDataByTripId().containsKey(AgencyAndId.convertFromString("MTA NYCT_20130106EE_001800_Q04_0146_Q4_1")));
  }
  @Test
  public void testNonRevStopParsingB42EdgeCase() throws IOException {
    InputStream in = getClass().getResourceAsStream("stif.b_0042__.413422.sat");
    String gtfs = getClass().getResource("GTFS_SURFACE_B_42_413421").getFile();

    GtfsReader reader = new GtfsReader();
    GtfsRelationalDaoImpl dao = new GtfsRelationalDaoImpl();
    reader.setEntityStore(dao);
    reader.setInputLocation(new File(gtfs));
    reader.run();

    StifTripLoader loader = new StifTripLoader();
    loader.setLogger(new MultiCSVLogger());
    loader.setGtfsDao(dao);
    loader.run(in, new File("stif.b_0042__.413422.sat"));
    
    // After parsing the STIF trip containing a non-revenue stop,
    // it should result in the following GTFS trip ids being mapped:
    //MTA NYCT_20130106EA_001800_Q04_0146_Q4_1
    //MTA NYCT_20130106EE_001800_Q04_0146_Q4_1
    assertTrue(loader.getNonRevenueStopDataByTripId().containsKey(AgencyAndId.convertFromString("MTA NYCT_EN_C3-Saturday-000000_B42_1")));
    assertTrue(loader.getNonRevenueStopDataByTripId().containsKey(AgencyAndId.convertFromString("MTA NYCT_EN_C3-Saturday-001000_B42_1")));
    
  }

  @Test
  public void testNextOperatorDepot() throws IOException {
    InputStream in = getClass().getResourceAsStream("stif.q_0058o__.413663.wkd.open");
    String gtfs = getClass().getResource("m14.zip").getFile();

    GtfsReader reader = new GtfsReader();
    GtfsRelationalDaoImpl dao = new GtfsRelationalDaoImpl();
    reader.setEntityStore(dao);
    reader.setInputLocation(new File(gtfs));
    reader.run();

    StifTripLoader loader = new StifTripLoader();
    loader.setLogger(new MultiCSVLogger());
    loader.setGtfsDao(dao);
    loader.run(in, new File("stif.q_0058o__.413663.wkd.open"));
    Map<String, List<AgencyAndId>> mapping = loader.getTripMapping();
    
    
    assertTrue(mapping.containsKey("5588"));
    List<AgencyAndId> trips = mapping.get("5588");
    for (AgencyAndId tripId : trips) {
      RawRunData rrd = loader.getRawRunDataByTrip().get(tripId);
      assertEquals("FP", rrd.getDepotCode());
    }
  }

}
