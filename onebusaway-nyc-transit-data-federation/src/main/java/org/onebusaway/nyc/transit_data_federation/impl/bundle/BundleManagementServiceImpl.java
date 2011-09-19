package org.onebusaway.nyc.transit_data_federation.impl.bundle;

import java.io.File;

import javax.annotation.PostConstruct;

import org.onebusaway.container.refresh.RefreshService;
import org.onebusaway.nyc.transit_data_federation.bundle.model.NycFederatedTransitDataBundle;
import org.onebusaway.transit_data_federation.bundle.model.FederatedTransitDataBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class BundleManagementServiceImpl {

	private static Logger _log = LoggerFactory.getLogger(BundleManagementServiceImpl.class);

	@Autowired
	private NycFederatedTransitDataBundle _nycBundle;
	
	@Autowired
	private FederatedTransitDataBundle _bundle;
	
	@Autowired
	private RefreshService _refreshService;
	
	private String _path;
	
	@SuppressWarnings("unused")
	@PostConstruct
	private void updateBundlePaths() {
		File path = new File(_path, "bundle1");
		
		_nycBundle.setPath(path);
		_refreshService.refresh(NycRefreshableResources.DESTINATION_SIGN_CODE_DATA);
		_refreshService.refresh(NycRefreshableResources.TERMINAL_DATA);

		_bundle.setPath(path);
		
		
		return;
	}

	public String getPath() {
		return _path;
	}

	public void setPath(String path) {
		this._path = path;
	}
}
