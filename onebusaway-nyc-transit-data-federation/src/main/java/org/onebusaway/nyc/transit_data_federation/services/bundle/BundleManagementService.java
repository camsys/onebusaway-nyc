package org.onebusaway.nyc.transit_data_federation.services.bundle;

import org.onebusaway.nyc.transit_data_federation.model.bundle.BundleItem;

public interface BundleManagementService {
  
  public void changeBundle(String bundleId) throws Exception;

  public boolean bundleWithIdExists(String bundleId);

  public BundleItem getBundleMetadataForBundleWithId(String bundleId);

  public BundleItem getCurrentBundleMetadata();

}

