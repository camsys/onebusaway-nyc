package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.MultiCSVLogger;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.StifTripLoader;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.impl.AbnormalStifDataLoggerImpl;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.impl.StifAggregatorImpl;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.impl.StifLoaderImpl;


public class StifAggregatorImplTest {
  final static Logger _log = Logger.getRootLogger();
  
  @Before
  public void setUp() throws Exception {
    BasicConfigurator.configure();
    _log.setLevel(Level.INFO);
  }

  
  @Test
  public void testComputeBlocksFromRuns() {
    StifAggregatorImpl agg = new StifAggregatorImpl();
    StifLoaderImpl sl = new StifLoaderImpl();
    
    InputStream in = getClass().getResourceAsStream("stif.q_0004__.516151.sun");
    String gtfs = getClass().getResource("q4_gtfs").getFile();

    GtfsReader reader = new GtfsReader();
    GtfsRelationalDaoImpl dao = new GtfsRelationalDaoImpl();
    reader.setEntityStore(dao);
    sl.setGtfsMutableRelationalDao(dao);
    
    try {
      reader.setInputLocation(new File(gtfs));
      reader.run();
    } catch (IOException e) {
      e.printStackTrace();
      fail("Exception when loading GTFS");
    }
    
    StifTripLoader loader = new StifTripLoader();
    loader.setLogger(new MultiCSVLogger());
    loader.setGtfsDao(dao);
    loader.run(in, new File("stif.q_0004__.516151.sun"));
    
    sl.setStifTripLoader(loader);
    agg.setStifLoader(sl);
       
    AbnormalStifDataLoggerImpl a = new AbnormalStifDataLoggerImpl();
    MultiCSVLogger logger = new MultiCSVLogger();
    a.setLogger(logger);
    
    agg.setAbnormalStifDataLoggerImpl(a);
    
    agg.computeBlocksFromRuns();
    
    assertEquals(174, agg.getMatchedGtfsTripsCount());
    assertEquals(1,agg.getRoutesWithTripsCount());
    assertEquals(0,agg.getUnmatchedTripsSize());
  }

}
