package org.onebusaway.nyc.vehicle_tracking.impl;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationService;
import org.onebusaway.realtime.api.VehicleLocationListener;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Automatically exports vehicle location to a listener on a fixed time
 * interval.
 * 
 * @author bdferris
 * @see VehicleLocationService
 * @see VehicleLocationListener
 */
public class VehiclePositionExporter {

  private VehicleLocationService _vehicleLocationService;

  private VehicleLocationListener _vehiclePositionListener;

  private ScheduledExecutorService _executor;

  private int _updateFrequencey;

  @Autowired
  public void setVehiclePositionService(
      VehicleLocationService vehicleLocationService) {
    _vehicleLocationService = vehicleLocationService;
  }

  @Autowired
  public void setVehiclePostionListener(
      VehicleLocationListener vehiclePositionListener) {
    _vehiclePositionListener = vehiclePositionListener;
  }

  public void setUpdateFrequency(int updateFrequency) {
    _updateFrequencey = updateFrequency;
  }

  @PostConstruct
  public void start() {
    _executor = Executors.newSingleThreadScheduledExecutor();
    _executor.scheduleAtFixedRate(new Task(), 0, _updateFrequencey,
        TimeUnit.SECONDS);
  }

  @PreDestroy
  public void stop() {
    _executor.shutdownNow();
  }

  private class Task implements Runnable {

    @Override
    public void run() {
      List<VehicleLocationRecord> records = _vehicleLocationService.getLatestProcessedVehicleLocationRecords();
      _vehiclePositionListener.handleVehicleLocationRecords(records);
    }
  }

}
