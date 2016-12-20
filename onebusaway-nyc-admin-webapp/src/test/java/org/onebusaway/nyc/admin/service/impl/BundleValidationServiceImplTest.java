package org.onebusaway.nyc.admin.service.impl;

import static org.junit.Assert.*;

import org.onebusaway.nyc.admin.model.ServiceDateRange;
import org.onebusaway.nyc.admin.service.bundle.GtfsValidationService;
import org.onebusaway.nyc.admin.service.bundle.impl.BundleValidationServiceImpl;
import org.onebusaway.nyc.admin.service.bundle.impl.JavaGtfsValidationServiceImpl;
import org.onebusaway.nyc.admin.service.bundle.impl.PythonGtfsValidationServiceImpl;
import org.onebusaway.nyc.admin.util.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BundleValidationServiceImplTest {
  
  BundleValidationServiceImpl bundleValidationService;
	
  @Before
  public void setup(){
	  bundleValidationService = new BundleValidationServiceImpl();
  }

  @Test
  public void testGetServiceDateRange() throws Exception {
	InputStream input = this.getClass().getResourceAsStream(
		        "google_transit_staten_island.zip");
    assertNotNull(input);
    List<ServiceDateRange> ranges = bundleValidationService.getServiceDateRanges(input);
    assertNotNull(ranges);
    assertTrue(ranges.size() == 4);
    ServiceDateRange sdr0 = ranges.get(0);
    assertEquals("MTA NYCT", sdr0.getAgencyId());
    assertEquals(2012, sdr0.getStartDate().getYear());
    assertEquals(4, sdr0.getStartDate().getMonth());
    assertEquals(8, sdr0.getStartDate().getDay());
    assertEquals(2012, sdr0.getEndDate().getYear());
    assertEquals(7, sdr0.getEndDate().getMonth());
    assertEquals(7, sdr0.getEndDate().getDay());

  }

  @Test
  public void testCommonServiceDateRange() throws Exception {
	InputStream input = this.getClass().getResourceAsStream(
		        "google_transit_staten_island.zip");
    assertNotNull(input);
    List<ServiceDateRange> ranges = bundleValidationService.getServiceDateRanges(input);
    Map<String, List<ServiceDateRange>> map = bundleValidationService.getServiceDateRangesByAgencyId(ranges);
    ServiceDateRange sdr0 = map.get("MTA NYCT").get(0);
    assertEquals("MTA NYCT", sdr0.getAgencyId());
    assertEquals(2012, sdr0.getStartDate().getYear());
    assertEquals(4, sdr0.getStartDate().getMonth());
    assertEquals(8, sdr0.getStartDate().getDay());
    assertEquals(2012, sdr0.getEndDate().getYear());
    assertEquals(7, sdr0.getEndDate().getMonth());
    assertEquals(7, sdr0.getEndDate().getDay());

  }

  @Test
  public void testCommonServiceDateRangeAcrossGTFS() throws Exception {
    ArrayList<InputStream> inputs = new ArrayList<InputStream>();
    inputs.add(this.getClass().getResourceAsStream(
        "google_transit_staten_island.zip"));
    inputs.add(this.getClass().getResourceAsStream(
        "google_transit_manhattan.zip"));
    Map<String, List<ServiceDateRange>> map = bundleValidationService.getServiceDateRangesAcrossAllGtfs(inputs);
    ServiceDateRange sdr0 = map.get("MTA NYCT").get(0);

    assertEquals(2012, sdr0.getStartDate().getYear());
    assertEquals(4, sdr0.getStartDate().getMonth());
    assertEquals(8, sdr0.getStartDate().getDay());
    assertEquals(2012, sdr0.getEndDate().getYear());
    assertEquals(7, sdr0.getEndDate().getMonth());
    assertEquals(7, sdr0.getEndDate().getDay());

  }

  @Test
  public void testPythonValidateGtfs() throws Exception {
	GtfsValidationService validationService = new PythonGtfsValidationServiceImpl();
	bundleValidationService.setGtfsValidationService(validationService);
    InputStream source = this.getClass().getResourceAsStream(
        "gtfs-m34.zip");
    assertNotNull(source);
    String goodFeedFilename = getTmpDir() + File.separatorChar + "good_feed.zip";
    new FileUtils().copy(source, goodFeedFilename);
    assertTrue(new File(goodFeedFilename).exists());
    String emptyFeedFilename = getTmpDir() + File.separatorChar + "good_feed.zip";
    assertTrue(new File(emptyFeedFilename).exists());
    String feedFilename = getTmpDir() + File.separatorChar + "feed.html";
    int rc = bundleValidationService.installAndValidateGtfs(emptyFeedFilename, 
        feedFilename);
    // feedValidator exits with returnCode 1
    if (rc == 2) {
      throw new RuntimeException("please execute this command: 'sudo ln -s /usr/bin/python /usr/bin/python2.5'");
    }
    assertEquals(0, rc);
    File output = new File(feedFilename);
    assertTrue(output.exists());
    
  }
  
  @Test
  public void testJavaValidateGtfs() throws Exception {
	GtfsValidationService validationService = new JavaGtfsValidationServiceImpl();
	bundleValidationService.setGtfsValidationService(validationService);
    InputStream source = this.getClass().getResourceAsStream(
        "gtfs-m34.zip");
    assertNotNull(source);
    String goodFeedFilename = getTmpDir() + File.separatorChar + "good_feed.zip";
    new FileUtils().copy(source, goodFeedFilename);
    assertTrue(new File(goodFeedFilename).exists());
    String emptyFeedFilename = getTmpDir() + File.separatorChar + "good_feed.zip";
    assertTrue(new File(emptyFeedFilename).exists());
    String feedFilename = getTmpDir() + File.separatorChar + "feed.html";
    int rc = bundleValidationService.installAndValidateGtfs(emptyFeedFilename, 
        feedFilename);
    // feedValidator exits with returnCode 1
    if (rc == 2) {
      throw new RuntimeException("please confirm that you have Java 8 or greater installed'");
    }
    assertEquals(0, rc);
    File output = new File(feedFilename);
    assertTrue(output.exists());
    
  }
  
  private String getTmpDir() {
    return System.getProperty("java.io.tmpdir");
  }
}
