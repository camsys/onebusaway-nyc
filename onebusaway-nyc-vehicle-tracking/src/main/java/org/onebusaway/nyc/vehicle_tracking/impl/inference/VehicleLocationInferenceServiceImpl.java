package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationInferenceRecord;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationInferenceService;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.springframework.stereotype.Component;

@Component
public class VehicleLocationInferenceServiceImpl implements
    VehicleLocationInferenceService {

  private ExecutorService _executorService;

  private int _numberOfProcessingThreads = 10;

  private ConcurrentMap<AgencyAndId, VehicleInferenceInstance> _vehicleInstancesByVehicleId = new ConcurrentHashMap<AgencyAndId, VehicleInferenceInstance>();

  public void setNumberOfProcessingThreads(int numberOfProcessingThreads) {
    _numberOfProcessingThreads = numberOfProcessingThreads;
  }

  @PostConstruct
  public void start() {
    if (_numberOfProcessingThreads <= 0)
      throw new IllegalArgumentException(
          "numberOfProcessingThreads must be positive");
    _executorService = Executors.newFixedThreadPool(_numberOfProcessingThreads);
  }

  @PreDestroy
  public void stop() {
    _executorService.shutdownNow();
  }

  @Override
  public void handleVehicleLocation(VehicleLocationInferenceRecord record) {
    _executorService.execute(new ProcessingTask(record));
  }

  @Override
  public List<VehicleLocationRecord> getLatestProcessedVehicleLocationRecords() {

    List<VehicleLocationRecord> records = new ArrayList<VehicleLocationRecord>();

    for (Map.Entry<AgencyAndId, VehicleInferenceInstance> entry : _vehicleInstancesByVehicleId.entrySet()) {

      AgencyAndId vehicleId = entry.getKey();
      VehicleInferenceInstance instance = entry.getValue();
      VehicleState state = instance.getCurrentState();

      VehicleLocationRecord record = new VehicleLocationRecord();
      record.setCurrentLocationLat(state.getLat());
      record.setCurrentLocationLon(state.getLon());
      // TODO: Is this the right time?
      record.setCurrentTime(System.currentTimeMillis());
      record.setPositionDeviation(state.getPositionDeviation());
      record.setTripId(state.getTripId());
      record.setVehicleId(vehicleId);
      records.add(record);
    }

    return records;
  }

  /****
   * Private Methods
   ****/

  private VehicleInferenceInstance getInstanceForVehicle(AgencyAndId vehicleId) {
    /**
     * Maybe there is a better idiom for the ConcurrentMap access?
     */
    VehicleInferenceInstance instance = new VehicleInferenceInstance();
    VehicleInferenceInstance existing = _vehicleInstancesByVehicleId.putIfAbsent(
        vehicleId, instance);
    if (existing == null)
      existing = instance;
    return existing;
  }

  private class ProcessingTask implements Runnable {

    private VehicleLocationInferenceRecord _record;

    public ProcessingTask(VehicleLocationInferenceRecord record) {
      _record = record;
    }

    @Override
    public void run() {
      VehicleInferenceInstance existing = getInstanceForVehicle(_record.getVehicleId());
      existing.handleUpdate(_record);
    }
  }

}
