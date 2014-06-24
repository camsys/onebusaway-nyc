package org.onebusaway.nyc.admin.service.bundle.task;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.onebusaway.nyc.admin.model.BundleRequestResponse;
import org.onebusaway.nyc.util.configuration.ConfigurationServiceClient;
import org.springframework.beans.factory.annotation.Autowired;

public class ValidationDiffTask extends DiffTask {
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
			_diff_log_filename = "diff_log.csv";
			_filename1 = configurationServiceClient.getItem("admin", "bundleStagingDir") 
			    + File.separator
			    + "prod"
			    + File.separator
			    + "outputs"
			    + File.separator
			    + FILENAME;
			_filename2 = bundleRequestResponse.getResponse().getBundleOutputDirectory() + File.separator + FILENAME;
		} catch(Exception e){
			_log.error(e.toString());
		}
	}
	
	@Override
	List<String> transform(List<String> preTransform) {
		List<String> diffResult = new LinkedList<String>(); 
		int minusLineNum = 0;
		int plusLineNum = 0;
		for(String line: preTransform.subList(2, preTransform.size())){
			diffResult.add(line);
			if(line.startsWith("@")){
				try{
					int firstComma = line.indexOf(",");
					minusLineNum = Integer.parseInt(line.substring(line.indexOf("-")+1, firstComma));
					plusLineNum = Integer.parseInt(line.substring(line.indexOf("+")+1, line.indexOf(",", firstComma+1)));
				} catch (Exception e){
					e.printStackTrace();
				}
			}
			else{
				if (line.startsWith("-")){
					log(minusLineNum, line);
					minusLineNum++;
				}
				else if (line.startsWith("+")){
					log(plusLineNum, line);
					plusLineNum++;
				}
			}
		}
		return diffResult;
	}
}