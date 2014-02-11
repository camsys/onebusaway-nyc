package org.onebusaway.nyc.transit_data_manager.bundle;

import org.onebusaway.nyc.transit_data_federation.impl.bundle.BundleManagementServiceImpl;
import org.onebusaway.nyc.transit_data_federation.impl.bundle.LocalBundleStoreImpl;
import org.onebusaway.nyc.transit_data_federation.impl.bundle.TDMBundleStoreImpl;
import org.onebusaway.nyc.util.configuration.ConfigurationServiceClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.annotation.PostConstruct;

public class TDMBundleManagementServiceImpl extends BundleManagementServiceImpl {

  private static Logger _log = LoggerFactory.getLogger(TDMBundleManagementServiceImpl.class);

  @Autowired
  private ConfigurationServiceClient _apiLibrary;

  @Autowired
  private ThreadPoolTaskScheduler _taskScheduler;

  @PostConstruct
  @Override
  protected void setup() throws Exception {
    /*
     * Place startup on background thread to prevent deadlocks on startup.
     */
    InitThread thread = new InitThread();
    new Thread(thread).start();
  }
  
  @Override
  protected void timingHook() {
    _log.info("sleeping to stabilize");
    try {
          /*
           * TODO:  long term goal of not needing this at all
           * RestApiLibrary read timeout may have solved this issue
           */
          Thread.sleep(10 * 1000);  
        } catch (InterruptedException ie) {
          return;
        }
    _log.info("end sleep");
	}
  
  private class InitThread implements Runnable {

    @Override
    public void run() {

      try {
        final int SLEEP_TIME = 20 * 1000; // TODO:  long term goal of not needing this at all
        _log.info("init thread sleeping " + SLEEP_TIME + " on startup");
        /*
         * Initial bundle load is already in progress due to other TDM
         * dependencies -- allow that load to complete before we re-load. This
         * value can be very small but is currently large for safety.
         * TODO:  long term goal of not needing this at all
         * RestApiLibrary read timeout may have solved this issue
         */

        Thread.sleep(SLEEP_TIME);
        _log.info("thread resuming");
      } catch (InterruptedException e) {
        // assuming server was killed, exit
        return;
      }

      _log.info("building initial bundle store");
      if (getStandaloneMode() == true) {
        _bundleStore = new LocalBundleStoreImpl(getBundleStoreRoot());
      } else {
        _bundleStore = new TDMBundleStoreImpl(getBundleStoreRoot(), _apiLibrary);
      }

      try {
        discoverBundles();
        refreshApplicableBundles();
        reevaluateBundleAssignment();
      } catch (Exception e) {
        _log.error("thread init failed:", e);
      }

      if (_taskScheduler != null) {
        _log.info("Starting bundle discovery and switch threads...");

        BundleDiscoveryUpdateThread discoveryThread = new BundleDiscoveryUpdateThread();
        _taskScheduler.schedule(discoveryThread, discoveryThread);

        BundleSwitchUpdateThread switchThread = new BundleSwitchUpdateThread();
        _taskScheduler.schedule(switchThread, switchThread);
      }

      _log.info("init thread exiting");
    }

  }

}
