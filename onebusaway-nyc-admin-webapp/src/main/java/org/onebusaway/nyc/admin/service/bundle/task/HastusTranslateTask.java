package org.onebusaway.nyc.admin.service.bundle.task;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.nyc.admin.service.bundle.hastus.HastusGtfsFactory;
import org.onebusaway.nyc.admin.util.NYCFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HastusTranslateTask extends BaseModTask implements Runnable {

	private static final String AUX_DIR = "aux";
  private static Logger _log = LoggerFactory.getLogger(GtfsModTask.class);
	private Map<String, HastusData> _agencyMap = new HashMap<String, HastusData>();
	
	@Override
	public void run() {
		
		_log.info("HastusTranslateTask Starting");
		
		List<String> zipList = requestResponse.getResponse().getAuxZipList();
		if (zipList == null) {
		  _log.info("nothing to do");
		}
		
		for (String zipFile : zipList) {
		  _log.info("found zipFile=" + zipFile);
		  buildAgencyMap(zipFile);
		}
		
		for (String agencyId : _agencyMap.keySet()) {
		  HastusData hd = _agencyMap.get(agencyId);
		  _log.info("creating gtfs for " + hd);
      createGtfs(hd);
    }

		_log.info("HastusTranslateTask Exiting");
	}

  private void createGtfs(HastusData hd) {
    if (hd == null) {
      _log.info("nothing to do");
      return;
    }
    if (!hd.isValid()) {
      _log.info("incomplete hd=" + hd);
      return;
    }
    
    File hastus = new File(hd.getScheduleDataDirectory());
    File gis = new File(hd.getGisDataDirectory());

    try {
      HastusGtfsFactory factory = new HastusGtfsFactory();
      if (hastus != null && gis != null) {
        factory.setScheduleInputPath(hastus);
        factory.setGisInputPath(gis);
        factory.setGtfsOutputPath(new File(requestResponse.getRequest().getTmpDirectory()));
        factory.setCalendarStartDate(new ServiceDate(requestResponse.getRequest().getBundleStartDate().toDateTimeAtStartOfDay().toDate()));
        factory.setCalendarEndDate(new ServiceDate(requestResponse.getRequest().getBundleEndDate().toDateTimeAtStartOfDay().toDate()));
        factory.run();
        logger.changelog("Packaged " + hastus + " and " + gis + " to GTFS to support Community Transit");
      } else {
        _log.error("missing required inputs: hastus=" + hastus + ", gis=" + gis);
      }
    } catch (Throwable ex) {
      _log.error("error packaging Community Transit gtfs:", ex);
    }
    
  }

  private void buildAgencyMap(String zipFile) {
    File auxFilePath = new File(zipFile);
    if (auxFilePath.exists() && auxFilePath.getName().contains("_")) {
      NYCFileUtils fu = new NYCFileUtils();
      String agencyId = fu.parseAgency(auxFilePath.toString());
      
      HastusData hd = null;
      if (agencyId != null) {
        hd = _agencyMap.get(agencyId);
      }
      if (hd == null) {
        hd = new HastusData();
        hd.setAgencyId(agencyId);
        _agencyMap.put(agencyId, hd);
      }
      
      if (zipFile.toUpperCase().contains("HASTUS")) {
        hd.setScheduleDataDirectory(createScheduleDataDir(zipFile));
      } else if (zipFile.toUpperCase().contains("GIS")) {
        hd.setGisDataDirectory(createGisDataDir(zipFile));
      }
    }
  }

  private String createGisDataDir(String file) {
    NYCFileUtils fu = new NYCFileUtils();
    _log.info("expanding " + file);
    String dir = fu.parseDirectory(file);
    String auxDir = dir + File.separator + AUX_DIR;
    fu.unzip(file, auxDir);
    File[] files = new File(auxDir).listFiles();
    if (files != null) {
      for (File checkDir : files) {
        if (checkDir.exists() && checkDir.isDirectory()) {
          if (checkDir.getName().toUpperCase().contains("GIS")) {
            _log.info("gis data dir=" + checkDir);
            return checkDir.toString();
          }
        }
      }
    }
    _log.error("could not find gis data dir");
    return null;
  }

  private String createScheduleDataDir(String file) {
    NYCFileUtils fu = new NYCFileUtils();
    _log.info("expanding " + file);
    String dir = fu.parseDirectory(file);
    String auxDir = dir + File.separator + AUX_DIR;
    fu.unzip(file, auxDir);
    File[] files = new File(auxDir).listFiles();
    if (files != null) {
      for (File checkDir : files) {
        if (checkDir.exists() && checkDir.isDirectory()) {
          if (checkDir.getName().toUpperCase().contains("ROUTES")) {
            _log.info("routes data dir=" + checkDir);
            return checkDir.toString();
          }
        }
      }
    }
    _log.error("could not find routes data dir");
    return null;
  }
}