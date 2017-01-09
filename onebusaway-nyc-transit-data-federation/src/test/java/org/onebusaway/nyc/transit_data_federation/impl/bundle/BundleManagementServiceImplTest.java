package org.onebusaway.nyc.transit_data_federation.impl.bundle;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.nyc.transit_data_federation.model.bundle.BundleItem;
import org.onebusaway.nyc.transit_data_federation.services.bundle.BundleStoreService;
import org.onebusaway.utility.DateLibrary;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class BundleManagementServiceImplTest extends BundleManagementServiceImpl {
  
  @Mock
  private BundleStoreService mockBundleStoreService;

  @Before
  public void setup() throws Exception {
    List<BundleItem> bundles = new ArrayList<BundleItem>();    

    BundleItem a = new BundleItem();
    a.setId("test0");
    a.setServiceDateFrom(ServiceDate.parseString("20110101"));
    a.setServiceDateTo(ServiceDate.parseString("20110601"));
    a.setUpdated(new DateTime(DateLibrary.getIso8601StringAsTime("2010-12-20T0:00:00-00:00")));
    bundles.add(a);

    BundleItem b = new BundleItem();
    b.setId("test1");
    b.setServiceDateFrom(ServiceDate.parseString("20110115"));
    b.setServiceDateTo(ServiceDate.parseString("20110601"));
    b.setUpdated(new DateTime(DateLibrary.getIso8601StringAsTime("2011-1-14T0:00:00-00:00")));
    bundles.add(b);
    
    BundleItem c = new BundleItem();
    c.setId("test2");
    c.setServiceDateFrom(ServiceDate.parseString("20110602"));
    c.setServiceDateTo(ServiceDate.parseString("20111231"));
    c.setUpdated(new DateTime(DateLibrary.getIso8601StringAsTime("2011-6-10T0:00:00-00:00")));
    bundles.add(c);
  
    when(mockBundleStoreService.getBundles())
      .thenReturn(bundles);
    
    _bundleStore = mockBundleStoreService;
    
    super.discoverBundles();
  }
  
  @Override
  public void changeBundle(String bundleId) throws Exception {
    _currentBundleId = bundleId;
  }
  
  
  @Test
  public void testBundleValidityCheck() throws Exception {
    super.setServiceDate(ServiceDate.parseString("20110102"));
    super.refreshApplicableBundles();
    
    // assertEquals(super.bundleWithIdExists("test0"), true);
    // assertEquals(super.bundleWithIdExists("test1"), false);
    // assertEquals(super.bundleWithIdExists("test2"), false);    
  }
  

  @Test
  public void testBundleValidityCheck2() throws Exception {
    super.setServiceDate(ServiceDate.parseString("20110125"));
    super.refreshApplicableBundles();

    // assertEquals(super.bundleWithIdExists("test0"), true);
    // assertEquals(super.bundleWithIdExists("test1"), true);
    // assertEquals(super.bundleWithIdExists("test2"), false);    
  }
  

  @Test
  public void testBundleValidityCheck3() throws Exception {
    super.setServiceDate(ServiceDate.parseString("20110615"));
    super.refreshApplicableBundles();

    // assertEquals(super.bundleWithIdExists("test0"), false);
    // assertEquals(super.bundleWithIdExists("test1"), false);
    // assertEquals(super.bundleWithIdExists("test2"), true);    
  }
  
  
  @Test
  public void testInclusiveBundleValidityCheck() throws Exception {
    super.setServiceDate(ServiceDate.parseString("20110601"));
    super.refreshApplicableBundles();

    // assertEquals(super.bundleWithIdExists("test0"), true);
    // assertEquals(super.bundleWithIdExists("test1"), true);
    // assertEquals(super.bundleWithIdExists("test2"), false);    
  }

  @Test
  public void testInclusiveBundleValidityCheck2() throws Exception {
    super.setServiceDate(ServiceDate.parseString("20110602"));
    super.refreshApplicableBundles();

    // assertEquals(super.bundleWithIdExists("test0"), false);
    // assertEquals(super.bundleWithIdExists("test1"), false);
    // assertEquals(super.bundleWithIdExists("test2"), true);    
  }

  
  @Test
  public void testBundleInclusiveSwitch() throws Exception {
    super.setServiceDate(ServiceDate.parseString("20110601"));
    super.refreshApplicableBundles();

    super.reevaluateBundleAssignment();

    assertEquals(super.getCurrentBundleMetadata().getId(), "test1");
  }

  @Test
  public void testBundleInclusiveSwitch2() throws Exception {
    super.setServiceDate(ServiceDate.parseString("20110602"));
    super.refreshApplicableBundles();

    super.reevaluateBundleAssignment();

    assertEquals(super.getCurrentBundleMetadata().getId(), "test2");
  }


  @Test
  public void testBundleSwitching() throws Exception {
    super.setServiceDate(ServiceDate.parseString("20110101"));
    super.refreshApplicableBundles();

    super.reevaluateBundleAssignment();

    assertEquals(super.getCurrentBundleMetadata().getId(), "test0");

    super.setServiceDate(ServiceDate.parseString("20110715"));
    super.refreshApplicableBundles();

    super.reevaluateBundleAssignment();

    assertEquals(super.getCurrentBundleMetadata().getId(), "test2");
  }
  
  
  @Test
  public void testBundleSwitchingWhenTwoAreActive() throws Exception {
    super.setServiceDate(ServiceDate.parseString("20110130"));
    super.refreshApplicableBundles();

    super.reevaluateBundleAssignment();

    assertEquals(super.getCurrentBundleMetadata().getId(), "test1");
  }
  
  @Test
  public void testNextBundleSwitchTime(){
	  
	  super.setBundleSwitchFrequencyHour(1);
	  
	  Date currentDateTime = new Date(1483986900000L); // Mon, 09 Jan 2017 18:35:00 GMT
	  Date expectedBundleSwitchTime = new Date(1483988401000L); // Mon, 09 Jan 2017 19:00:01 GMT
	  Date actualBundleSwitchTime = super.getNextBundleSwitchTime(currentDateTime); 
	  assertEquals(expectedBundleSwitchTime, actualBundleSwitchTime);
	  
	  // Right at the hour
	  currentDateTime = new Date(1484078400000L); // Mon, 10 Jan 2017 20:00:00 GMT
	  expectedBundleSwitchTime = new Date(1484082001000L); // Mon, 10 Jan 2017 21:00:01 GMT
	  actualBundleSwitchTime = super.getNextBundleSwitchTime(currentDateTime); 
	  assertEquals(expectedBundleSwitchTime, actualBundleSwitchTime);
	  
	  // Right at the end of the hour
	  currentDateTime = new Date(1484164799000L); // Wed, 11 Jan 2017 19:59:59 GMT
	  expectedBundleSwitchTime = new Date(1484164801000L); // Wed, 11 Jan 2017 20:00:01 GMT
	  actualBundleSwitchTime = super.getNextBundleSwitchTime(currentDateTime); 
	  assertEquals(expectedBundleSwitchTime, actualBundleSwitchTime);
	  
	  // 2 seconds before next hour
	  currentDateTime = new Date(1484251198000L); // Thu, 12 Jan 2017 19:59:58 GMT
	  expectedBundleSwitchTime = new Date(1484251201000L); // Thu, 12 Jan 2017 20:00:01 GMT
	  actualBundleSwitchTime = super.getNextBundleSwitchTime(currentDateTime); 
	  assertEquals(expectedBundleSwitchTime, actualBundleSwitchTime);
  }
  
  @Test
  public void testNextBundleDiscoveryTime(){
	  
	  // Test 15 min Frequency 
	  super.setBundleDiscoveryFrequencyMin(15);
	  
	  Date currentDateTime = new Date(1483986900000L); // Mon, 09 Jan 2017 18:35:00 GMT
	  Date expectedBundleDiscoveryTime = new Date(1483987500000L); // Mon, 09 Jan 2017 18:45:00 GMT
	  Date actualBundleDiscoveryTime = super.getNextBundleDiscoveryTime(currentDateTime); 
	  assertEquals(expectedBundleDiscoveryTime, actualBundleDiscoveryTime);

	  currentDateTime = new Date(1483984860000L); // Mon, 09 Jan 2017 18:01:00 GMT
	  expectedBundleDiscoveryTime = new Date(1483985700000L); // Mon, 09 Jan 2017 18:15:00 GMT
	  actualBundleDiscoveryTime = super.getNextBundleDiscoveryTime(currentDateTime); 
	  assertEquals(expectedBundleDiscoveryTime, actualBundleDiscoveryTime);
	  
	  currentDateTime = new Date(1483988280000L); // Mon, 09 Jan 2017 18:58:00 GMT
	  expectedBundleDiscoveryTime = new Date(1483988400000L); // Mon, 09 Jan 2017 19:00:00 GMT
	  actualBundleDiscoveryTime = super.getNextBundleDiscoveryTime(currentDateTime); 
	  assertEquals(expectedBundleDiscoveryTime, actualBundleDiscoveryTime);
	  
	  
	  // Test 5 min Frequency 
	  super.setBundleDiscoveryFrequencyMin(5);
	  
	  currentDateTime = new Date(1483986540000L); // Mon, 09 Jan 2017 18:29:00 GMT
	  expectedBundleDiscoveryTime = new Date(1483986600000L); // Mon, 09 Jan 2017 18:30:00 GMT
	  actualBundleDiscoveryTime = super.getNextBundleDiscoveryTime(currentDateTime); 
	  assertEquals(expectedBundleDiscoveryTime, actualBundleDiscoveryTime);
	  
	  currentDateTime = new Date(1483986600000L); // Mon, 09 Jan 2017 18:30:00 GMT
	  expectedBundleDiscoveryTime = new Date(1483986900000L); // Mon, 09 Jan 2017 18:35:00 GMT
	  actualBundleDiscoveryTime = super.getNextBundleDiscoveryTime(currentDateTime); 
	  assertEquals(expectedBundleDiscoveryTime, actualBundleDiscoveryTime);
	  
	  
	  // Test 29 min Frequency
	  super.setBundleDiscoveryFrequencyMin(29);
	  
	  currentDateTime = new Date(1483988280000L); // Mon, 09 Jan 2017 18:58:00 GMT
	  expectedBundleDiscoveryTime = new Date(1483990020000L); // Mon, 09 Jan 2017 19:27:00 GMT
	  actualBundleDiscoveryTime = super.getNextBundleDiscoveryTime(currentDateTime); 
	  assertEquals(expectedBundleDiscoveryTime, actualBundleDiscoveryTime);
  }
  
}
