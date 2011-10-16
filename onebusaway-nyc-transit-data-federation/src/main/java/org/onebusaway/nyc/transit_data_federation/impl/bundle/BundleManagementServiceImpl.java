package org.onebusaway.nyc.transit_data_federation.impl.bundle;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;

import org.onebusaway.container.cache.CacheableMethodManager;
import org.onebusaway.container.refresh.RefreshService;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.nyc.transit_data_federation.bundle.model.NycFederatedTransitDataBundle;
import org.onebusaway.nyc.transit_data_federation.impl.tdm.TransitDataManagerApiLibrary;
import org.onebusaway.nyc.transit_data_federation.model.bundle.BundleFileItem;
import org.onebusaway.nyc.transit_data_federation.model.bundle.BundleItem;
import org.onebusaway.nyc.transit_data_federation.services.bundle.BundleManagementService;
import org.onebusaway.transit_data_federation.bundle.model.FederatedTransitDataBundle;
import org.onebusaway.transit_data_federation.impl.RefreshableResources;
import org.onebusaway.utility.ObjectSerializationLibrary;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

public class BundleManagementServiceImpl implements BundleManagementService {

  private static Logger _log = LoggerFactory.getLogger(BundleManagementServiceImpl.class);

  private Timer _updateTimer = null;
  
  // all bundles present in the local disk store (from TDM or otherwise)
  private HashMap<String, BundleItem> _allBundles = new HashMap<String, BundleItem>();

  // bundles that are active now (as defined in _today)
  private HashMap<String, BundleItem> _validBundles = new HashMap<String, BundleItem>();

  // the currently loaded bundle ID
  protected String _currentBundleId = null;

  // time to use when comparing bundles for applicability to "today"
  private Date _today = new Date();
  
  // does this bundle manager exist without a TDM attached?
  private boolean _standaloneMode = true;

  // where should we store our bundle data?
  private String _bundleRootPath = null;

  private static final int _fileDownloadRetries = 2;

  private static SimpleDateFormat _serviceDateFormatter = new SimpleDateFormat("yyyy-MM-dd");

  private TransitDataManagerApiLibrary _apiLibrary = new TransitDataManagerApiLibrary();
	
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
	  // does this path exist? if not, it'll cause problems later on.
		File bundleRootPath = new File(path);
		if(!bundleRootPath.exists() || !bundleRootPath.canWrite())
		  throw new Exception("Bundle store path " + bundleRootPath + " does not exist or is not writable.");

	   this._bundleRootPath = path;
	}
	
  public void setTime(Date time) {
	  _today = time;
	  
	  refreshValidBundleList();
  }
	
  public void setStandaloneMode(boolean standalone) {
	  _standaloneMode = standalone;
	}
	
  public boolean getStandaloneMode() {
	  return _standaloneMode;
	}
	
  /******
   * Helper methods for discovery of bundles
   ******/
  protected ArrayList<BundleItem> getBundleListFromLocalStore() throws Exception {
    ArrayList<BundleItem> output = new ArrayList<BundleItem>();

    File bundleRoot = new File(_bundleRootPath);
    if(!bundleRoot.isDirectory())    
      return output;
    
    for(String filename : bundleRoot.list()) {
      File possibleBundle = new File(bundleRoot, filename);
      if(possibleBundle.isDirectory()) {
        File calendarServiceObjectFile = new File(possibleBundle, "CalendarServiceData.obj");
        if(!calendarServiceObjectFile.exists()) {
          _log.info("Could not find CalendarServiceData.obj in local bundle '" + filename + "'; skipping. Not a bundle?");
          continue;
        }

        // get data to fill in the BundleItem for this bundle.
        Date minServiceDate = null;
        Date maxServiceDate = null;
        try {
          CalendarServiceData data = 
              ObjectSerializationLibrary.readObject(calendarServiceObjectFile);

          // loop through all service IDs and find the minimum and max--most likely they'll all
          // be the same range, but not necessarily...
          for(AgencyAndId serviceId : data.getServiceIds()) {
            // can we assume these are sorted chronologically?
            List<ServiceDate> serviceDates = data.getServiceDatesForServiceId(serviceId);
            for(ServiceDate serviceDate : serviceDates) {
              if(minServiceDate == null || serviceDate.getAsDate().before(minServiceDate))
                minServiceDate = serviceDate.getAsDate();

              if(maxServiceDate == null || serviceDate.getAsDate().after(maxServiceDate))
                maxServiceDate = serviceDate.getAsDate();
            }
          }             
        } catch(Exception e) {
          _log.info("Deserialization of CalendarServiceData.obj in local bundle " + 
                filename + "; skipping.");
          continue;
        }        

        BundleItem validLocalBundle = new BundleItem();
        validLocalBundle.setId(filename);
        validLocalBundle.setServiceDateFrom(minServiceDate);
        validLocalBundle.setServiceDateTo(maxServiceDate);        
        output.add(validLocalBundle);

        _log.info("Found local bundle " + filename + " with service range (" + 
            _serviceDateFormatter.format(minServiceDate) + "," + 
            _serviceDateFormatter.format(maxServiceDate) + ").");
      }
    }
    
    return output;
	}
	
  private ArrayList<BundleItem> getBundleListFromTDM() throws Exception {
	  _log.info("Getting current bundle list from TDM...");
	  
	  ArrayList<JsonObject> bundles = 
	      _apiLibrary.getItemsForRequest("bundle", "list");

	  ArrayList<BundleItem> output = new ArrayList<BundleItem>();
	  for(JsonObject itemToAdd : bundles) {
	    BundleItem item = new BundleItem();

	    item.setId(itemToAdd.get("id").getAsString());
	    item.setServiceDateFrom(_serviceDateFormatter.parse(itemToAdd.get("service-date-from").getAsString()));
      item.setServiceDateTo(_serviceDateFormatter.parse(itemToAdd.get("service-date-to").getAsString()));
      
      ArrayList<BundleFileItem> files = new ArrayList<BundleFileItem>();
      for(JsonElement _subitemToAdd : itemToAdd.get("files").getAsJsonArray()) {
        JsonObject subitemToAdd = _subitemToAdd.getAsJsonObject();
        
        BundleFileItem fileItemToAdd = new BundleFileItem();
        fileItemToAdd.setFilename(subitemToAdd.get("filename").getAsString());
        fileItemToAdd.setMd5(subitemToAdd.get("md5").getAsString());
        files.add(fileItemToAdd);
      }
      item.setFiles(files);
	    	    
	    output.add(item);
	  }
	  
	  _log.info("Found " + output.size() + " bundles available from the TDM.");
	  
	  return output;
	}
	
  private String getMd5HashForFile(File filename) throws Exception {
    MessageDigest md5Hasher = MessageDigest.getInstance("MD5");

    FileInputStream in = new FileInputStream(filename.getPath());
    byte data[] = new byte[1024];
    while(true) {
      int readBytes = in.read(data, 0, data.length);
      if(readBytes < 0)
        break;
      md5Hasher.update(data, 0, readBytes);
    }

    byte messageDigest[] = md5Hasher.digest();
    StringBuffer hexString = new StringBuffer();
    for (int i=0;i<messageDigest.length;i++) {
      String hex = Integer.toHexString(0xFF & messageDigest[i]); 
      if(hex.length() == 1)
        hexString.append('0');
      hexString.append(hex);
    }
    
    return hexString.toString();
	}
	
  private void downloadUrlToLocalPath(URL url, File destFilename, String expectedMd5) 
      throws Exception {
	  
    try {
	    _log.info("Downloading bundle item from " + url + "...");
	    
	    File containerPath = destFilename.getParentFile();
	    if(!containerPath.exists() && !containerPath.mkdirs()) {
	      throw new Exception("Could not create parent directories for path " + destFilename);
	    }
    
	    if(!destFilename.createNewFile()) {
        throw new Exception("Could not create empty file at path " + destFilename);	      
	    }
	    
	    // download file 
	    FileOutputStream out = new FileOutputStream(destFilename.getPath());
	    BufferedOutputStream bufferedOut = new BufferedOutputStream(out, 1024);

	    MessageDigest md5Hasher = MessageDigest.getInstance("MD5");
	    BufferedInputStream in = new java.io.BufferedInputStream(url.openStream());    

	    byte data[] = new byte[1024];
	    while(true) {
	      int readBytes = in.read(data, 0, data.length);
	      if(readBytes < 0)
	        break;

	      md5Hasher.update(data, 0, readBytes);
	      bufferedOut.write(data, 0, readBytes);
	    }

      bufferedOut.close();
      out.close();
      in.close();         

      // check hash
      byte messageDigest[] = md5Hasher.digest();
      StringBuffer hexString = new StringBuffer();
      for (int i=0;i<messageDigest.length;i++) {
        String hex = Integer.toHexString(0xFF & messageDigest[i]); 
        if(hex.length() == 1)
          hexString.append('0');
        hexString.append(hex);
      }

      if(!hexString.toString().equals(expectedMd5)) {
        _log.info("Hash doesn't match! Deleting file.");
        throw new Exception("MD5 hash doesn't match.");
	    }	    
	  } catch(Exception e) {
	    if(!destFilename.delete()) {
	      if(destFilename.exists()) {
	        throw new Exception("Could not delete corrupted file " + destFilename);
	      }
	    }
	    throw e;
	  }
	}
	
  /**
   * This method finds and makes ready all bundles available to us, via the TDM
   * or locally if in "standalone mode". It downloads the files from the TDM if they
   * are not already present in the local disk store.
   * 
   * @throws Exception
   */
  private void discoverBundles() throws Exception {
	  // if there's no TDM present, we just use what we already have locally.
	  if(_standaloneMode) {
	    ArrayList<BundleItem> localBundles = getBundleListFromLocalStore();
	    for(BundleItem localBundle: localBundles) {
	      _allBundles.put(localBundle.getId(), localBundle);
	    }
	    return;
	  }
	  
    ArrayList<BundleItem> bundlesFromTdm = getBundleListFromTDM();
	  for(BundleItem bundle : bundlesFromTdm) {
	    boolean bundleIsValid = true;

	    // ensure bundle path exists locally
	    File bundleRoot = new File(_bundleRootPath, bundle.getId());
	    if(!bundleRoot.exists())
	      if(!bundleRoot.mkdirs()) 
	        throw new Exception("Creation of bundle root for " + bundle.getId() + " failed.");

	    // see if files already exist locally and match their MD5s
	    for(BundleFileItem file : bundle.getFiles()) {
	      File fileInBundlePath = new File(bundleRoot, file.getFilename());
	      if(fileInBundlePath.exists()) {
	        String hashOfExistingFile = getMd5HashForFile(fileInBundlePath);
	        if(!hashOfExistingFile.equals(file.getMd5())) {
	          _log.info("File " + fileInBundlePath + " is corrupted; removing.");

	          if(!fileInBundlePath.delete()) {
	            _log.error("Could not remove corrupted file " + fileInBundlePath);
	            bundleIsValid = false;
	            break;
	          }
	        }
	      }

	      // if the file is not there, or was removed above, try to download it again from the TDM.
	      if(!fileInBundlePath.exists()) {
	        int tries = _fileDownloadRetries;
	        while(tries > 0) {
            URL fileDownloadUrl = _apiLibrary.buildUrl("bundle", bundle.getId(), "file", file.getFilename(), "get");
            try {
	            downloadUrlToLocalPath(fileDownloadUrl, fileInBundlePath, file.getMd5());
	          } catch(Exception e) {
	            tries--;
              if(tries == 0)
                bundleIsValid = false;

              _log.info("Download of " + fileDownloadUrl + " failed (" + e.getMessage() + ");" + 
                  " retrying (retries left=" + tries + ")");

              continue;
	          }

            // file was downloaded successfully--break out of retry loop
            break;
	        }
	      }
	    } // for each file

      if(bundleIsValid) {
        _allBundles.put(bundle.getId(), bundle);
        _log.info("Bundle " + bundle.getId() + " files pass checksums; added to list of local bundles.");
      } else {
        _log.info("Bundle " + bundle.getId() + " files do NOT pass checksums; skipped.");
      }
	  } // for each bundle
	}
	
  /**
   * This method calculates which of the bundles available to us are valid for today,
   * and updates the internal list appropriately. It does not switch any bundles.
   */
	public synchronized void refreshValidBundleList() {
	  _validBundles.clear();
	  
    for(BundleItem bundle : _allBundles.values()) {
      if(bundle.getServiceDateFrom().before(_today) && bundle.getServiceDateTo().after(_today)) {
        _log.info("Bundle " + bundle.getId() + " is active for today; adding to list of active bundles.");
        _validBundles.put(bundle.getId(), bundle);
      }
    }
	}
	
	/**
	 * Recalculate which of the bundles that are available and active for today we should be 
	 * using. Switch to that bundle if not already active. 
	 * 
	 * @throws Exception
	 */
	protected void reevaluateBundleAssignment() throws Exception {
	  refreshValidBundleList();

	  if(_validBundles.size() == 0) {
	    _log.error("No valid and active bundles found!");
	    return;
	  }
	  
    // sort bundles by preference 
    ArrayList<BundleItem> bestBundleCandidates = new ArrayList<BundleItem>(_validBundles.values());
    Collections.sort(bestBundleCandidates, new BundleComparator(_today));
    BundleItem bestBundle = bestBundleCandidates.get(bestBundleCandidates.size() - 1);
    changeBundle(bestBundle.getId());
	}
	
  @PostConstruct
  protected void setup() throws Exception {
    discoverBundles();
    reevaluateBundleAssignment();    
    
    // this process updates the list of bundles every 30m.
    _log.info("Starting bundle discovery update process...");    
    long updateInterval = 30 * 60 * 1000; // 30m 
    _updateTimer = new Timer();
    _updateTimer.schedule(new BundleDiscoveryUpdateThread(), updateInterval, updateInterval); 
    
    // this process makes sure we're using the best bundle for the current 
    // service date every hour, on the hour.
    if(_taskScheduler != null) {
      BundleSwitchUpdateThread updateThread = new BundleSwitchUpdateThread();
      _taskScheduler.schedule(updateThread, updateThread);
    }
  }	
	
  /******
   * Service methods
   ******/
	@Override
	public synchronized BundleItem getBundleMetadataForBundleWithId(String bundleId) {
	  return _validBundles.get(bundleId);
	}
	
  @Override
  public synchronized BundleItem getCurrentBundleMetadata() {
    return _validBundles.get(_currentBundleId);
  }

  @Override
	public synchronized boolean bundleWithIdExists(String bundleId) {
	  return _validBundles.containsKey(bundleId);
	}  
	
	@Override
	public void changeBundle(String bundleId) throws Exception {
	  if(bundleId == null || !_validBundles.containsKey(bundleId))
	    throw new Exception("Bundle " + bundleId + " is not valid.");

	  if(bundleId.equals(_currentBundleId)) {
	    _log.info("Received command to change to " + bundleId + "; bundle is already active.");
	    return;
	  }
	  
	  File path = new File(_bundleRootPath, bundleId);

	  _log.info("Switching to bundle " + bundleId + "...");
		
		_bundle.setPath(path);
		_refreshService.refresh(RefreshableResources.CALENDAR_DATA);
		_refreshService.refresh(RefreshableResources.ROUTE_COLLECTIONS_DATA);
		_refreshService.refresh(RefreshableResources.ROUTE_COLLECTION_SEARCH_DATA);
		_refreshService.refresh(RefreshableResources.STOP_SEARCH_DATA);
		_refreshService.refresh(RefreshableResources.WALK_PLANNER_GRAPH);
		_refreshService.refresh(RefreshableResources.TRANSIT_GRAPH);
		_refreshService.refresh(RefreshableResources.BLOCK_INDEX_DATA);
		_refreshService.refresh(RefreshableResources.BLOCK_INDEX_SERVICE);
		_refreshService.refresh(RefreshableResources.STOP_TRANSFER_DATA);
		_refreshService.refresh(RefreshableResources.SHAPE_GEOSPATIAL_INDEX);
    _refreshService.refresh(RefreshableResources.STOP_GEOSPATIAL_INDEX);
		_refreshService.refresh(RefreshableResources.TRANSFER_PATTERNS);
		_refreshService.refresh(RefreshableResources.NARRATIVE_DATA);
		
    _nycBundle.setPath(path);
		_refreshService.refresh(NycRefreshableResources.DESTINATION_SIGN_CODE_DATA);
		_refreshService.refresh(NycRefreshableResources.TERMINAL_DATA);
		_refreshService.refresh(NycRefreshableResources.RUN_DATA);

    // attempt to cleanup any dereferenced data--is old data really cleaned up? FIXME?
		System.gc();
		System.gc();

		// set cache name prefix FIXME--can we avoid the ref. to app context? and or use brian's
		// cacheable key pluggable architecture to do this?
		Map<String, CacheableMethodManager> cacheMethodBeans 
		    = _applicationContext.getBeansOfType(CacheableMethodManager.class);
		
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
        reevaluateBundleAssignment();  
      } catch(Exception e) {
        _log.error("Error re-evaluating bundle assignment: " + e.getMessage());
        e.printStackTrace();
      }
    }

    @Override
    public Date nextExecutionTime(TriggerContext arg0) {
      Date lastTime = arg0.lastScheduledExecutionTime();
      if(lastTime == null)
        lastTime = new Date();

      Calendar calendar = new GregorianCalendar();
      calendar.setTime(lastTime);
      calendar.set(Calendar.MILLISECOND, 0);
      calendar.set(Calendar.SECOND, 0);
      calendar.set(Calendar.MINUTE, 0);
      
      int hour = calendar.get(Calendar.HOUR);
      calendar.set(Calendar.HOUR, hour + 1);
      
      return calendar.getTime();
    }   
  }
  
	private class BundleDiscoveryUpdateThread extends TimerTask {
	  @Override
	  public void run() {     
	    try {       
	      discoverBundles();  
	    } catch(Exception e) {
	      _log.error("Error updating bundle list: " + e.getMessage());
	      e.printStackTrace();
	    }
	  }   
	}

	// pick the bundle with the most applicability into the future if 
	// more than one is active for a given date
	public class BundleComparator implements Comparator<BundleItem>{
	  
	  Date _epoch = null;
	  
	  public BundleComparator() {
	    _epoch = new Date();
	  }
	  
	  public BundleComparator(Date epoch) {
	    _epoch = epoch;
	  }
	  
    @Override
    public int compare(BundleItem o1, BundleItem o2) {
      Long epoch = _epoch.getTime();
      Long interval1 = o1.getServiceDateTo().getTime() - epoch;
      Long interval2 = o2.getServiceDateTo().getTime() - epoch;
    
      return interval1.compareTo(interval2);
    }
  }
	
}
