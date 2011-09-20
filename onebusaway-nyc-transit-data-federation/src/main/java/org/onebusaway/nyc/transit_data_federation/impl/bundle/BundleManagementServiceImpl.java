package org.onebusaway.nyc.transit_data_federation.impl.bundle;

import java.io.File;

import org.onebusaway.container.refresh.RefreshService;
import org.onebusaway.nyc.transit_data_federation.bundle.model.NycFederatedTransitDataBundle;
import org.onebusaway.nyc.transit_data_federation.services.bundle.BundleManagementService;
import org.onebusaway.transit_data_federation.bundle.model.FederatedTransitDataBundle;
import org.onebusaway.transit_data_federation.impl.RefreshableResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class BundleManagementServiceImpl implements BundleManagementService {

	private static Logger _log = LoggerFactory.getLogger(BundleManagementServiceImpl.class);

	@Autowired
	private NycFederatedTransitDataBundle _nycBundle;
	
	@Autowired
	private FederatedTransitDataBundle _bundle;
	
	@Autowired
	private RefreshService _refreshService;
	
	private String _bundleRootPath = null;
	
	public String getBundleRootPath() {
		return _bundleRootPath;
	}

	public void setBundleRootPath(String path) {
		this._bundleRootPath = path;
	}

	@Override
	public void changeBundle(String bundleId) {
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
