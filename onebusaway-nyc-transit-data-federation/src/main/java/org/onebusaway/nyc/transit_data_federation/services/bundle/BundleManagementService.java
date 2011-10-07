package org.onebusaway.nyc.transit_data_federation.services.bundle;

import org.onebusaway.nyc.transit_data_federation.impl.bundle.model.BundleItem;

public interface BundleManagementService {

  public void changeBundle(String bundleId) throws Exception;

  public boolean bundleWithIdExists(String bundleId);

  public BundleItem getBundleMetadataForBundleWithId(String bundleId);

  public BundleItem getCurrentBundleMetadata();

}

