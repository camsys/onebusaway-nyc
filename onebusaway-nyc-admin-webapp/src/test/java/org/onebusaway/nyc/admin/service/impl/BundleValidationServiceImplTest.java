package org.onebusaway.nyc.admin.service.impl;

import static org.junit.Assert.*;

import org.onebusaway.nyc.admin.model.ServiceDateRange;

import org.junit.Test;

import java.io.InputStream;
import java.util.List;

public class BundleValidationServiceImplTest {

  @Test
  public void testGetServiceDateRange() throws Exception {
    BundleValidationServiceImpl impl = new BundleValidationServiceImpl();
    // load zip file
    InputStream input = this.getClass().getResourceAsStream("google_transit_staten_island.zip");
    assertNotNull(input);
    List<ServiceDateRange> ranges = impl.getServiceDateRanges(input);
    assertNotNull(ranges);
    assertTrue(ranges.size() == 4);
    ServiceDateRange sdr0 = ranges.get(0);
    assertEquals(2012, sdr0.getStartDate().getYear());
    assertEquals(4, sdr0.getStartDate().getMonth());
    assertEquals(8, sdr0.getStartDate().getDay());
    assertEquals(2012, sdr0.getEndDate().getYear());
    assertEquals(7, sdr0.getEndDate().getMonth());
    assertEquals(7, sdr0.getEndDate().getDay());
    
  }
  
  @Test
  public void testCommonServiceDateRange() throws Exception {
    BundleValidationServiceImpl impl = new BundleValidationServiceImpl();
    // load zip file
    InputStream input = this.getClass().getResourceAsStream("google_transit_staten_island.zip");
    assertNotNull(input);
    List<ServiceDateRange> ranges = impl.getServiceDateRanges(input);
    ServiceDateRange sdr0 = impl.getCommonServiceDateRange(ranges);
    assertEquals(2012, sdr0.getStartDate().getYear());
    assertEquals(4, sdr0.getStartDate().getMonth());
    assertEquals(8, sdr0.getStartDate().getDay());
    assertEquals(2012, sdr0.getEndDate().getYear());
    assertEquals(7, sdr0.getEndDate().getMonth());
    assertEquals(7, sdr0.getEndDate().getDay());
    
  }
}
