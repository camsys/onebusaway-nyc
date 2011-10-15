package org.onebusaway.nyc.transit_data_federation.impl.bundle;

import static org.junit.Assert.assertEquals;

import org.onebusaway.nyc.transit_data_federation.model.bundle.BundleItem;
import org.onebusaway.utility.DateLibrary;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;

public class BundleManagementServiceImplTest extends BundleManagementServiceImpl {
  
  ArrayList<BundleItem> outputBundles = new ArrayList<BundleItem>();

  @Before
  public void setup() throws Exception {
    BundleItem a = new BundleItem();
    a.setId("test0");
    a.setServiceDateFrom(DateLibrary.getIso8601StringAsTime("2011-10-1T0:00:00-05:00"));
    a.setServiceDateTo(DateLibrary.getIso8601StringAsTime("2011-11-15T0:00:00-05:00"));
    outputBundles.add(a);

    BundleItem b = new BundleItem();
    b.setId("test1");
    b.setServiceDateFrom(DateLibrary.getIso8601StringAsTime("2011-11-16T0:00:00-05:00"));
    b.setServiceDateTo(DateLibrary.getIso8601StringAsTime("2011-12-31T0:00:00-05:00"));
    outputBundles.add(b);
    
    BundleItem c = new BundleItem();
    c.setId("test2");
    c.setServiceDateFrom(DateLibrary.getIso8601StringAsTime("2011-10-1T0:00:00-05:00"));
    c.setServiceDateTo(DateLibrary.getIso8601StringAsTime("2012-1-31T0:00:00-05:00"));
    outputBundles.add(c);
    
    Date date = DateLibrary.getIso8601StringAsTime("2011-10-14T10:00:00-05:00");
    super.setStandaloneMode(true);
    super.setTime(date);    
    super.setup();
  }
  
  @Override
  protected ArrayList<BundleItem> getBundleListFromLocalStore() throws Exception {
    return outputBundles;
  }
  
  @Override
  public void changeBundle(String bundleId) throws Exception {
    _currentBundleId = bundleId;
  }
  
  @Test
  public void testBundleValidityCheck() throws Exception {
    Date date = DateLibrary.getIso8601StringAsTime("2011-10-14T10:00:00-05:00");
    super.setTime(date);
    super.refreshValidBundleList();

    assertEquals(super.bundleWithIdExists("test0"), true);
    assertEquals(super.bundleWithIdExists("test1"), false);    
    assertEquals(super.bundleWithIdExists("test2"), true);
  }

  @Test
  public void testBundleSwitching() throws Exception {
    Date date = DateLibrary.getIso8601StringAsTime("2011-10-14T10:00:00-05:00");
    super.setTime(date);
    super.reevaluateBundleAssignment();

    assertEquals(super.getCurrentBundleMetadata().getId(), "test2");

    date = DateLibrary.getIso8601StringAsTime("2012-1-14T10:00:00-05:00");
    super.setTime(date);
    super.reevaluateBundleAssignment();

    assertEquals(super.getCurrentBundleMetadata().getId(), "test2");
  }

  @Test
  public void testBundleSwitchingWhenTwoAreActive() throws Exception {

    super.setup();
    
    Date date = DateLibrary.getIso8601StringAsTime("2011-10-14T10:00:00-05:00");
    super.setTime(date);
    super.reevaluateBundleAssignment();

    assertEquals(super.getCurrentBundleMetadata().getId(), "test2");
  }
}
