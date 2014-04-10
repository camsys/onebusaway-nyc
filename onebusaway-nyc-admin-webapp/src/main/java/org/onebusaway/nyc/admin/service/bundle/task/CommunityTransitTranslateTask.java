package org.onebusaway.nyc.admin.service.bundle.task;

import java.io.File;

import org.onebusaway.community_transit_gtfs.CommunityTransitGtfsFactory;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.transit_data_federation.bundle.model.GtfsBundle;
import org.onebusaway.transit_data_federation.bundle.model.GtfsBundles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommunityTransitTranslateTask extends BaseModTask implements Runnable {

	private static Logger _log = LoggerFactory.getLogger(GtfsModTask.class);

	@Override
	public void run() {
		
		_log.info("CommunityTransitTranslateTask Starting");
		/*
		GtfsBundles gtfsBundles = getGtfsBundles(_applicationContext);
		File hastus = null;
		File gis = null;

		try {
			for (GtfsBundle gtfsBundle : gtfsBundles.getBundles()) {
				if(gtfsBundle.getDefaultAgencyId().equals("29")){
					if (gtfsBundle.getPath().getPath().contains("Hastus")){
						hastus = gtfsBundle.getPath();
					}
					else if (gtfsBundle.getPath().getPath().contains("Routes&Stops")){
						gis = gtfsBundle.getPath();
					}
					else{_log.error("Cannot resolve naming convention for Community Transit");}
				}
			}
			CommunityTransitGtfsFactory factory = new CommunityTransitGtfsFactory();
			if (hastus != null && gis != null){
				factory.setScheduleInputPath(hastus);
				factory.setGisInputPath(gis);
				factory.setGtfsOutputPath(new File(requestResponse.getRequest().getTmpDirectory()));
				factory.setCalendarStartDate(new ServiceDate(requestResponse.getRequest().getBundleStartDate().toDateTimeAtStartOfDay().toDate()));
				factory.setCalendarEndDate(new ServiceDate(requestResponse.getRequest().getBundleEndDate().toDateTimeAtStartOfDay().toDate()));
				factory.run();
				logger.changelog("Packaged " + hastus + " and " + gis + " to GTFS to support Community Transit");
			}
		} catch (Throwable ex) {
			_log.error("error packaging Community Transit gtfs:", ex);
		}
		*/
	}
}