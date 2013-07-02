package org.onebusaway.nyc.transit_data_federation.impl.queue;

import java.sql.Timestamp;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.realtime.api.VehicleLocationListener;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class FileInputTask {

  protected static Logger _log = LoggerFactory.getLogger(FileInputTask.class);
  
  @Autowired
  private VehicleLocationListener _vehicleLocationListener;
  
  private String filename;
  public void setFilename(String filename) {
    this.filename = filename;
  }
  
  public void execute() {
    try {
      open();
      while (next()) {
        Record record = parseCurrentRecord();
        processResult(record);
      }
    } catch (Exception e) {
      _log.error("file issue:" + e);
    } finally {
    close();
    }
  }
  
  private boolean next() {
    return false;
  }
  
  private Record parseCurrentRecord() {
    return null;
  }
  
  private void processResult(Record record) {
    VehicleLocationRecord vlr = new VehicleLocationRecord();
    vlr.setVehicleId(record.getVehicleId());
    vlr.setTimeOfRecord(record.getTimeOfRecord());
    vlr.setTimeOfLocationUpdate(record.getTimeOfLocationUpdate());
    vlr.setBlockId(record.getBlockId());
    vlr.setTripId(record.getTripId());
    vlr.setServiceDate(record.getServiceDate());
    vlr.setDistanceAlongBlock(record.getDistanceAlongBlock());
    vlr.setCurrentLocationLat(record.getLat());
    vlr.setCurrentLocationLon(record.getLon());
    vlr.setPhase(record.getPhase());
    vlr.setStatus(record.getStatus());
    _vehicleLocationListener.handleVehicleLocationRecord(vlr);
  }
  
  // perform file handling
  private void open() throws Exception {
    
  }
  
  // perform file handling cleanup
  private void close() {
    
  }
  
  
  
    private static class Record {

      private AgencyAndId vehicleId;
      private long timeOfRecord;
      private long timeOfLocationUpdate;
      private AgencyAndId blockId;
      private AgencyAndId tripId;
      private long serviceDate;
      private double distanceAlongBlock;
      private double lat;
      private double lon;
      private EVehiclePhase phase;
      private String status;

      public AgencyAndId getVehicleId() {
        return vehicleId;
      }
      public void setVehicleId(AgencyAndId vehicleId) {
        this.vehicleId = vehicleId;
      }
      public long getTimeOfRecord() {
        return timeOfRecord;
      }
      public void setTimeOfRecord(long timeOfRecord) {
        this.timeOfRecord = timeOfRecord;
      }
      public long getTimeOfLocationUpdate() {
        return timeOfLocationUpdate;
      }
      public void setTimeOfLocationUpdate(long timeOfLocationUpdate) {
        this.timeOfLocationUpdate = timeOfLocationUpdate;
      }
      public AgencyAndId getBlockId() {
        return blockId;
      }
      public void setBlockId(AgencyAndId blockId) {
        this.blockId = blockId;
      }
      public AgencyAndId getTripId() {
        return tripId;
      }
      public void setTripId(AgencyAndId tripId) {
        this.tripId = tripId;
      }
      public long getServiceDate() {
        return serviceDate;
      }
      public void setServiceDate(long serviceDate) {
        this.serviceDate = serviceDate;
      }
      public double getDistanceAlongBlock() {
        return distanceAlongBlock;
      }
      public void setDistanceAlongBlock(double distanceAlongBlock) {
        this.distanceAlongBlock = distanceAlongBlock;
      }
      public double getLat() {
        return lat;
      }
      public void setLat(double lat) {
        this.lat = lat;
      }
      public double getLon() {
        return lon;
      }
      public void setLon(double lon) {
        this.lon = lon;
      }
      public EVehiclePhase getPhase() {
        return phase;
      }
      public void setPhase(EVehiclePhase phase) {
        this.phase = phase;
      }
      public String getStatus() {
        return status;
      }
      public void setStatus(String status) {
        this.status = status;
      }
  }
}
