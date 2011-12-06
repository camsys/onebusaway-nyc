package org.onebusaway.nyc.transit_data_federation.services.bundle;

import org.onebusaway.nyc.transit_data_federation.model.bundle.BundleItem;

import java.util.List;

public interface BundleManagementService {
  
  public void changeBundle(String bundleId) throws Exception;

  public BundleItem getCurrentBundleMetadata();

  public List<BundleItem> getAllKnownBundles();

  public boolean bundleWithIdExists(String bundleId);

}

