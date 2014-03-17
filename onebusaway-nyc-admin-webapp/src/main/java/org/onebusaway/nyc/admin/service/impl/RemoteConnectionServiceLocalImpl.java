package org.onebusaway.nyc.admin.service.impl;

import java.io.File;
import java.io.FileNotFoundException;

import javax.ws.rs.core.Response;

import org.onebusaway.nyc.admin.service.RemoteConnectionService;
import org.onebusaway.nyc.transit_data_manager.bundle.BundleProvider;
import org.onebusaway.nyc.transit_data_manager.bundle.model.BundleMetadata;
import org.onebusaway.nyc.util.configuration.ConfigurationServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class RemoteConnectionServiceLocalImpl implements
		RemoteConnectionService {

	private static Logger _log = LoggerFactory.getLogger(RemoteConnectionServiceLocalImpl.class);
	
    private ConfigurationServiceClient _configClient;
    public void setConfigurationServiceClient(ConfigurationServiceClient configClient) {
    	_configClient = configClient;
    }
    
    private BundleProvider _bundleProvider;
    public void setBundleProvider(BundleProvider bundleProvider) {
      _bundleProvider = bundleProvider;
    }

	@Override
	public String getContent(String url) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T postBinaryData(String url, File data, Class<T> responseType) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getList(String environment) {
		String bundleStagingProp = null;
		try {
			bundleStagingProp = _configClient.getItem("admin", "bundleStagingDir");
		} catch (Exception e) {
			_log.error("error looking up bundleStagingDir:", e);
		}
		if (bundleStagingProp == null) {
			_log.error("expecting bundleStagingDir property from config, Failing");
			return null;
		}
		File stagingDirectory = new File(bundleStagingProp);
		if (stagingDirectory.exists() && stagingDirectory.isDirectory()) {
			String[] bundles = stagingDirectory.list();
			String json = "[";
			for (String bundle : bundles) {
				if (!"[".equals(json)) {
					json = json + ", ";
				}
				json = json + "\"" + bundle + "\"";
			}
			return json+"]";
		}
		return null;
	}

  public BundleMetadata getStagedBundleMetadata() throws Exception {
    String bundleStagingProp = null;
    try {
      bundleStagingProp = _configClient.getItem("admin", "bundleStagingDir");
    } catch (Exception e) {
      _log.error("error looking up bundleStagingDir:", e);
    }
    if (bundleStagingProp == null) {
      _log.error("expecting bundleStagingDir property from config, Failing");
      return null;
    }
    File stagingDirectory = new File(bundleStagingProp);
    if (!stagingDirectory.exists() || !stagingDirectory.isDirectory()) {
      _log.error("expecting bundleStagingDir directory to exist: " + stagingDirectory);
      return null;
    }
    
    return _bundleProvider.getMetadata(stagingDirectory.toString());
  }

  public File getStagedBundleFile(String relativeFilePath) {
    String bundleStagingProp = null;
    try {
      bundleStagingProp = _configClient.getItem("admin", "bundleStagingDir");
    } catch (Exception e) {
      _log.error("error looking up bundleStagingDir:", e);
    }
    if (bundleStagingProp == null) {
      _log.error("expecting bundleStagingDir property from config, Failing");
      return null;
    }
    try {
      return _bundleProvider.getStagedBundleFile(bundleStagingProp, relativeFilePath);
    } catch (FileNotFoundException e) {
      _log.error("error retrieving file " + bundleStagingProp + File.separator + relativeFilePath);
      return null;
    }
  }
  
}
