package org.onebusaway.nyc.admin.service.bundle.task;

import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.GtfsMutableRelationalDao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Calendar;
import java.util.Collection;

/**
 * To help with major pick changes (bundle swaps that are completely different
 * bundles, not just updates), modify the start date of the gtfs to be start-1
 * to allow inference of overnight routes.
 * 
 *  This task must be inserted before the calendar_service task to be effective.
 *
 */
public class ModifyStartDateTask implements Runnable {
  
  private Logger _log = LoggerFactory.getLogger(ModifyStartDateTask.class);
  
  private GtfsMutableRelationalDao _gtfsMutableRelationalDao;
  
  @Autowired
  public void setGtfsMutableRelationalDao(
      GtfsMutableRelationalDao gtfsMutableRelationalDao) {
    _gtfsMutableRelationalDao = gtfsMutableRelationalDao;
  }

  public void run() {
    Collection<ServiceCalendar> calendars = _gtfsMutableRelationalDao.getAllCalendars();
    _log.info("found " + calendars.size() + " calendar entries");
    for (ServiceCalendar sc : calendars) {
      Calendar cal = Calendar.getInstance();
      cal.setTime(sc.getStartDate().getAsDate());
      cal.add(Calendar.DAY_OF_YEAR, -1);  //go back a day
      _log.info("changed calendar start date from " + sc.getStartDate().getAsDate()
          + " to " + cal.getTime());
      sc.setStartDate(new ServiceDate(cal));
    }
  }
}
