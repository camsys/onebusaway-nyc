package org.onebusaway.nyc.admin.service.bundle.task;

import org.onebusaway.nyc.admin.model.BundleRequestResponse;
import org.onebusaway.nyc.util.configuration.ConfigurationServiceClient;
import org.springframework.beans.factory.annotation.Autowired;


public class ValidationTask extends DiffTask {
	BundleRequestResponse bundleRequestResponse;
	ConfigurationServiceClient configurationServiceClient;
	
	@Autowired
	public void setBundleRequestResponse(BundleRequestResponse bundleRequestResponse) {
		this.bundleRequestResponse = bundleRequestResponse;
	}

	@Autowired
	public void setConfigurationServiceClient(
			ConfigurationServiceClient configurationServiceClient) {
		this.configurationServiceClient = configurationServiceClient;
	}
	private final String FILENAME = "gtfs_stats.csv";
	
	@Override
	public void initFilename(){
		try{
			diff_log_filename = "diff_log.csv";
			filename1 = configurationServiceClient.getItem("admin", "bundleStagingDir")+"/"+FILENAME;
			filename2 = bundleRequestResponse.getResponse().getBundleOutputDirectory()+"/"+FILENAME;
		} catch(Exception e){
			_log.error(e.toString());
		}
	}
}