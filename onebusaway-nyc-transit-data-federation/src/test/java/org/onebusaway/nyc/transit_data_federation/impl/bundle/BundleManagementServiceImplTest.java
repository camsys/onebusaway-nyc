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
    
    assertEquals(super.bundleWithIdExists("test0"), true);
    assertEquals(super.bundleWithIdExists("test1"), false);
    assertEquals(super.bundleWithIdExists("test2"), false);    
  }
  

  @Test
  public void testBundleValidityCheck2() throws Exception {
    super.setServiceDate(ServiceDate.parseString("20110125"));
    super.refreshApplicableBundles();

    assertEquals(super.bundleWithIdExists("test0"), true);
    assertEquals(super.bundleWithIdExists("test1"), true);
    assertEquals(super.bundleWithIdExists("test2"), false);    
  }
  

  @Test
  public void testBundleValidityCheck3() throws Exception {
    super.setServiceDate(ServiceDate.parseString("20110615"));
    super.refreshApplicableBundles();

    assertEquals(super.bundleWithIdExists("test0"), false);
    assertEquals(super.bundleWithIdExists("test1"), false);
    assertEquals(super.bundleWithIdExists("test2"), true);    
  }
  
  
  @Test
  public void testInclusiveBundleValidityCheck() throws Exception {
    super.setServiceDate(ServiceDate.parseString("20110601"));
    super.refreshApplicableBundles();

    assertEquals(super.bundleWithIdExists("test0"), true);
    assertEquals(super.bundleWithIdExists("test1"), true);
    assertEquals(super.bundleWithIdExists("test2"), false);    
  }

  @Test
  public void testInclusiveBundleValidityCheck2() throws Exception {
    super.setServiceDate(ServiceDate.parseString("20110602"));
    super.refreshApplicableBundles();

    assertEquals(super.bundleWithIdExists("test0"), false);
    assertEquals(super.bundleWithIdExists("test1"), false);
    assertEquals(super.bundleWithIdExists("test2"), true);    
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
}
