package org.onebusaway.nyc.admin.service.bundle.task;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.onebusaway.gtfs_merge.GtfsMerger;
import org.onebusaway.gtfs_merge.strategies.AgencyMergeStrategy;
import org.onebusaway.gtfs_merge.strategies.EDuplicateDetectionStrategy;
import org.onebusaway.gtfs_merge.strategies.EDuplicateRenamingStrategy;
import org.onebusaway.gtfs_merge.strategies.RouteMergeStrategy;
import org.onebusaway.gtfs_merge.strategies.ServiceCalendarMergeStrategy;
import org.onebusaway.gtfs_merge.strategies.StopMergeStrategy;
import org.onebusaway.gtfs_merge.strategies.TripMergeStrategy;
import org.onebusaway.nyc.admin.model.BundleRequestResponse;
import org.onebusaway.nyc.util.configuration.ConfigurationServiceClient;
import org.onebusaway.transit_data_federation.bundle.model.GtfsBundle;
import org.onebusaway.transit_data_federation.bundle.model.GtfsBundles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/*
 * Combine all Modified GTFS data from multiple agencies into single zip file.
 */
public class GtfsMergeTask extends BaseModTask implements Runnable {

	private static Logger _log = LoggerFactory.getLogger(GtfsMergeTask.class);
	

	public void run() {
	  if (!requestResponse.getRequest().getConsolidateFlag()) {
	    _log.info("consolidate flag not set, extiting");
	  }
		_log.info("GtfsMergeTask Starting with outputDirectory=" + getOutputDirectory());
		try {			
		  
			_log.info("Started merging modified GTFS feeds.");
			GtfsBundles gtfsBundles = getGtfsBundles(_applicationContext);
			List<File> inputPaths = new ArrayList<File>();
			String outputLocation = "/tmp/gtfs_merged_mod.zip";			
			if (getOutputDirectory() != null) {
				outputLocation = getOutputDirectory() + File.separator + "gtfs_merged_mod.zip";		   
			}
			int i = 0;
			for (GtfsBundle gtfsBundle : gtfsBundles.getBundles()) {	
				if(gtfsBundle.getPath() != null){
					_log.info("addiing agency data file path for agency[" + i + "]=" + gtfsBundle.getPath());				
					inputPaths.add(gtfsBundle.getPath());
				}else{
					_log.info("null file path for agency.");
				}
			}
						
			//Now call GTFS merger
			GtfsMerger feedMerger = new GtfsMerger();
			AgencyMergeStrategy agencyStrategy = new AgencyMergeStrategy();
			// agencies aren't duplicates, its by design
			agencyStrategy.setDuplicateDetectionStrategy(EDuplicateDetectionStrategy.FUZZY);
      feedMerger.setAgencyStrategy(agencyStrategy);
      
			StopMergeStrategy stopStrategy = new StopMergeStrategy();
			stopStrategy.setDuplicateRenamingStrategy(EDuplicateRenamingStrategy.AGENCY);
			feedMerger.setStopStrategy(stopStrategy);
			
			RouteMergeStrategy routeStrategy = new RouteMergeStrategy();
			routeStrategy.setDuplicateRenamingStrategy(EDuplicateRenamingStrategy.AGENCY);
      feedMerger.setRouteStrategy(routeStrategy);
			
      ServiceCalendarMergeStrategy serviceCalendarStrategy = new ServiceCalendarMergeStrategy();
      serviceCalendarStrategy.setDuplicateRenamingStrategy(EDuplicateRenamingStrategy.AGENCY);
      feedMerger.setServiceCalendarStrategy(serviceCalendarStrategy);
      
      
      TripMergeStrategy tripStrategy = new TripMergeStrategy();
      tripStrategy.setDuplicateRenamingStrategy(EDuplicateRenamingStrategy.AGENCY);
      feedMerger.setTripStrategy(tripStrategy);
      
			feedMerger.run(inputPaths, new File(outputLocation));
			
		} catch (Throwable ex) {
			_log.error("Error merging gtfs:", ex);
		} finally {
			_log.info("GtfsMergeTask Exiting");
		}
	}
}
