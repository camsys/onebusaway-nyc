package org.onebusaway.nyc.transit_data.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;

/**
 * An "over the wire", queued inferred location result--gets passed between the inference
 * engine and the TDF/TDS running on all front-end notes, plus the archiver and other inference
 * data consumers.
 * 
 * @author jmaki
 *
 */
public class NycQueuedInferredLocationBean implements Serializable {

	private static final long serialVersionUID = 1L;

	// the timestamp applied to the record when received by the inference engine
	private Long recordTimestamp;

	private String vehicleId;

	// service date of trip/block
	private Long serviceDate;

	private Integer scheduleDeviation;

	private String blockId;

	private String tripId;

	private Double distanceAlongBlock;

	private Double distanceAlongTrip;

	// snapped lat/long of vehicle to route shape
	private Double inferredLatitude;

	private Double inferredLongitude;

	// raw lat/long of vehicle as reported by BHS.
	private Double observedLatitude;

	private Double observedLongitude;

	// inferred operational status/phase
	private String phase;

	private String status;

	// inference engine telemetry
	private NycVehicleManagementStatusBean managementRecord;

	private String runId;

	private String routeId;

	private Double bearing;
	
	// Fields from TDS
	
	// Stop ID of next scheduled stop
	private String nextScheduledStopId;

	// Distance to next scheduled stop
	private Double nextScheduledStopDistance;

	private String assignedRunId = null;

	private String inferredBlockId;

	private String inferredTripId;
	
	private String inferredRouteId;
	
	private String inferredDirectionId;
	
	private Long lastLocationUpdateTime;
	

	public NycQueuedInferredLocationBean() {}

	public Long getRecordTimestamp() {
		return recordTimestamp;
	}

	public void setRecordTimestamp(Long recordTimestamp) {
		this.recordTimestamp = recordTimestamp;
	}

	public String getVehicleId() {
		return vehicleId;
	}

	public void setVehicleId(String vehicleId) {
		this.vehicleId = vehicleId;
	}

	public Long getServiceDate() {
		return serviceDate;
	}

	public void setServiceDate(Long serviceDate) {
		this.serviceDate = serviceDate;
	}

	public Integer getScheduleDeviation() {
		return scheduleDeviation;
	}

	public void setScheduleDeviation(Integer scheduleDeviation) {
		this.scheduleDeviation = scheduleDeviation;
	}

	public String getBlockId() {
		return blockId;
	}

	public void setBlockId(String blockId) {
		this.blockId = blockId;
	}

	public String getTripId() {
		return tripId;
	}

	public void setTripId(String tripId) {
		this.tripId = tripId;
	}

	public Double getDistanceAlongBlock() {
		return distanceAlongBlock;
	}

	public void setDistanceAlongBlock(Double distanceAlongBlock) {
		this.distanceAlongBlock = distanceAlongBlock;
	}

	public Double getDistanceAlongTrip() {
		return distanceAlongTrip;
	}

	public void setDistanceAlongTrip(Double distanceAlongTrip) {
		this.distanceAlongTrip = distanceAlongTrip;
	}

	public Double getInferredLatitude() {
		return inferredLatitude;
	}

	public void setInferredLatitude(Double inferredLatitude) {
		this.inferredLatitude = inferredLatitude;
	}

	public Double getInferredLongitude() {
		return inferredLongitude;
	}

	public void setInferredLongitude(Double inferredLongitude) {
		this.inferredLongitude = inferredLongitude;
	}

	public Double getObservedLatitude() {
		return observedLatitude;
	}

	public void setObservedLatitude(Double observedLatitude) {
		this.observedLatitude = observedLatitude;
	}

	public Double getObservedLongitude() {
		return observedLongitude;
	}

	public void setObservedLongitude(Double observedLongitude) {
		this.observedLongitude = observedLongitude;
	}

	public String getPhase() {
		return phase;
	}

	public void setPhase(String phase) {
		this.phase = phase;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public NycVehicleManagementStatusBean getManagementRecord() {
		return managementRecord;
	}

	public void setManagementRecord(NycVehicleManagementStatusBean managementRecord) {
		this.managementRecord = managementRecord;
	}

	public void setRunId(String runId) {
		this.runId = runId;
	}

	public String getRunId() {
		return runId;
	}

	public void setRouteId(String routeId) {
		this.routeId = routeId;
	}

	public String getRouteId() {
		return routeId;
	}

	public void setBearing(double bearing) {
		this.bearing = bearing;
	}

	public double getBearing() {
		return bearing;
	}
	
	// Properties from TDS
	
	public void setAssignedRunId(String assignedRunId) {
		this.assignedRunId = assignedRunId;
	}
	
	public String getAssignedRunId() {
		return assignedRunId;
	}
	
	public void setNextScheduledStopId(String nextScheduledStopId) {
		this.nextScheduledStopId = nextScheduledStopId;
	}
	
	public String getNextScheduledStopId() {
		return nextScheduledStopId;
	}
	
	public void setNextScheduledStopDistance(Double distance) {
		this.nextScheduledStopDistance = distance;
	}
	
	public Double getNextScheduledStopDistance() {
		return nextScheduledStopDistance;
	}

	public String getInferredBlockId() {
		return inferredBlockId;
	}

	public void setInferredBlockId(String inferredBlockId) {
		this.inferredBlockId = inferredBlockId;
	}

	public String getInferredTripId() {
		return inferredTripId;
	}

	public void setInferredTripId(String inferredTripId) {
		this.inferredTripId = inferredTripId;
	}

	public String getInferredRouteId() {
		return inferredRouteId;
	}

	public void setInferredRouteId(String inferredRouteId) {
		this.inferredRouteId = inferredRouteId;
	}

	public String getInferredDirectionId() {
		return inferredDirectionId;
	}

	public void setInferredDirectionId(String inferredDirectionId) {
		this.inferredDirectionId = inferredDirectionId;
	}
	
	public Long getLastLocationUpdateTime() {
		return lastLocationUpdateTime;
	}

	public void setLastLocationUpdateTime(Long lastLocationUpdateTime) {
		this.lastLocationUpdateTime = lastLocationUpdateTime;
	}

	public void setRouteBean(RouteBean rb) {
		setInferredRouteId(rb.getId());
	}

	public void setVehicleStatusBean(VehicleStatusBean vehicle) {
		if (vehicle == null)
			return;
	    TripStatusBean tsb = vehicle.getTripStatus();
	    if (tsb != null) {setTripStatusBean(tsb);
	    }
	}

	public void setTripStatusBean(TripStatusBean tsb) {
		setNextScheduledStopDistance(tsb.getNextStopDistanceFromVehicle());
	    if (tsb.getNextStop() != null) {
	      setNextScheduledStopId(tsb.getNextStop().getId());
	    }
	    if (!Double.isNaN(tsb.getLastKnownDistanceAlongTrip())) {
	      setDistanceAlongTrip(tsb.getLastKnownDistanceAlongTrip());
	    }
	    // todo: can this be pulled from TDS?
	    // setDistanceAlongBlock()
	    TripBean trip = tsb.getActiveTrip();
	    if (trip != null && trip.getRoute() != null) {
	      setInferredBlockId(trip.getBlockId());
	      setInferredTripId(trip.getId());
	      setRouteBean(trip.getRoute());
	      setInferredDirectionId(trip.getDirectionId());
	    }
	}
	
	public void setVehicleLocationRecordBean(VehicleLocationRecordBean vlr) {
	    if (vlr != null && (getInferredLatitude() == null || getInferredLongitude() == null)) {
	      setInferredLatitude(vlr.getCurrentLocation().getLat()); //Previously BigDecimal
	      setInferredLongitude(vlr.getCurrentLocation().getLon()); //Previously BigDecimal
	      setLastLocationUpdateTime(vlr.getTimeOfLocationUpdate());
	      setScheduleDeviation((int) new Double(vlr.getScheduleDeviation()).longValue());
	    }
	  }

}

