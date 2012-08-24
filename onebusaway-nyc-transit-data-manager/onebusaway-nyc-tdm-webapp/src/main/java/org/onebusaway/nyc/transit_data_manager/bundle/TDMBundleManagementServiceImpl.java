package org.onebusaway.nyc.transit_data_manager.bundle;

import org.onebusaway.nyc.transit_data_federation.impl.bundle.BundleManagementServiceImpl;
import org.onebusaway.nyc.transit_data_federation.impl.bundle.LocalBundleStoreImpl;
import org.onebusaway.nyc.transit_data_federation.impl.bundle.TDMBundleStoreImpl;
import org.onebusaway.nyc.util.impl.tdm.TransitDataManagerApiLibrary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.annotation.PostConstruct;

public class TDMBundleManagementServiceImpl extends BundleManagementServiceImpl {

  private static Logger _log = LoggerFactory.getLogger(TDMBundleManagementServiceImpl.class);

  @Autowired
  private TransitDataManagerApiLibrary _apiLibrary;

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

  private class InitThread implements Runnable {

    @Override
    public void run() {

      try {
        final int SLEEP_TIME = 10 * 1000;
        _log.info("init thread sleeping " + SLEEP_TIME + " on startup");
        /*
         * Initial bundle load is already in progress due to other TDM
         * dependencies -- allow that load to complete before we re-load. This
         * value can be very small but is currently large for safety.
         */
        Thread.sleep(SLEEP_TIME);
        Thread.yield();
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
