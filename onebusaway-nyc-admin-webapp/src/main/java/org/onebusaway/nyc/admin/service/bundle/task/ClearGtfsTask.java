package org.onebusaway.nyc.admin.service.bundle.task;

import org.onebusaway.gtfs.impl.GenericDaoImpl;
import org.onebusaway.gtfs.services.GenericMutableDao;
import org.springframework.beans.factory.annotation.Autowired;

public class ClearGtfsTask implements Runnable {

  private GenericMutableDao _dao;
  
  @Autowired
  public void setDao(GenericMutableDao dao) {
    _dao = dao;
  }
  
  @Override
  public void run() {
    try {
      ((GenericDaoImpl)_dao).clear();
    } catch (Throwable ex) {
      throw new IllegalStateException("error loading gtfs", ex);
    }
  }
}
