package org.onebusaway.nyc.stif;

import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.onebusaway.transit_data_federation.bundle.model.FederatedTransitDataBundle;
import org.onebusaway.utility.ObjectSerializationLibrary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.io.File;

public class StifTask implements Runnable {

  @Autowired
  FederatedTransitDataBundle _bundle;
  
  private File _stifPath;

  private ApplicationContext _applicationContext;

  @Autowired
  public void setApplicationContext(ApplicationContext applicationContext) {
    _applicationContext = applicationContext;
  }
  
  /**
   * The path of the directory containing STIF files to process
   */
  public void setStifPath(File path) {
    this._stifPath = path;
  }
  
  public File getStifPath() {
    return _stifPath;
  }
  
  public void run() {
    
    StifTripLoader loader = new StifTripLoader();
    GtfsRelationalDao dao = (GtfsRelationalDao) _applicationContext.getBean("springHibernateGtfsRelationalDaoImpl");
    loader.setGtfsDao(dao);
    loader.init();
    
    loadStif(_stifPath, loader);
    try {
      ObjectSerializationLibrary.writeObject(new File(_bundle.getPath(), "dscToTripMapping"), loader.getTripMapping());
    } catch (Exception ex) {
      throw new IllegalStateException("error writing graph to file", ex);
    }
  }

  public void loadStif(File path, StifTripLoader loader) {
    if (path.isDirectory()) {
      for (String filename : path.list()) {
        File contained = new File(path, filename);
        loadStif(contained, loader);
      }
    } else {
      loader.run(path);
    }
  }
}
