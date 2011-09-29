package org.onebusaway.nyc.transit_data_federation.impl.bundle;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;

import org.onebusaway.container.refresh.RefreshService;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.nyc.transit_data_federation.bundle.model.NycFederatedTransitDataBundle;
import org.onebusaway.nyc.transit_data_federation.services.bundle.BundleManagementService;
import org.onebusaway.transit_data_federation.bundle.model.FederatedTransitDataBundle;
import org.onebusaway.transit_data_federation.impl.RefreshableResources;
import org.onebusaway.utility.ObjectSerializationLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class BundleManagementServiceImpl implements BundleManagementService {

	private static Logger _log = LoggerFactory.getLogger(BundleManagementServiceImpl.class);

	private HashMap<String, List<Date>> _bundleEffectiveDateMap = new HashMap<String, List<Date>>();
	
	@Autowired
	private NycFederatedTransitDataBundle _nycBundle;
	
	@Autowired
	private FederatedTransitDataBundle _bundle;
	
	@Autowired
	private RefreshService _refreshService;
	
	private String _bundleRootPath = null;
	
	public String getBundleStoreRoot() {
		return _bundleRootPath;
	}

	public void setBundleStoreRoot(String path) {
		this._bundleRootPath = path;
	}

	private void buildBundleEffectiveDateMap() {
		File bundleRoot = new File(_bundleRootPath);
	    if (bundleRoot.isDirectory()) {
	      for (String filename : bundleRoot.list()) {
	        File possibleBundle = new File(bundleRoot, filename);

	        // is a directory inside the bundle root--could be a bundle!
	        if(possibleBundle.isDirectory()) {
	        	Date minDate = null;
	        	Date maxDate = null;
	        	
		        File calendarServiceObjectPath = new File(possibleBundle, "CalendarServiceData.obj");

		        if(!calendarServiceObjectPath.exists())
		        	continue;
		        
		        try {
		        	CalendarServiceData data = ObjectSerializationLibrary.readObject(calendarServiceObjectPath);
		        	
		        	for(AgencyAndId serviceId : data.getServiceIds()) {
		        		// can we assume these are sorted chronologically?
		        		List<ServiceDate> dates = data.getServiceDatesForServiceId(serviceId);
		        		for(ServiceDate date : dates) {
			        		if(minDate == null || date.getAsDate().before(minDate))
			        			minDate = date.getAsDate();

			        		if(maxDate == null || date.getAsDate().after(maxDate))
			        			maxDate = date.getAsDate();
		        		}
		        	}		        	
		        } catch(Exception e) {
		        	continue;
		        }

		        if(minDate != null && maxDate != null) {
		        	ArrayList<Date> dateRange = new ArrayList<Date>();
		        	dateRange.add(minDate);
		        	dateRange.add(maxDate);
		        	_bundleEffectiveDateMap.put(filename, dateRange);
		        }
	        }
	      }
	    }
	}

	private String chooseBestBundleForToday() {
		HashMap<Long, String> bundlesThatApplyToToday = new HashMap<Long, String>();
		for(String bundleDirectory : _bundleEffectiveDateMap.keySet()) {
			List<Date> activeDateRange = _bundleEffectiveDateMap.get(bundleDirectory);
			Date startDate = activeDateRange.get(0);
			Date endDate = activeDateRange.get(1);
			Date now = new Date();

			if(startDate.before(now) && endDate.after(now)) {
				bundlesThatApplyToToday.put(endDate.getTime() - startDate.getTime(), bundleDirectory);
			}
		}

		// if there are more than one applicable bundle, choose the one with the most applicability into the future
		if(bundlesThatApplyToToday.size() > 1) {
			List<Long> timeIntoFutureThatBundlesApply = new ArrayList<Long>(bundlesThatApplyToToday.keySet());
			Collections.sort(timeIntoFutureThatBundlesApply);
			return bundlesThatApplyToToday.get(timeIntoFutureThatBundlesApply.get(timeIntoFutureThatBundlesApply.size() - 1));
			
		// if there's one bundle, use that one!
		} else if(bundlesThatApplyToToday.size() > 0){
			return bundlesThatApplyToToday.get(0);
		} else
			return null;
	}
	
	@PostConstruct
	public void setup() throws Exception {
		buildBundleEffectiveDateMap();
		
		changeBundle(chooseBestBundleForToday());
	}
	
	@Override
	public void changeBundle(String bundleId) throws Exception {
	  if(_bundleRootPath == null) {
	    throw new Exception("Your bundleRootPath is not specified in data-sources.xml!");
	  }
	  
		File path = new File(_bundleRootPath, bundleId);

		if(!path.exists()) {
			_log.error("Bundle path " + path + " does not exist; not switching bundle.");
			return;
		}
		
		_log.info(">> Switching to " + path + "...");
		
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
		_refreshService.refresh(RefreshableResources.TRANSFER_PATTERNS);
		_refreshService.refresh(RefreshableResources.NARRATIVE_DATA);

		_nycBundle.setPath(path);
		_refreshService.refresh(NycRefreshableResources.DESTINATION_SIGN_CODE_DATA);
		_refreshService.refresh(NycRefreshableResources.TERMINAL_DATA);
		_refreshService.refresh(NycRefreshableResources.RUN_DATA);

		_log.info(">> Done switching to " + path + ".");

		return;
	}
}
