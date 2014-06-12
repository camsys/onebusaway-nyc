package org.onebusaway.nyc.admin.service.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.onebusaway.nyc.admin.service.RemoteConnectionService;
import org.onebusaway.nyc.transit_data_manager.bundle.BundleProvider;
import org.onebusaway.nyc.transit_data_manager.bundle.StagingBundleProvider;
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
    
    private StagingBundleProvider _stagingBundleProvider;
    public void setStagingBundleProvider(StagingBundleProvider bundleProvider) {
      _stagingBundleProvider = bundleProvider;
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
		} else {
		  _log.error("expected property bundleStagingDir to have existing directory=" + bundleStagingProp);
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
    
    return _stagingBundleProvider.getMetadata(stagingDirectory.toString());
  }

  public File getStagedBundleFile(String relativeFilePath) {
    _log.debug("getStagedBundleFile(" + relativeFilePath + ")");
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
      return _stagingBundleProvider.getBundleFile(bundleStagingProp, relativeFilePath);
    } catch (FileNotFoundException e) {
      _log.error("error retrieving file " + bundleStagingProp + File.separator + relativeFilePath);
      return null;
    }
  }

  public boolean checkIsValidStagedBundleFile(String relativeFilename) {
    return _stagingBundleProvider.checkIsValidStagedBundleFile("prod", relativeFilename);
  }

  @Override
  public String postContent(String url, Map<String, String> params) {
    throw new UnsupportedOperationException("postContent not implemented");
  }
  
}
