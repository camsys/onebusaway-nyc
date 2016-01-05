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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.MultiCSVLogger;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.ServiceCode;

public class TestStifTripLoaderTest {
  //it's log, log, it's better than bad, it's good.
  final static Logger _log = Logger.getRootLogger();
  
  @BeforeClass
  public static void setUp() {
    BasicConfigurator.configure();
    _log.setLevel(Level.DEBUG);
  }
  
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
    
    //check if dao and stif loader concur, ie all trips from the map must accessible from the GTFS dao.
    for (AgencyAndId trip:trips) {
    	assertTrue(dao.getTripForId(trip) != null );
    }

  }
  
  @Test
  public void testNonRevenueFirstStop() throws IOException {
    String stifFile = "stif.b_0001__.416188.sun";
    String gtfsDir = "b1_gtfs";
    
    InputStream in = getClass().getResourceAsStream(stifFile);
    String gtfs = getClass().getResource(gtfsDir).getFile();

    GtfsReader reader = new GtfsReader();
    GtfsRelationalDaoImpl dao = new GtfsRelationalDaoImpl();
    reader.setEntityStore(dao);
    reader.setInputLocation(new File(gtfs));
    
    reader.run();

    StifTripLoader loader = new StifTripLoader();
    loader.setLogger(new MultiCSVLogger());
    loader.setGtfsDao(dao);
    
    _log.info("reading stif");
    loader.run(in, new File(stifFile));
    Map<String, List<AgencyAndId>> mapping = loader.getTripMapping();
    _log.info("done mapping stif");
    
    // get trip IDs by sign code. 
    // 4013 has a non-rev first stop.
    assertTrue(mapping.containsKey("4013"));
    List<AgencyAndId> trips = mapping.get("4013");
    
    //check if dao and stif loader concur, ie all trips from the map must accessible from the GTFS dao.
    for (AgencyAndId trip:trips) {
      assertTrue(dao.getTripForId(trip) != null );
    }

  }
  
  @Test
  public void testNonRevenueLastStop() throws IOException {
    String stifFile = "stif.q_0004__.516151.sun";
    String gtfsDir = "q4_gtfs";
    
    InputStream in = getClass().getResourceAsStream(stifFile);
    String gtfs = getClass().getResource(gtfsDir).getFile();

    GtfsReader reader = new GtfsReader();
    GtfsRelationalDaoImpl dao = new GtfsRelationalDaoImpl();
    reader.setEntityStore(dao);
    reader.setInputLocation(new File(gtfs));
    
    reader.run();

    StifTripLoader loader = new StifTripLoader();
    loader.setLogger(new MultiCSVLogger());
    loader.setGtfsDao(dao);
    
    loader.run(in, new File(stifFile));
    Map<String, List<AgencyAndId>> mapping = loader.getTripMapping();
    
    // 5041 has a non-rev last stop.
    assertTrue(mapping.containsKey("5041"));
    List<AgencyAndId> trips = mapping.get("5041");
    
    for (AgencyAndId trip:trips) {
      assertTrue(dao.getTripForId(trip) != null );
    }

  }
  
  // modified to use the 2016 q4 data, saving a couple hundred megs in the repo. 
  @Test
  public void testNonRevStopParsing() throws IOException {
    String stifFile = "stif.q_0004__.516151.sun";
    String gtfsDir = "q4_gtfs";
    
    InputStream in = getClass().getResourceAsStream(stifFile);
    String gtfs = getClass().getResource(gtfsDir).getFile();

    GtfsReader reader = new GtfsReader();
    GtfsRelationalDaoImpl dao = new GtfsRelationalDaoImpl();
    reader.setEntityStore(dao);
    reader.setInputLocation(new File(gtfs));
    reader.run();

    StifTripLoader loader = new StifTripLoader();
    loader.setLogger(new MultiCSVLogger());
    loader.setGtfsDao(dao);
    loader.run(in, new File(stifFile));
    
    // After parsing the STIF trip containing a non-revenue stop,
    // it should result in the following GTFS trip ids being mapped:
    //MTA NYCT_JA_A6-Sunday-001800_Q4_1 (first stop non-rev)
    //MTA NYCT_JA_A6-Sunday-056000_MISC_428 (last stop non-rev)
    assertTrue(
        loader.getNonRevenueStopDataByTripId().containsKey(AgencyAndId.convertFromString("MTA NYCT_JA_A6-Sunday-001800_Q4_1"))
        && loader.getNonRevenueStopDataByTripId().containsKey(AgencyAndId.convertFromString("MTA NYCT_JA_A6-Sunday-056000_MISC_428")));
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

  //@Test
  public void testNextOperatorDepot() throws IOException {
    InputStream in = getClass().getResourceAsStream("stif.q_0058o_.413663.wkd.open");
    assertNotNull(in);
    String gtfs = getClass().getResource("q58.zip").getFile();
    assertNotNull(gtfs);
    GtfsReader reader = new GtfsReader();
    GtfsRelationalDaoImpl dao = new GtfsRelationalDaoImpl();
    reader.setEntityStore(dao);
    reader.setInputLocation(new File(gtfs));
    reader.run();
    
    // verify gtfs loaded
    Collection<Trip> allTrips = dao.getAllTrips();
    assertTrue(allTrips.size() > 0);
    Collection<Trip> gTrips = dao.getAllTrips();

    StifTripLoader loader = new StifTripLoader();
    loader.setLogger(new MultiCSVLogger());
    loader.setGtfsDao(dao);
    loader.run(in, new File("stif.q_0058o_.413663.wkd.open"));
    assertTrue(loader.getTripsCount() > 0);
    List<StifTrip> strips = null;
    for (ServiceCode sc : loader.getRawStifData().keySet()) {
      strips = loader.getRawStifData().get(sc);
    }
    
    assertEquals("FP", strips.get(0).depot);
    assertEquals("FP", strips.get(0).nextTripOperatorDepot);
    assertEquals("CS", strips.get(1).depot);
    assertEquals("CS", strips.get(1).nextTripOperatorDepot);
    assertEquals("FP", strips.get(2).depot);
    assertEquals("FP", strips.get(2).nextTripOperatorDepot);
 
  }

}
