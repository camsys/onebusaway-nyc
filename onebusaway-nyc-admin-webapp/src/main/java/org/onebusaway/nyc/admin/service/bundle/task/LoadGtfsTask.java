package org.onebusaway.nyc.admin.service.bundle.task;

import org.onebusaway.gtfs.serialization.GtfsEntitySchemaFactory;
import org.onebusaway.gtfs.services.GenericMutableDao;
import org.onebusaway.king_county_metro_gtfs.model.PatternPair;
import org.onebusaway.transit_data_federation.bundle.tasks.GtfsReadingSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

public class LoadGtfsTask implements Runnable {

private ApplicationContext _applicationContext;
  
  private GenericMutableDao _dao;
  
  private boolean _disableStopConsolidation = false;

  @Autowired
  public void setApplicationContext(ApplicationContext applicationContext) {
    _applicationContext = applicationContext;
  }
  
  @Autowired
  public void setDao(GenericMutableDao dao) {
    _dao = dao;
  }
  
  public void setDisableStopConsolidation(boolean disable) {
    _disableStopConsolidation = disable;
  }

  @Override
  public void run() {
    try {
      
      GtfsEntitySchemaFactory.getEntityClasses().add(PatternPair.class);
      GtfsReadingSupport.readGtfsIntoStore(_applicationContext, _dao, _disableStopConsolidation);
      
    } catch (Throwable ex) {
      throw new IllegalStateException("error loading gtfs", ex);
    }

  }
  
}
