package org.onebusaway.nyc.vehicle_tracking.services;

import org.onebusaway.nyc.vehicle_tracking.model.NycTestLocationRecord;

public class VehicleLocationSimulationSummary {

  private int id;

  private int numberOfRecordsProcessed;

  private int numberOfRecordsTotal;

  private NycTestLocationRecord mostRecentRecord;

  private boolean paused = false;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getNumberOfRecordsProcessed() {
    return numberOfRecordsProcessed;
  }

  public void setNumberOfRecordsProcessed(int numberOfRecordsProcessed) {
    this.numberOfRecordsProcessed = numberOfRecordsProcessed;
  }

  public int getNumberOfRecordsTotal() {
    return numberOfRecordsTotal;
  }

  public void setNumberOfRecordsTotal(int numberOfRecordsTotal) {
    this.numberOfRecordsTotal = numberOfRecordsTotal;
  }

  public NycTestLocationRecord getMostRecentRecord() {
    return mostRecentRecord;
  }

  public void setMostRecentRecord(NycTestLocationRecord mostRecentRecord) {
    this.mostRecentRecord = mostRecentRecord;
  }

  public boolean isPaused() {
    return paused;
  }

  public void setPaused(boolean paused) {
    this.paused = paused;
  }
}
