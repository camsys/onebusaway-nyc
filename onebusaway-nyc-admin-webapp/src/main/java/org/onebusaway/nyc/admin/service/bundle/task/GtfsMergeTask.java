package org.onebusaway.nyc.admin.service.bundle.task;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.onebusaway.gtfs_merge.GtfsMerger;
import org.onebusaway.nyc.util.configuration.ConfigurationServiceClient;
import org.onebusaway.transit_data_federation.bundle.model.GtfsBundle;
import org.onebusaway.transit_data_federation.bundle.model.GtfsBundles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/*TODO
 * This Merge Task
 * 1.Combine all Modified GTFS data from multiple agencies into single _mod.zip file. - Done
 * 2.Remove duplicates. (Keep in mind this is very important).
 * 3.GtfsMergeTask will kick in after GtfsMergeTask does its last set of transformations.
 */
public class GtfsMergeTask extends BaseModTask implements Runnable {

	private static Logger log = LoggerFactory.getLogger(GtfsMergeTask.class);
	
	@Autowired
	private ConfigurationServiceClient configurationServiceClient;
	public void setConfigurationServiceClient(ConfigurationServiceClient configurationServiceClient) {
		this.configurationServiceClient = configurationServiceClient;
	}

	public void run() {
		log.info("GtfsMergeTask Starting");
		try {			
			log.info("Started merging modified GTFS feeds.");
			GtfsBundles gtfsBundles = getGtfsBundles(_applicationContext);
			List<File> inputPaths = new ArrayList<File>();
			String outputLocation = null;			
			if (getOutputDirectory() != null) {
				outputLocation = getOutputDirectory() + File.separator + "gtfs_merged_mod.zip";		   
			}
			for (GtfsBundle gtfsBundle : gtfsBundles.getBundles()) {	
				if(gtfsBundle.getPath() != null){
					log.info("addiing agency data file path for agencies");				
					inputPaths.add(gtfsBundle.getPath());
				}else{
					log.info("null file path for agency.");
				}
			}
						
			//Now call GTFS merger
			GtfsMerger feedMerger = new GtfsMerger();
			feedMerger.run(inputPaths, new File(outputLocation));
			
		} catch (Throwable ex) {
			log.error("Error merging gtfs:", ex);
		} finally {
			log.info("GtfsMergeTask Exiting");
		}
	}
}
