package org.onebusaway.nyc.admin.service.bundle.task;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

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
		GtfsBundles gtfsBundles = getGtfsBundles(_applicationContext);
		File hastus = null;
		File gis = null;

		try {
			for (GtfsBundle gtfsBundle : gtfsBundles.getBundles()) {
				if(gtfsBundle.getDefaultAgencyId().equals("29")){
					if (gtfsBundle.getPath().getPath().contains("Hastus")){
						hastus = gtfsBundle.getPath();
						unzip(hastus);
						
					}
					else if (gtfsBundle.getPath().getPath().contains("Routes&Stops")){
						gis = gtfsBundle.getPath();
						unzip(gis);
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
	}
	
	//TODO: fix unzip method
	private void unzip(File zipFile){
		final int BUFFER = 2048;
	      try {
	         BufferedOutputStream dest = null;
	         FileInputStream fis = new 
		   FileInputStream(zipFile);
	         ZipInputStream zis = new 
		   ZipInputStream(new BufferedInputStream(fis));
	         ZipEntry entry;
	         while((entry = zis.getNextEntry()) != null) {
	            System.out.println("Extracting: " +entry);
	            int count;
	            byte data[] = new byte[BUFFER];
	            // write the files to the disk
	            FileOutputStream fos = new 
		      FileOutputStream(entry.getName());
	            dest = new 
	              BufferedOutputStream(fos, BUFFER);
	            while ((count = zis.read(data, 0, BUFFER)) 
	              != -1) {
	               dest.write(data, 0, count);
	            }
	            dest.flush();
	            dest.close();
	         }
	         zis.close();
	      } catch(Exception e) {
	         e.printStackTrace();
	      }
	   }
	}