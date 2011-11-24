package org.onebusaway.nyc.report_archive.queue;

import org.onebusaway.nyc.report_archive.model.ArchivedInferredLocationRecord;
import org.onebusaway.nyc.report_archive.services.NycQueuedInferredLocationDao;
import org.onebusaway.nyc.report_archive.model.NycVehicleManagementStatusRecord;
import org.onebusaway.nyc.report_archive.services.NycVehicleManagementStatusDao;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.nyc.transit_data.model.NycVehicleManagementStatusBean;
import org.onebusaway.nyc.transit_data_federation.impl.queue.InferenceQueueListenerTask;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.transit_data.services.TransitDataService;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;

import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArchivingInferenceQueueListenerTask extends InferenceQueueListenerTask {

  private static Logger _log = LoggerFactory.getLogger(ArchivingInferenceQueueListenerTask.class);

  @Autowired
  private NycQueuedInferredLocationDao _locationDao;
  @Autowired
  private NycVehicleManagementStatusDao _statusDao;
  @Autowired
  private TransitDataService _transitDataService;

  @Refreshable(dependsOn = {"inference-engine.outputQueueHost", "inference-engine.outputQueuePort", 
      "inference-engine.outputQueueName"})
  @Override
  public void startListenerThread() {
    if(_initialized == true) {
      _log.warn("Configuration service tried to reconfigure inference output queue service; this service is not reconfigurable once started.");
      return;
    }

    String host = _configurationService.getConfigurationValueAsString("inference-engine.outputQueueHost", null);
    String queueName = _configurationService.getConfigurationValueAsString("inference-engine.outputQueueName", null);
    Integer port = _configurationService.getConfigurationValueAsInteger("inference-engine.outputQueuePort", 5567);

    if(host == null || queueName == null || port == null) {
      _log.info("Inference input queue is not attached; input hostname was not available via configuration service.");
      return;
    }

    initializeZmq(host, queueName, port);
  }

  @Override
  // this method can't throw exceptions or it will stop the queue
  // listening
  protected void processResult(NycQueuedInferredLocationBean inferredResult, String contents) {
    try {
      ArchivedInferredLocationRecord locationRecord = new ArchivedInferredLocationRecord(inferredResult, contents);
      postProcess(locationRecord);
      _locationDao.saveOrUpdateRecord(locationRecord);
    } catch (Throwable t) {
      _log.error("Exception processing contents= " + contents, t);
    }
  }
 
  private void postProcess(ArchivedInferredLocationRecord locationRecord) { 
    // Extract next stop id and distance
    String vehicleId = locationRecord.getAgencyId() + "_" + locationRecord.getVehicleId().toString();
    VehicleStatusBean vehicle = _transitDataService.getVehicleForAgency(vehicleId, System.currentTimeMillis());

    TripStatusBean status = vehicle.getTripStatus();

    if ((status == null) || (status.getNextStop() == null))
	_log.debug("Null trip status. Skipping TDS values."); // Common case, particularly, e.g. when dead heading
    else {
	locationRecord.setNextScheduledStopId(status.getNextStop().getId());
	locationRecord.setNextScheduledStopDistance(status.getNextStopDistanceFromVehicle());

	TripBean trip = status.getActiveTrip();
	if (trip != null) {
	    locationRecord.setInferredTripId(trip.getId());
	    RouteBean route = trip.getRoute();
	    if (route != null) {
		locationRecord.setInferredRouteId(route.getId());
	    }
	}
	StopBean stop = status.getNextStop();
	if (stop != null) {
	    //locationRecord.setDirectionDeg(stop.getDirection());
	    locationRecord.setNextScheduledStopId(stop.getId());
	}
	locationRecord.setNextScheduledStopDistance(status.getNextStopDistanceFromVehicle());
	locationRecord.setScheduleDeviation((int)new Double(status.getScheduleDeviation()).longValue());
    }
  }

  @PostConstruct
  public void setup() {
    super.setup();
  }
  
  @PreDestroy 
  public void destroy() {
    super.destroy();
  }


}
