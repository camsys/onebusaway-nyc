package org.onebusaway.nyc.report_archive.impl;

import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.nyc.transit_data_federation.impl.bundle.BundleManagementServiceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Archive specific version of BundleManagementService; needs to be timezone aware. 
 *
 */
public class ArchiveBundleManagementServiceImpl extends
    BundleManagementServiceImpl {
  
  private String tz;
  private static Logger _log = LoggerFactory.getLogger(ArchiveBundleManagementServiceImpl.class);  
  
  @Override
  public ServiceDate getServiceDate() {
		if(_currentServiceDate != null) {
		  _log.debug("getServiceDate returning existing serviceDate=" + _currentServiceDate.getAsString());
			return _currentServiceDate;
		} else {
		  
		  ServiceDate sd = new ServiceDate(getAdjustedDate());
		  _log.debug("getServiceDate returning new serviceDate=" + sd.getAsString());
			return sd;
		}
	}

  // GMT is midnight, EST is 20h, don't swap bundle until EST midnight
  public Date getAdjustedDate() {
    Calendar cal = Calendar.getInstance();
    int offset = getDefaultTzOffset();
    cal.add(Calendar.MILLISECOND, offset);
    _log.info("adjustedDate=" + cal.getTime() + " for offset=" + offset + " and tz=" + tz);
    return cal.getTime();
  }
  
  public int getDefaultTzOffset() {
    String tz = getTz();
    if (tz == null || tz.length() == 0) {
      _log.info("no tz configured");
      return 0; // no offset
    } else {
      _log.info("using tz=" + getTz());
    }
    return TimeZone.getTimeZone(getTz()).getOffset(System.currentTimeMillis());
  }
  
  public String getTz() {
     return tz;
  }
  
  public void setTz(String tz) {
    this.tz = tz;
  }

}
