package org.onebusaway.nyc.report_archive.queue;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;


import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.report_archive.services.DummyPredictionOutputQueueSenderService;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.impl.queue.InferenceQueueListenerTask;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.blocks.BlockInstanceBean;
import org.onebusaway.transit_data.model.blocks.BlockStopTimeBean;
import org.onebusaway.transit_data.model.blocks.BlockTripBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedHeader;
import com.google.transit.realtime.GtfsRealtime.FeedHeader.Incrementality;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.VehicleDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import com.google.transit.realtime.GtfsRealtimeConstants;


/**
 * Test component to place dummy time predictions on time queue for retrieval by TDS and display by UIs.
 * This should not be used in production.
 * 
 * @author sheldonabrown
 *
 */
public class DummyQueueBasedPredictionGeneratorTask extends InferenceQueueListenerTask {

  private static Logger _log = LoggerFactory.getLogger(DummyQueueBasedPredictionGeneratorTask.class);
  
  @Autowired
  private NycTransitDataService _transitDataService;
  
  @Autowired
  private DummyPredictionOutputQueueSenderService _senderService;
  
  
  public Boolean generateTimePredictions() {
    return Boolean.parseBoolean(_configurationService.getConfigurationValueAsString("display.generateTimePredictions", "false"));
  }
  
  @Refreshable(dependsOn = {
      "tds.inputQueueHost", "tds.inputQueuePort", "tds.inputQueueName"})
  @Override
  public void startListenerThread() {
    if (_initialized == true) {
      _log.warn("Configuration service reconfiguring prediction inference output queue service.");
    }

    String host = getQueueHost();
    String queueName = getQueueName();
    Integer port = getQueuePort();

    if (Boolean.TRUE != generateTimePredictions()) {
      _log.error("Prediction generator disabled, exiting");
      return;
    }
    
    // TODO check if test predictions are turned on!
    if (host == null || queueName == null || port == null ) {
      _log.error("Prediction inference input queue is not attached; input hostname was not available via configuration service or service was not configured to run.");
      return;
    }
    _log.info("prediction inference archive listening on " + host + ":" + port
        + ", queue=" + queueName);
    try {
      initializeQueue(host, queueName, port);
      _log.warn("queue config:" + queueName + " COMPLETE");
    } catch (InterruptedException ie) {
      _log.error("queue " + queueName + " interrupted");
      return;
    } catch (Throwable t) {
      _log.error("queue " + queueName + " init failed:", t);
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
  @Override
  protected void processResult(NycQueuedInferredLocationBean inferredResult,
      String contents) {
    FeedMessage message = generatePredictionsForVehicle(inferredResult.getVehicleId());
    if (message != null) {
      _senderService.enqueue(message);
    } 
  }

  private FeedMessage generatePredictionsForVehicle(String vehicleId) {
    
    FeedHeader.Builder header = FeedHeader.newBuilder();
    header.setTimestamp(System.currentTimeMillis()/1000);
    header.setIncrementality(Incrementality.DIFFERENTIAL);
    header.setGtfsRealtimeVersion(GtfsRealtimeConstants.VERSION);
    FeedMessage.Builder feedMessageBuilder = FeedMessage.newBuilder();
    feedMessageBuilder.setHeader(header);
    
    
    VehicleStatusBean vehicleStatus = _transitDataService.getVehicleForAgency(vehicleId, System.currentTimeMillis());
    if(vehicleStatus == null) {
      _log.debug("no vehicle status");
      return null;
    }
    TripStatusBean tripStatus = vehicleStatus.getTripStatus();    
    if(tripStatus == null) {
      _log.debug("no trip status");
      return null;
    }
      
    
    BlockInstanceBean blockInstance = _transitDataService.getBlockInstance(tripStatus.getActiveTrip().getBlockId(), tripStatus.getServiceDate());
    if(blockInstance == null) {
      _log.debug("no block");
      return null;
    }
      
    
    List<BlockTripBean> blockTrips = blockInstance.getBlockConfiguration().getTrips();

    double distanceOfVehicleAlongBlock = 0;
    boolean foundActiveTrip = false;
    for(int i = 0; i < blockTrips.size(); i++) {
      BlockTripBean blockTrip = blockTrips.get(i);

      if(!foundActiveTrip) {
        if(tripStatus.getActiveTrip().getId().equals(blockTrip.getTrip().getId())) {
          distanceOfVehicleAlongBlock += tripStatus.getDistanceAlongTrip();

          foundActiveTrip = true;
        } else {
          // a block trip's distance along block is the *beginning* of that block trip along the block
          // so to get the size of this one, we have to look at the next.
          if(i + 1 < blockTrips.size()) {
            distanceOfVehicleAlongBlock = blockTrips.get(i + 1).getDistanceAlongBlock();
          }

          // bus has already served this trip, so no need to go further
          continue;
        }
      }

      int stopSequence = -1;
      for(BlockStopTimeBean blockStopTime : blockTrip.getBlockStopTimes()) {
        stopSequence++;
        if(blockStopTime.getDistanceAlongBlock() < distanceOfVehicleAlongBlock) {
          continue;
        }
   
        long arrivalTime = tripStatus.getServiceDate() + blockStopTime.getStopTime().getArrivalTime();
        long departureTime = tripStatus.getServiceDate() + blockStopTime.getStopTime().getDepartureTime();
        fillEntity(feedMessageBuilder, 
            vehicleStatus.getVehicleId(), 
            blockTrip.getTrip().getId(),blockTrip.getTrip().getRoute().getId(), 
            stopSequence, 
            blockStopTime.getStopTime().getStop().getId(), 
            arrivalTime, 
            departureTime,
            vehicleStatus.getLastUpdateTime());
        
      }
      
      break;
    }
    
    if (!feedMessageBuilder.isInitialized()) {
      _log.error("msg missing fields!");
    }
    return feedMessageBuilder.build();
  }
  
  @Override
  public String getQueueDisplayName() {
    return "timePredictionOutput";
  }
  
  private void fillEntity(FeedMessage.Builder feedMessageBuilder, String vehicleId, String tripId, String routeId, int stopSequence, String stopId, long arrivalTime, long departureTime, long timeReported) {
    FeedEntity.Builder entity = FeedEntity.newBuilder();
    entity.setId(vehicleId);
    
    
    GtfsRealtime.TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

    // trip
    TripDescriptor.Builder tripDescriptor = TripDescriptor.newBuilder();
    tripDescriptor.setTripId(tripId);
    tripDescriptor.setRouteId(routeId);
    tripDescriptor.setScheduleRelationship(ScheduleRelationship.SCHEDULED);
    tripUpdateBuilder.setTrip(tripDescriptor);
    
       
    //vehicle
    VehicleDescriptor.Builder vehicleDescriptor = VehicleDescriptor.newBuilder();
    vehicleDescriptor.setId(vehicleId);
    vehicleDescriptor.setLabel(AgencyAndIdLibrary.convertFromString(vehicleId).getId());
    tripUpdateBuilder.setVehicle(vehicleDescriptor);
    
    
    StopTimeUpdate.Builder stopTimeUpdateBuilder = StopTimeUpdate.newBuilder();
    stopTimeUpdateBuilder.setStopSequence(stopSequence);
    stopTimeUpdateBuilder.setStopId(stopId);
    
    
    StopTimeEvent.Builder arrival = StopTimeEvent.newBuilder();
    arrival.setTime(arrivalTime);
    stopTimeUpdateBuilder.setArrival(arrival);
    StopTimeEvent.Builder departure = StopTimeEvent.newBuilder();
    departure.setTime(departureTime);
    stopTimeUpdateBuilder.setDeparture(departure);
    
    tripUpdateBuilder.addStopTimeUpdate(stopTimeUpdateBuilder);
    entity.setTripUpdate(tripUpdateBuilder);
    feedMessageBuilder.addEntity(entity);
    // TODO timeReported -- I can't find it
  }
}
