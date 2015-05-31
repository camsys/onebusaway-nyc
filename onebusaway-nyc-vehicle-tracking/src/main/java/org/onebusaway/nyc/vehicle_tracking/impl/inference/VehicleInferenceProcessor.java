package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.impl.tdm.DummyOperatorAssignmentServiceImpl;
import org.onebusaway.nyc.transit_data_federation.model.bundle.BundleItem;
import org.onebusaway.nyc.transit_data_federation.services.bundle.InferenceProcessor;
import org.onebusaway.nyc.transit_data_federation.services.predictions.PredictionIntegrationService;
import org.onebusaway.nyc.vehicle_tracking.model.InferenceOutputState;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestInferredLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.services.queue.OutputQueueSenderService;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.realtime.api.VehicleLocationListener;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Strings;
import com.jhlabs.map.proj.ProjectionException;

public class VehicleInferenceProcessor extends Thread implements InferenceProcessor  {
  
  private static Logger _log = LoggerFactory.getLogger(VehicleInferenceProcessor.class);
  private static final int MAX_SURGE = 100;
  private VehicleInferenceInstance _inferenceInstance;
  private OutputQueueSenderService _outputQueueSenderService;
  private NycTransitDataService _nycTransitDataService;
  private VehicleLocationListener _vehicleLocationListener;
  private PredictionIntegrationService _predictionIntegrationService;
  private ObservationCache _observationCache;

  private ArrayBlockingQueue<NycRawLocationRecord> _inqueue = new ArrayBlockingQueue<NycRawLocationRecord>(MAX_SURGE);
  private ArrayBlockingQueue<NycTestInferredLocationRecord> _testQueue = new ArrayBlockingQueue<NycTestInferredLocationRecord>(MAX_SURGE);
  
  private ArrayBlockingQueue<InferenceOutputState> _outqueue = null;
  
  private AgencyAndId _vehicleId;
  private BundleItem _currentBundle;
  private String _depotId;
  private boolean _bypass = false;
  
  private boolean _simulation = false;
  private boolean _checkAge = false;
  private int _ageLimit = 300;
  private boolean _useTimePredictions = false;
  private boolean _running = true;
  private boolean _exitFlag = false;


  public void setVehicleInferenceInstance(VehicleInferenceInstance newInstance) {
    _inferenceInstance = newInstance;
  }

  @Override
  public void run() {
    try {
    while (!Thread.currentThread().isInterrupted() && !_exitFlag) {
      try {

        NycTestInferredLocationRecord nycTestInferredLocationRecord = null;
        NycRawLocationRecord nycRawLocationRecord = null;

        if (_simulation) {
          nycTestInferredLocationRecord = _testQueue.poll(1, TimeUnit.SECONDS);
          if (nycTestInferredLocationRecord == null) 
            continue; // check _exitFlag
        } else {
          nycRawLocationRecord = _inqueue.poll(1, TimeUnit.SECONDS);
          if (nycRawLocationRecord == null) 
            continue; // check exitFlag
        }
        
        // operator assignment service in simulation case: returns a 1:1 result
        // to what the trace indicates is the
        // true operator assignment.
        if (_simulation) {
          final DummyOperatorAssignmentServiceImpl opSvc = new DummyOperatorAssignmentServiceImpl();

          final String assignedRun = nycTestInferredLocationRecord.getAssignedRunId();
          final String operatorId = nycTestInferredLocationRecord.getOperatorId();
          if (!Strings.isNullOrEmpty(assignedRun)
              && !Strings.isNullOrEmpty(operatorId)) {
            final String[] runParts = assignedRun.split("-");
            if (runParts.length > 2) {
              opSvc.setOperatorAssignment(
                  new AgencyAndId(
                      nycRawLocationRecord.getVehicleId().getAgencyId(),
                      operatorId), runParts[1] + "-" + runParts[2], runParts[0]);

            } else {
              opSvc.setOperatorAssignment(
                  new AgencyAndId(
                      nycRawLocationRecord.getVehicleId().getAgencyId(),
                      operatorId), runParts[1], runParts[0]);
            }
          }

          _inferenceInstance.setOperatorAssignmentService(opSvc);
          _log.warn("Set operator assignment service to dummy for debugging!");
        }

        // bypass/process record through inference
        boolean inferenceSuccess = false;
        NycQueuedInferredLocationBean record = null;
        if (nycRawLocationRecord != null) {
          if (_bypass == true) {
            record = _inferenceInstance.handleBypassUpdateWithResults(nycTestInferredLocationRecord);
          } else {
            record = _inferenceInstance.handleUpdateWithResults(nycRawLocationRecord);
          }
        }
        if (record != null)
          inferenceSuccess = true;

        if (inferenceSuccess) {
          // send input "actuals" as inferred result to output queue to bypass
          // inference process
          if (_bypass == true) {
            record.setVehicleId(_vehicleId.toString());

            // fix up the service date if we're shifting the dates of the record
            // to now, since
            // the wrong service date will make the TDS not match the trip
            // properly.
            if (record.getServiceDate() == 0) {
              final GregorianCalendar gc = new GregorianCalendar();
              gc.setTime(nycTestInferredLocationRecord.getTimestampAsDate());
              gc.set(Calendar.HOUR_OF_DAY, 0);
              gc.set(Calendar.MINUTE, 0);
              gc.set(Calendar.SECOND, 0);
              record.setServiceDate(gc.getTimeInMillis());
            }

            enqueue(record);
            

            // send inferred result to output queue
          } else {
            // add some more properties to the record before sending off
            record.setVehicleId(_vehicleId.toString());
            record.getManagementRecord().setInferenceEngineIsPrimary(
                _outputQueueSenderService.getIsPrimaryInferenceInstance());
            record.getManagementRecord().setDepotId(getDepotId());

            // Process TDS Fields - OBANYC-2184 (Previously Archive
            // PostProceess)
            postProcess(record);

            final BundleItem currentBundle = getCurrentBundle();
            if (currentBundle != null) {
              record.getManagementRecord().setActiveBundleId(
                  currentBundle.getId());
            }

            enqueue(record);
          }
        }
      } catch (final ProjectionException e) {
        // discard this one
      } catch (final Throwable ex) {
        _log.error(
            "Error processing new location record for inference on vehicle "
                + _vehicleId + ": ", ex);
        resetVehicleLocation(_vehicleId);
      }
    } //end while
    _log.error(_vehicleId + " exiting");
    resetVehicleLocation(_vehicleId);
    } catch (final Throwable ex) {
      _log.error("Processor broke for vehicle " + _vehicleId, ex);
      resetVehicleLocation(_vehicleId);
    }
  }

  private void enqueue(NycQueuedInferredLocationBean record) throws InterruptedException {
    _outputQueueSenderService.enqueue(record);
    InferenceOutputState state = new InferenceOutputState();
    state.setNycQueuedInferredLocationBean(record);
    state.setNycTestInferredLocationRecord(_inferenceInstance.getCurrentState());
    state.setVehicleId(_vehicleId);
    state.setCurrentParticle(_inferenceInstance.getCurrentParticles());
    state.setCurrentSampledParticles(_inferenceInstance.getCurrentSampledParticles());
    state.setCurrentJourneySummaries(_inferenceInstance.getJourneySummaries());
    state.setVehicleLocationDetails(_inferenceInstance.getDetails());
    state.setBadParticleDetails(_inferenceInstance.getBadParticleDetails());
    _outqueue.put(state);
    
  }

  /**
   * This method is used to populate additional record values using the TDS
   */
  protected void postProcess(NycQueuedInferredLocationBean record){
  // Populate TDS Fields (VLR)
  processRecord(record);  
    
  // Process TDS Fields - OBANYC-2184 (Previously Archive PostProceess)  
  VehicleStatusBean vehicle = _nycTransitDataService.getVehicleForAgency(
      record.getVehicleId(), new Date(record.getRecordTimestamp()).getTime());
  record.setVehicleStatusBean(vehicle);
  
  VehicleLocationRecordBean vehicleLocation = _nycTransitDataService.getVehicleLocationRecordForVehicleId(
      record.getVehicleId(), new Date(record.getRecordTimestamp()).getTime());
  
  record.setVehicleLocationRecordBean(vehicleLocation);
  
  }
  
  /**
   * This method is used to generate values for the TDS
   */
  protected void processRecord(NycQueuedInferredLocationBean inferredResult) {
    VehicleLocationRecord vlr = new VehicleLocationRecord();
    if (_checkAge) {
      long difference = computeTimeDifference(inferredResult.getRecordTimestamp());
      if (difference > _ageLimit) {
        _log.info("VehicleLocationRecord for "+ inferredResult.getVehicleId() + " discarded.");
        return;
      }
    }
    vlr.setVehicleId(AgencyAndIdLibrary.convertFromString(inferredResult.getVehicleId()));
    vlr.setTimeOfRecord(inferredResult.getRecordTimestamp());
    vlr.setTimeOfLocationUpdate(inferredResult.getRecordTimestamp());
    vlr.setBlockId(AgencyAndIdLibrary.convertFromString(inferredResult.getBlockId()));
    vlr.setTripId(AgencyAndIdLibrary.convertFromString(inferredResult.getTripId()));
    vlr.setServiceDate(inferredResult.getServiceDate());
    vlr.setDistanceAlongBlock(inferredResult.getDistanceAlongBlock());
    vlr.setCurrentLocationLat(inferredResult.getInferredLatitude());
    vlr.setCurrentLocationLon(inferredResult.getInferredLongitude());
    vlr.setPhase(EVehiclePhase.valueOf(inferredResult.getPhase()));
    vlr.setStatus(inferredResult.getStatus());
    if (_vehicleLocationListener != null) {
      _vehicleLocationListener.handleVehicleLocationRecord(vlr);
    }
    if (_useTimePredictions) {
      // if we're updating time predictions with the generation service,
      // tell the integration service to fetch
      // a new set of predictions now that the TDS has been updated
      // appropriately.
      _predictionIntegrationService.updatePredictionsForVehicle(vlr.getVehicleId());
    }
    
  }
  
  public void resetVehicleLocation(AgencyAndId vid) {
    _log.error("RESET[" + vid + "]");
    _running = false;
    _observationCache.purge(vid);
  }

  
  private long computeTimeDifference(long timestamp) {
    return (System.currentTimeMillis() - timestamp) / 1000; // output in seconds
  }
  private BundleItem getCurrentBundle() {
    return _currentBundle;
  }

  private String getDepotId() {
    return _depotId;
  }

  public void enqueue(NycRawLocationRecord r) {
    if (_inqueue.size() > 0) {
      _log.warn("output[" + _vehicleId + "] size: " + _inqueue.size());
    }
    _inqueue.add(r);
    
  }

  public void enqueue(NycTestInferredLocationRecord record) {
    _testQueue.add(record);
  }

  
  public void setVehicleId(AgencyAndId vehicleId) {
    _vehicleId = vehicleId;
  }

  public void setOutputQueueSenderService(OutputQueueSenderService outputQueue) {
    this._outputQueueSenderService = outputQueue;
  }

  public void setNycTransitDataService(
      NycTransitDataService nycTransitDataService) {
    _nycTransitDataService = nycTransitDataService;
  }

  public void setCheckAge(boolean checkAge) {
    _checkAge = checkAge;
  }

  public void setAgeLimit(int ageLimit) {
    _ageLimit = ageLimit;    
  }

  public void setVehicleLocationListener(
      VehicleLocationListener vehicleLocationListener) {
    _vehicleLocationListener = vehicleLocationListener;
  }

  public void setPredictionIntegrationService(
      PredictionIntegrationService predictionIntegrationService) {
    _predictionIntegrationService = predictionIntegrationService;
  }

  public void setUseTimePredictions(boolean useTimePredictions) {
    _useTimePredictions = useTimePredictions;
  }

  @Override
  public void shutdown() {
    _exitFlag = true;
    
  }

  public void setObservationCache(ObservationCache observationCache) {
    _observationCache = observationCache;
  }

  public void setOutQueue(ArrayBlockingQueue<InferenceOutputState> outQueue) {
    _outqueue = outQueue;
  }

  public boolean isDone() {
    return !_running;
  }

  @Override
  public boolean isCancelled() {
    return !_running;
  }

  public void setCurrentBundle(BundleItem currentBundle) {
    _currentBundle = currentBundle;
  }

  public void setDepotId(String depotId) {
    _depotId = depotId;
  }

}
