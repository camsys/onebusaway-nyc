package org.onebusaway.nyc.transit_data_federation.services.bundle;

import org.onebusaway.nyc.transit_data_federation.model.bundle.BundleItem;

import java.util.List;
import java.util.concurrent.Future;

/**
 * A service to manage bundles over time.
 * 
 * @author jmaki
 *
 */
@SuppressWarnings("rawtypes") 
public interface BundleManagementService {
  
  public void changeBundle(String bundleId) throws Exception;

  public BundleItem getCurrentBundleMetadata();

  public List<BundleItem> getAllKnownBundles();

  public boolean bundleWithIdExists(String bundleId);

  // is bundle finished loading? 
  public Boolean bundleIsReady();

  // thread reference keepers
  public void registerInferenceProcessingThread(Future thread);
  
}

