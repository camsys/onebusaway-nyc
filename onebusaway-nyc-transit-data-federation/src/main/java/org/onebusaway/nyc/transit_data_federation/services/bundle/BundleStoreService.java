package org.onebusaway.nyc.transit_data_federation.services.bundle;

import org.onebusaway.nyc.transit_data_federation.model.bundle.BundleItem;

import java.util.List;

public interface BundleStoreService {
  
  public List<BundleItem> getBundles() throws Exception;
  
}

