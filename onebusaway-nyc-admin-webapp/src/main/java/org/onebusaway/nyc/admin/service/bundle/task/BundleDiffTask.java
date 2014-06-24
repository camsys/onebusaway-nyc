package org.onebusaway.nyc.admin.service.bundle.task;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.onebusaway.nyc.util.configuration.ConfigurationServiceClient;
import org.springframework.beans.factory.annotation.Autowired;

public class BundleDiffTask extends DiffTask {
	
	private String bundleBuildPath;
	ConfigurationServiceClient configurationServiceClient;
	
	final String ADD_PREFIX = "<div class=\"greenListData\">";
	final String ADD_SUFFIX = "</div>";
	final String REMOVE_PREFIX = "<div class=\"redListData\">";
	final String REMOVE_SUFFIX = "</div>";
	final String DESCRIPTOR_PREFIX = "<div class=\"blueListData\">";
	final String DESCRIPTOR_SUFFIX = "</div>";

	@Autowired
	public void setConfigurationServiceClient(ConfigurationServiceClient configurationServiceClient) {
		this.configurationServiceClient = configurationServiceClient;
	}
	private final String FILENAME = "gtfs_stats.csv";
	
	@Override
	public void initFilename(){
		try{
			_filename1 = "/var/lib/obanyc/bundles/staged" 
			    + File.separator
			    + "prod"
			    + File.separator
			    + "outputs"
			    + File.separator
			    + FILENAME;
			_filename2 = bundleBuildPath + File.separator + FILENAME;
			context = 5;
		} catch(Exception e){
			_log.error(e.toString());
		}
	}
	
	public void setBundleBuildPath(String bundleBuildPath) {
		this.bundleBuildPath = bundleBuildPath;
	}
	
	public List<String> diff(){
		initFilename();
		try {
            return super.diff();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void setFilename1(String filename){
		_filename1 = filename;
	}

	@Override
	List<String> transform(List<String> preTransform) {
		List<String> diffResult = new LinkedList<String>(); 
		for(String line: preTransform.subList(2, preTransform.size())){
			if (line.startsWith("+")){
				line = ADD_PREFIX + line + ADD_SUFFIX;
			}
			else if (line.startsWith("-")){
				line = REMOVE_PREFIX + line + REMOVE_SUFFIX;
			}
			else if(line.startsWith("@")){
				line = DESCRIPTOR_PREFIX + line + DESCRIPTOR_SUFFIX;
			}
			diffResult.add(line);
		}
		return diffResult;
	}
}