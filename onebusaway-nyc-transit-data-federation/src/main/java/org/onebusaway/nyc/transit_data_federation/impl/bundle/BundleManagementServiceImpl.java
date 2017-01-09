package org.onebusaway.nyc.transit_data_federation.impl.bundle;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;

import net.sf.ehcache.CacheManager;

import org.onebusaway.container.refresh.RefreshService;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.bundle.model.NycFederatedTransitDataBundle;
import org.onebusaway.nyc.transit_data_federation.model.bundle.BundleItem;
import org.onebusaway.nyc.transit_data_federation.services.bundle.BundleManagementService;
import org.onebusaway.nyc.transit_data_federation.services.bundle.BundleStoreService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.util.impl.tdm.TransitDataManagerApiLibrary;
import org.onebusaway.transit_data.model.AgencyBean;
import org.onebusaway.transit_data.model.AgencyWithCoverageBean;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data_federation.impl.RefreshableResources;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.FederatedTransitDataBundle;
import org.onebusaway.transit_data_federation.services.beans.NearbyStopsBeanService;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * A component that manages what bundles are loaded and available, and changes them
 * at command or when the currently loaded one expires.
 * 
 * @author jmaki
 *
 */
@SuppressWarnings("rawtypes")
public class BundleManagementServiceImpl implements BundleManagementService {

	// how long to wait for inference threads to exit before forcefully stopping them
	// when the command to change the bundle has been received.
	private static final int INFERENCE_PROCESSING_THREAD_WAIT_TIMEOUT_IN_SECONDS = 60;

  private static final int MAX_EXPECTED_THREADS = 3000;

	private static Logger _log = LoggerFactory.getLogger(BundleManagementServiceImpl.class);

	private List<BundleItem> _allBundles = new ArrayList<BundleItem>();

	protected HashMap<String, BundleItem> _applicableBundles = new HashMap<String, BundleItem>();

	private volatile List<Future> _inferenceProcessingThreads = new ArrayList<Future>();

	protected String _currentBundleId = null;

	protected ServiceDate _currentServiceDate = null;

	private boolean _bundleIsReady = false;

	private boolean _standaloneMode = true;

	private String _bundleRootPath = null;

	protected BundleStoreService _bundleStore = null;
	
	private int bundleDiscoveryFrequencyMin = 15;
  
	private int bundleSwitchFrequencyMin = 60;
	
	private int bundleSwitchFrequencyHour = 1;

	@Autowired
	private NycTransitDataService _nycTransitDataService;

	@Autowired
	private TransitGraphDao _transitGraphDao;

	@Autowired
	private TransitDataManagerApiLibrary _apiLibrary;

	@Autowired
	private NycFederatedTransitDataBundle _nycBundle;

	@Autowired
	private FederatedTransitDataBundle _bundle;

	@Autowired
	private ThreadPoolTaskScheduler _taskScheduler;

	@Autowired
	private RefreshService _refreshService;
	
	@Autowired
	NearbyStopsBeanService _nearbyStopsBeanService;
	
	@Autowired
  private ConfigurationService _configurationService;
  
   // This is only used when logging block info at bundle change.
   @Autowired
   private CalendarService _calendarService;

	/******
	 * Getters / Setters
	 ******/
	public String getBundleStoreRoot() {
		return _bundleRootPath;
	}

	public void setBundleStoreRoot(String path) throws Exception {
		File bundleRootPath = new File(path);

		if(!bundleRootPath.exists() || !bundleRootPath.canWrite()) {
			throw new Exception("Bundle store path " + bundleRootPath + " does not exist or is not writable.");
		}

		this._bundleRootPath = path;
	}

	public void setTime(Date time) {
		Calendar cal = new GregorianCalendar();
		cal.setTime(time);
		_currentServiceDate = new ServiceDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DATE));

		refreshApplicableBundles();
	}

	public void setServiceDate(ServiceDate serviceDate) {
		_currentServiceDate = serviceDate;

		refreshApplicableBundles();
	}

	public ServiceDate getServiceDate() {
		if(_currentServiceDate != null)
			return _currentServiceDate;
		else
			return new ServiceDate();
	}

	public void setStandaloneMode(boolean standalone) {
		_standaloneMode = standalone;
	}

	public boolean getStandaloneMode() {
		return _standaloneMode;
	}

	public void discoverBundles() throws Exception {
		_allBundles = _bundleStore.getBundles();
	}

	/**
	 * This method calculates which of the bundles available to us are valid for today,
	 * and updates the internal list appropriately. It does not switch any bundles.
	 */
	public synchronized void refreshApplicableBundles() {
		_applicableBundles.clear();

		for(BundleItem bundle : _allBundles) {
			if(bundle.isApplicableToDate(getServiceDate())) {
				_log.info("Bundle " + bundle.getId() + " is active for today; adding to list of active bundles.");

				_applicableBundles.put(bundle.getId(), bundle);
			}
		}
	}

	/**
	 * Recalculate which of the bundles that are available and active for today we should be 
	 * using. Switch to that bundle if not already active. 
	 */
	public void reevaluateBundleAssignment() throws Exception {
		if(_applicableBundles.size() == 0) {
			_log.error("No valid and active bundles found!");
			return;
		}

		// sort bundles by preference 
		ArrayList<BundleItem> bestBundleCandidates = new ArrayList<BundleItem>(_applicableBundles.values());
		Collections.sort(bestBundleCandidates);

		BundleItem bestBundle = bestBundleCandidates.get(bestBundleCandidates.size() - 1);
		_log.info("Best bundle is " + bestBundle.getId());

		changeBundle(bestBundle.getId());
	}
	
	@Refreshable(dependsOn = {"tdm.bundleDiscoveryFrequencyMin", "tdm.bundleSwitchFrequencyMin", "tdm.bundleSwitchFrequencyHour"})
	protected void refreshCache() {
	    bundleDiscoveryFrequencyMin = Integer.parseInt(_configurationService.getConfigurationValueAsString(
	        "tdm.bundleDiscoveryFrequencyMin", "15"));
	    bundleSwitchFrequencyMin = Integer.parseInt(_configurationService.getConfigurationValueAsString(
	        "tdm.bundleSwitchFrequencyMin", "60"));
	    bundleSwitchFrequencyHour = Integer.parseInt(_configurationService.getConfigurationValueAsString(
	            "tdm.bundleSwitchFrequencyHour", "1"));
	}

	@PostConstruct
	protected void setup() throws Exception {
		if(_standaloneMode == true) {
			_bundleStore = new LocalBundleStoreImpl(_bundleRootPath);
		} else {
			_bundleStore = new TDMBundleStoreImpl(_bundleRootPath, _apiLibrary);      
		}
		refreshCache();
		discoverBundles();
		refreshApplicableBundles();
		reevaluateBundleAssignment();

		if(_taskScheduler != null) {
			_log.info("Starting bundle discovery and switch threads...");

			BundleDiscoveryUpdateThread discoveryThread = new BundleDiscoveryUpdateThread();
			_taskScheduler.schedule(discoveryThread, discoveryThread);

			BundleSwitchUpdateThread switchThread = new BundleSwitchUpdateThread();
			_taskScheduler.schedule(switchThread, switchThread);
		}
	}	

	public int getBundleDiscoveryFrequencyMin() {
		return bundleDiscoveryFrequencyMin;
	}

	public void setBundleDiscoveryFrequencyMin(int bundleDiscoveryFrequencyMin) {
		this.bundleDiscoveryFrequencyMin = bundleDiscoveryFrequencyMin;
	}

	public int getBundleSwitchFrequencyMin() {
		return bundleSwitchFrequencyMin;
	}

	public void setBundleSwitchFrequencyMin(int bundleSwitchFrequencyMin) {
		this.bundleSwitchFrequencyMin = bundleSwitchFrequencyMin;
	}
	
	public int getBundleSwitchFrequencyHour() {
		return bundleSwitchFrequencyHour;
	}

	public void setBundleSwitchFrequencyHour(int bundleSwitchFrequencyHour) {
		this.bundleSwitchFrequencyHour = bundleSwitchFrequencyHour;
	}

	/******
	 * Service methods
	 ******/
	@Override
	public List<BundleItem> getAllKnownBundles() {
		return _allBundles;
	}

	@Override
	public boolean bundleWithIdExists(String bundleId) {
		return _applicableBundles.containsKey(bundleId);
	}

	@Override
	public synchronized BundleItem getCurrentBundleMetadata() {
		return _applicableBundles.get(_currentBundleId);
	}

	// Can messages be processed using this bundle and current state?
	@Override
	public Boolean bundleIsReady() {
		return _bundleIsReady;
	}

	// register inference processing thread with the bundle manager--
	// bundles cannot be changed as long as threads are actively using it.
	@Override
	public void registerInferenceProcessingThread(Future thread) {
		_inferenceProcessingThreads.add(thread);

		// keep our thread list from getting /too/ big unnecessarily
		if(_inferenceProcessingThreads.size() > MAX_EXPECTED_THREADS) {
			removeDeadInferenceThreads();
		}
	}

	@Override
	public void changeBundle(String bundleId) throws Exception {
		if(bundleId == null || !_applicableBundles.containsKey(bundleId)) {
			throw new Exception("Bundle " + bundleId + " is not valid or does not exist.");
		}

		if(bundleId.equals(_currentBundleId)) {
			_log.info("Received command to change to " + bundleId + "; bundle is already active.");
			return;
		}

		_log.info("Switching to bundle " + bundleId + "...");   
		_bundleIsReady = false;

		// wait until all inference processing threads have exited...
		int t = INFERENCE_PROCESSING_THREAD_WAIT_TIMEOUT_IN_SECONDS / 5;
		while(t-- >= 0) {
			removeDeadInferenceThreads();
			_log.info("Waiting for all inference processing threads to exit... " + _inferenceProcessingThreads.size() + " thread(s) left.");     

			// have all inference threads finished yet?
			if(allInferenceThreadsHaveExited()) {
				break;

			// forcefully cancel threads when we timeout
			} else if(t == 0) {
				for(Future thread : _inferenceProcessingThreads) {
					if(!thread.isDone() && !thread.isCancelled()) {
						thread.cancel(true);
					}
				}

				_inferenceProcessingThreads.clear();

				break;
			}

			Thread.yield();
			Thread.sleep(5 * 1000);
		}

		_log.info("All inference processing threads have now exited--changing bundle...");

		// switch bundle files
		File path = new File(_bundleRootPath, bundleId);
		_bundle.setPath(path);
		_nycBundle.setPath(path);

		try {
			_refreshService.refresh(RefreshableResources.TRANSIT_GRAPH);
			
			// give child classes a chance to do work here 
			timingHook();
			
			_refreshService.refresh(RefreshableResources.CALENDAR_DATA);
			_refreshService.refresh(RefreshableResources.ROUTE_COLLECTIONS_DATA);
			_refreshService.refresh(RefreshableResources.ROUTE_COLLECTION_SEARCH_DATA);
			_refreshService.refresh(RefreshableResources.STOP_SEARCH_DATA);
			_refreshService.refresh(RefreshableResources.WALK_PLANNER_GRAPH);
			_refreshService.refresh(RefreshableResources.BLOCK_INDEX_DATA);
			_refreshService.refresh(RefreshableResources.BLOCK_INDEX_SERVICE);
			_refreshService.refresh(RefreshableResources.STOP_TRANSFER_DATA);
			_refreshService.refresh(RefreshableResources.SHAPE_GEOSPATIAL_INDEX);
			_refreshService.refresh(RefreshableResources.STOP_GEOSPATIAL_INDEX);
			_refreshService.refresh(RefreshableResources.TRANSFER_PATTERNS);
			_refreshService.refresh(RefreshableResources.NARRATIVE_DATA);

			_refreshService.refresh(NycRefreshableResources.DESTINATION_SIGN_CODE_DATA);
			_refreshService.refresh(NycRefreshableResources.TERMINAL_DATA);
			_refreshService.refresh(NycRefreshableResources.RUN_DATA);
			_refreshService.refresh(NycRefreshableResources.NON_REVENUE_MOVES_DATA);
			_refreshService.refresh(NycRefreshableResources.NON_REVENUE_STOP_DATA);
		} catch(Exception e) {
			_log.error("Bundle " + bundleId + " failed to load. Disabling for this session...");
			_applicableBundles.remove(bundleId);
			reevaluateBundleAssignment();

			throw new Exception("Bundle " + bundleId + " loading exception. Root exception follows.", e);
		}

		_log.info("Refresh/reload of bundle data complete.");

		// attempt to cleanup any dereferenced data--I know this is a debate in the Java space--
		// do you let the magic GC do it's thing or force its hand? With a profiler, I found this helps
		// keep memory use more consistently under 2x initial heap size. FWIW.
		System.gc();
		System.gc();
		_log.info("Garbage collection after bundle switch complete.");

		_currentBundleId = bundleId;
		_bundleIsReady = true;	
		_log.info("New bundle is now ready.");

		// need to do after bundle is ready so TDS can not block
		removeAndRebuildCache();
		
		return;
	}

	// some kind of event notification system camsys setup? 
	protected void timingHook() {}
	
	/*****
	 * Private helper things
	 *****/

	private void removeAndRebuildCache() {
	  // give subclasses a chance to do work
	  timingHook();
		clearCache();
		rebuildCache();
	}
	
	private void clearCache(){
	  _log.info("Clearing all caches...");
    for(CacheManager cacheManager : CacheManager.ALL_CACHE_MANAGERS) {
      _log.info("Found " + cacheManager.getName()); 
      for(String cacheName : cacheManager.getCacheNames()) {
        _log.info(" > Clearing Cache: " + cacheName);
        cacheManager.getCache(cacheName).flush();
        cacheManager.clearAllStartingWith(cacheName);
      }
      cacheManager.clearAll();
    }
    _log.info("Cache clearing complete!");
	}
	
	private void rebuildCache(){
	  _log.info("Rebuilding caches...");
    try {
      
      List<AgencyWithCoverageBean> agenciesWithCoverage = _nycTransitDataService.getAgenciesWithCoverage();
      for (AgencyWithCoverageBean agencyWithCoverage : agenciesWithCoverage) {
        AgencyBean agency = agencyWithCoverage.getAgency();
        
        ListBean<String> stopIds = _nycTransitDataService.getStopIdsForAgencyId(agency.getId());
        for (String stopId : stopIds.getList()) {
          _nycTransitDataService.getStop(stopId);
        }

        ListBean<String> routeIds = _nycTransitDataService.getRouteIdsForAgencyId(agency.getId());
        for (String routeId : routeIds.getList()) {
          _nycTransitDataService.getStopsForRoute(routeId);
        }
      }

      Set<AgencyAndId> shapeIds = new HashSet<AgencyAndId>();
      for (TripEntry trip : _transitGraphDao.getAllTrips()) {
        AgencyAndId shapeId = trip.getShapeId();
        if (shapeId != null && shapeId.hasValues())
          shapeIds.add(shapeId);
      }

      for (AgencyAndId shapeId : shapeIds) {
        _nycTransitDataService.getShapeForId(AgencyAndIdLibrary.convertToString(shapeId));
      }
      
      listCacheNames(); 
      
      _log.info("Cache rebuild complete!");
 
    } catch (Exception e) {
      _log.error("Exception during cache rebuild: ", e.getMessage());
    }
  }
	
	private void listCacheNames(){
	  for(CacheManager cacheManager : CacheManager.ALL_CACHE_MANAGERS) { 
      _log.info("Found " + cacheManager.getName());
      
      for(String cacheName : cacheManager.getCacheNames()) {
        _log.info(" > Cache: " + cacheName);
      }
    }
	}

	private void removeDeadInferenceThreads() {
		List<Future> finishedThreads = new ArrayList<Future>();

		// find all threads that are not running...
		for(Future thread : _inferenceProcessingThreads) {
			if(thread.isDone() || thread.isCancelled()) {
				finishedThreads.add(thread);
			}
		}

		// ...and then remove them from our list of processing threads
		for(Future deadThread : finishedThreads) {
			_inferenceProcessingThreads.remove(deadThread);
		}
	}

	private boolean allInferenceThreadsHaveExited() {
		removeDeadInferenceThreads();

		return (_inferenceProcessingThreads.size() == 0);
	}

	protected class BundleSwitchUpdateThread extends TimerTask implements Trigger {

		// required for subclass
		public BundleSwitchUpdateThread() {
		}

		@Override
		public void run() {     
			try {
				refreshApplicableBundles();
				reevaluateBundleAssignment();  
			} catch(Exception e) {
				_log.error("Error re-evaluating bundle assignment: " + e.getMessage());
				e.printStackTrace();
			}
		}

		@Override
		public Date nextExecutionTime(TriggerContext arg0) {
			Date lastTime = arg0.lastScheduledExecutionTime();
			if(lastTime == null) {
				lastTime = new Date();
			}

			return getNextBundleSwitchTime(lastTime);
		}   
	}
	
	public Date getNextBundleSwitchTime(Date lastExecutionTime){
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(lastExecutionTime);
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.set(Calendar.SECOND, 1); // go into the next hour/day

		// if we have no current bundle, keep retrying every minute
		// to see if we're just waiting for the clock to rollover to the next day
		if(_applicableBundles.size() > 0 && _currentBundleId == null) {
			int minutes = calendar.get(Calendar.MINUTE);
			calendar.set(Calendar.MINUTE, minutes + 1);        

		} else {
			calendar.set(Calendar.MINUTE, bundleSwitchFrequencyMin);            
		}
		
		
		if(_applicableBundles.size() > 0 && _currentBundleId == null) {
			int minutes = calendar.get(Calendar.MINUTE);
			calendar.set(Calendar.MINUTE, minutes + 1);        

		} else {
			calendar.set(Calendar.MINUTE, 0);
			int hour = calendar.get(Calendar.HOUR);
			calendar.set(Calendar.HOUR, hour + bundleSwitchFrequencyHour);        
		}

		return calendar.getTime();
	}

	protected class BundleDiscoveryUpdateThread extends TimerTask implements Trigger {

		// required for subclass
		public BundleDiscoveryUpdateThread() {
		}

		@Override
		public void run() {     
			try {       
				discoverBundles();
			} catch(Exception e) {
				_log.error("Error updating bundle list: " + e.getMessage());
				e.printStackTrace();
			}
		}   

		@Override
		public Date nextExecutionTime(TriggerContext arg0) {
			Date lastTime = arg0.lastScheduledExecutionTime();
			
			if(lastTime == null) {
				lastTime = new Date();
			}

			return getNextBundleDiscoveryTime(lastTime);
		}  
	}
	
	public Date getNextBundleDiscoveryTime(Date lastExecutionTime){
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(lastExecutionTime);
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.set(Calendar.SECOND, 0);
		
		int minute = calendar.get(Calendar.MINUTE);
		int nextExecutionMinute = getNextExecutionMinute(bundleDiscoveryFrequencyMin, minute);
		calendar.set(Calendar.MINUTE, nextExecutionMinute);

		return calendar.getTime();
	}
	
	public int getNextExecutionMinute(int frequency, int currentMinute){
		int occurances = currentMinute / frequency;
		if(occurances < 1){
			return frequency;
		}
		return frequency * (occurances + 1);	
	}
}
