package org.onebusaway.nyc.vehicle_tracking.impl;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.bundle.model.NycFederatedTransitDataBundle;
import org.onebusaway.nyc.transit_data_federation.model.RunData;
import org.onebusaway.nyc.vehicle_tracking.services.RunService;
import org.onebusaway.utility.ObjectSerializationLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RunServiceImpl implements RunService {
  private Logger _log = LoggerFactory.getLogger(RunServiceImpl.class);

  private NycFederatedTransitDataBundle _bundle;
  
  Map<AgencyAndId, RunData> runDataByTrip;

  @Autowired
  public void setBundle(NycFederatedTransitDataBundle bundle) {
    _bundle = bundle;
  }

  @PostConstruct
  public void setup() throws IOException, ClassNotFoundException {
    File path = _bundle.getTripRunDataPath();

    if (path.exists()) {
      _log.info("loading trip run data");
      runDataByTrip = ObjectSerializationLibrary.readObject(path);
    }
  }

  @Override
  public String getInitialRunForTrip(AgencyAndId trip) {
    RunData runData = runDataByTrip.get(trip);
    if (runData == null) {
    	return null;
    }
	return runData.initialRun;
  }

  @Override
  public String getReliefRunForTrip(AgencyAndId trip) {
	  RunData runData = runDataByTrip.get(trip);
	    if (runData == null) {
	    	return null;
	    }
    return runData.reliefRun;
  }

  @Override
  public int getReliefTimeForTrip(AgencyAndId trip) {
    RunData runData = runDataByTrip.get(trip);
    return (runData == null || runData.reliefRun == null) ? 0 : runData.reliefTime;
  }

}
