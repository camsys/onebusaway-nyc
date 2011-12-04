package org.onebusaway.nyc.transit_data_federation.impl.bundle;

import org.onebusaway.container.cache.CacheableMethodManager;
import org.onebusaway.container.refresh.RefreshService;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.nyc.transit_data_federation.bundle.model.NycFederatedTransitDataBundle;
import org.onebusaway.nyc.transit_data_federation.impl.tdm.TransitDataManagerApiLibrary;
import org.onebusaway.nyc.transit_data_federation.model.bundle.BundleItem;
import org.onebusaway.nyc.transit_data_federation.services.bundle.BundleManagementService;
import org.onebusaway.nyc.transit_data_federation.services.bundle.BundleStoreService;
import org.onebusaway.transit_data_federation.bundle.model.FederatedTransitDataBundle;
import org.onebusaway.transit_data_federation.impl.RefreshableResources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import javax.annotation.PostConstruct;

public class BundleManagementServiceImpl implements BundleManagementService {

  private static Logger _log = LoggerFactory.getLogger(BundleManagementServiceImpl.class);

  private List<BundleItem> _allBundles = new ArrayList<BundleItem>();
  
  private HashMap<String, BundleItem> _applicableBundles = new HashMap<String, BundleItem>();

  protected String _currentBundleId = null;

  private ServiceDate _currentServiceDate = null;
  
  private boolean _standaloneMode = true;

  private String _bundleRootPath = null;

  protected BundleStoreService _bundleStore = null;

  @Autowired
  private TransitDataManagerApiLibrary _apiLibrary;

	@Autowired
  private ApplicationContext _applicationContext;
	
	@Autowired
	private NycFederatedTransitDataBundle _nycBundle;
	
	@Autowired
	private FederatedTransitDataBundle _bundle;

	@Autowired
	private ThreadPoolTaskScheduler _taskScheduler;
	
	@Autowired
	private RefreshService _refreshService;

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
	  _currentServiceDate = new ServiceDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));

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
		
  public synchronized void discoverBundles() throws Exception {
    _allBundles = _bundleStore.getBundles();
  }

  /**
   * This method calculates which of the bundles available to us are valid for today,
   * and updates the internal list appropriately. It does not switch any bundles.
   */
	public synchronized void refreshApplicableBundles() {
	  _applicableBundles.clear();
	  
	  try {
	    for(BundleItem bundle : _allBundles) {
	      if(bundle.isApplicableToDate(getServiceDate())) {
	        _log.info("Bundle " + bundle.getId() + " is active for today; adding to list of active bundles.");

	        _applicableBundles.put(bundle.getId(), bundle);
	      }
	    }
	  } catch(Exception e) {
      _log.info("Error updating bundle list: " + e.getMessage());
	  }
	}
	
	/**
	 * Recalculate which of the bundles that are available and active for today we should be 
	 * using. Switch to that bundle if not already active. 
	 */
	protected void reevaluateBundleAssignment() throws Exception {
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
	
  @PostConstruct
  protected void setup() throws Exception {
    if(_standaloneMode == true) {
      _bundleStore = new LocalBundleStoreImpl(_bundleRootPath);
    } else {
      _bundleStore = new TDMBundleStoreImpl(_bundleRootPath, _apiLibrary);      
    }
    
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
	
  /******
   * Service methods
   ******/
	@Override
	public synchronized BundleItem getBundleMetadataForBundleWithId(String bundleId) {
	  return _applicableBundles.get(bundleId);
	}
	
  @Override
  public synchronized BundleItem getCurrentBundleMetadata() {
    return _applicableBundles.get(_currentBundleId);
  }
  
  @Override
  public boolean bundleWithIdExists(String bundleId) {
    return _applicableBundles.containsKey(bundleId);
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
	  
	  File path = new File(_bundleRootPath, bundleId);

	  _log.info("Switching to bundle " + bundleId + "...");
		
		_bundle.setPath(path);
    _nycBundle.setPath(path);

		try {
		  _refreshService.refresh(RefreshableResources.TRANSIT_GRAPH);
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
		} catch(Exception e) {
		  _log.error("Bundle " + bundleId + " failed to load. Disabling for this session...");
		  
		  _applicableBundles.remove(bundleId);
		  reevaluateBundleAssignment();

		  return;
		}
		
    // attempt to cleanup any dereferenced data--is old data really cleaned up? FIXME?
		System.gc();
		System.gc();

		// set cache name prefix FIXME--can we avoid the ref. to app context? and or use brian's
		// cacheable key pluggable architecture to do this?
		Map<String, CacheableMethodManager> cacheMethodBeans = _applicationContext.getBeansOfType(
		    CacheableMethodManager.class);
		
		for(String beanId : cacheMethodBeans.keySet()) {
		  CacheableMethodManager bean = cacheMethodBeans.get(beanId);
		  bean.setCacheNamePrefix(bundleId);
		}

		_currentBundleId = bundleId;
		return;
	}

	/*****
	 * Private helper things
	 *****/
  private class BundleSwitchUpdateThread extends TimerTask implements Trigger {
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
      
      Calendar calendar = new GregorianCalendar();
      calendar.setTime(lastTime);
      calendar.set(Calendar.MILLISECOND, 0);
      calendar.set(Calendar.SECOND, 1); // go into the next hour/day

      // if we have no current bundle, keep retrying every minute
      // to see if we're just waiting for the clock to rollover to the next day
      if(_applicableBundles.size() > 0 && _currentBundleId == null) {
        int minutes = calendar.get(Calendar.MINUTE);
        calendar.set(Calendar.MINUTE, minutes + 1);        

      } else {
        calendar.set(Calendar.MINUTE, 0);
        
        int hour = calendar.get(Calendar.HOUR);
        calendar.set(Calendar.HOUR, hour + 1);        
      }
      
      return calendar.getTime();
    }   
  }
  
	private class BundleDiscoveryUpdateThread extends TimerTask implements Trigger {

	  @Override
	  public void run() {     
	    try {       
	      discoverBundles();
	      refreshApplicableBundles();
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

      Calendar calendar = new GregorianCalendar();
      calendar.setTime(lastTime);
      calendar.set(Calendar.MILLISECOND, 0);
      calendar.set(Calendar.SECOND, 0);
      
      int minute = calendar.get(Calendar.MINUTE);
      calendar.set(Calendar.MINUTE, minute + 30);
      
      return calendar.getTime();
    }  
	}
	
}
