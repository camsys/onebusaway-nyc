package org.onebusaway.nyc.vehicle_tracking.model.unassigned;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

import java.util.Objects;

/**
 * Represents SPOOKY / zombie bus data passed to us from an external system -- Depot pull-out vehicles.
 * Pass along to TDS confirming that bus is in service and hence schedule data should be shown.
 */
public class UnassignedVehicleRecord {
    private String agencyId;
    private String blockId;
    private Double distanceAlongBlock;
    private Double latitude;
    private Double longitude;
    private String phase;
    private Long serviceDate;
    private String status;
    private Long timeReceived;
    private String tripId;
    private String vehicleId;
    // external system doesn't know this, but IE may remember this from last update
    private Integer scheduleDeviation;

    @JsonProperty("agency-id")
    public String getAgencyId() {
        return agencyId;
    }

    public void setAgencyId(String agencyId) {
        this.agencyId = agencyId;
    }

    @JsonProperty("inferred-block-id")
    public String getBlockId() {
        return blockId;
    }

    public void setBlockId(String blockId) {
        this.blockId = blockId;
    }

    @JsonProperty("distance-along-block")
    public Double getDistanceAlongBlock() {
        return distanceAlongBlock;
    }

    public void setDistanceAlongBlock(Double distanceAlongBlock) {
        this.distanceAlongBlock = distanceAlongBlock;
    }

    @JsonProperty("inferred-latitude")
    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    @JsonProperty("inferred-longitude")
    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    @JsonProperty("inferred-phase")
    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    @JsonProperty("service-date")
    @JsonDeserialize(using = CustomJsonDateDeserializer.class)
    public Long getServiceDate() {
        return serviceDate;
    }

    public void setServiceDate(Long serviceDate) {
        this.serviceDate = serviceDate;
    }

    @JsonProperty("schedule-deviation")
    public Integer getScheduleDeviation() { return scheduleDeviation; }

    public void setScheduleDeviation(Integer scheduleDeviation) {
        this.scheduleDeviation = scheduleDeviation;
    }

    @JsonProperty("inferred-status")
    public String getStatus(){
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @JsonProperty("time-received")
    @JsonDeserialize(using = CustomJsonDateTimeDeserializer.class)
    public Long getTimeReceived() {
        return timeReceived;
    }

    public void setTimeReceived(Long timeReceived) {
        this.timeReceived = timeReceived;
    }

    @JsonProperty("inferred-trip-id")
    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    @JsonProperty("vehicle-id")
    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnassignedVehicleRecord that = (UnassignedVehicleRecord) o;
        return Objects.equals(agencyId, that.agencyId) &&
                Objects.equals(blockId, that.blockId) &&
                Objects.equals(distanceAlongBlock, that.distanceAlongBlock) &&
                Objects.equals(latitude, that.latitude) &&
                Objects.equals(longitude, that.longitude) &&
                Objects.equals(phase, that.phase) &&
                Objects.equals(serviceDate, that.serviceDate) &&
                Objects.equals(status, that.status) &&
                Objects.equals(timeReceived, that.timeReceived) &&
                Objects.equals(tripId, that.tripId) &&
                Objects.equals(vehicleId, that.vehicleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agencyId, blockId, distanceAlongBlock, latitude, longitude, phase, serviceDate, status, timeReceived, tripId, vehicleId);
    }
}
