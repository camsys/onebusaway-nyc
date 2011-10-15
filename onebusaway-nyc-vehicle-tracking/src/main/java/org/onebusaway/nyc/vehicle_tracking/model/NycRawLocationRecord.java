/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.vehicle_tracking.model;

import java.util.Date;

import org.onebusaway.csv_entities.schema.annotations.CsvField;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.serialization.mappings.AgencyIdFieldMappingFactory;

public class NycRawLocationRecord {

  private long id;

  /**
   * The time on the bus when this record was sent to us.
   */
  private long time;

  /**
   * The time on the server when we received this record.
   */
  @CsvField(name = "timereceived")
  private long timeReceived;

  private double latitude;

  private double longitude;

  private double bearing;

  @CsvField(name = "destinationsigncode")
  private String destinationSignCode;

  @CsvField(name = "vehicle", mapping = AgencyIdFieldMappingFactory.class)
  private AgencyAndId vehicleId;

  @CsvField(name = "operator")
  private String operatorId;

  @CsvField(name = "runnumber")
  private String runNumber;
  
  @CsvField(name = "runroute")
  private String runRouteId;

  @CsvField(name = "emergency", optional = true)
  private boolean emergencyFlag;
  
  @CsvField(name = "deviceid")
  private String deviceId;

  /* raw GPS sentences */
  @CsvField(optional = true)
  private String gga;

  @CsvField(optional = true)
  private String rmc;

  /* raw data as received from bus hardware */
  @CsvField(name = "rawdata", optional = true)
  private String rawData;

  @CsvField(name = "speed")
  private short speed;

  public void setId(long id) {
    this.id = id;
  }

  public long getId() {
    return id;
  }

  public void setTime(long time) {
    this.time = time;
  }

  public long getTime() {
    return time;
  }
  
  public Date getTimeAsDate() {
    return new Date(time);
  }

  public void setLatitude(double latitude) {
    this.latitude = latitude;
  }

  public double getLatitude() {
    return latitude;
  }

  public void setLongitude(double longitude) {
    this.longitude = longitude;
  }

  public double getLongitude() {
    return longitude;
  }

  public void setBearing(double bearing) {
    this.bearing = bearing;
  }

  public double getBearing() {
    return bearing;
  }

  public String getDestinationSignCode() {
    return destinationSignCode;
  }

  public void setDestinationSignCode(String destinationSignCode) {
    this.destinationSignCode = destinationSignCode;
  }

  public AgencyAndId getVehicleId() {
    return vehicleId;
  }

  public void setVehicleId(AgencyAndId vehicleId) {
	  this.vehicleId = vehicleId;
  }

  public String getOperatorId() {
	  return operatorId;
  }

  public void setOperatorId(String operatorId) {
	  this.operatorId = operatorId;
  }

  public String getRunNumber() {
	  return runNumber;
  }

  public String getRunRouteId() {
	  return runRouteId;
  }

  public void setRunRouteId(String runRouteId) {
	  this.runRouteId = runRouteId;
  }

  public boolean isEmergencyFlag() {
	  return emergencyFlag;
  }

  public void setEmergencyFlag(boolean emergencyFlag) {
	  this.emergencyFlag = emergencyFlag;
  }

  public void setGga(String gga) {
	  this.gga = gga;
  }

  public String getGga() {
    return gga;
  }

  public void setRmc(String rmc) {
    this.rmc = rmc;
  }

  public String getRmc() {
    return rmc;
  }

  public void setTimeReceived(long timeReceived) {
    this.timeReceived = timeReceived;
  }

  public long getTimeReceived() {
    return timeReceived;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  public String getDeviceId() {
    return deviceId;
  }

  public void setRawData(String rawData) {
    this.rawData = rawData;
  }

  public String getRawData() {
    return rawData;
  }
  
  /**
   * Location data is considered missing if the values are NaN or if both are
   * zero
   */
  public boolean locationDataIsMissing() {
    return (Double.isNaN(this.latitude) || Double.isNaN(this.longitude))
        || (this.latitude == 0.0 && this.longitude == 0.0);
  }
  
  @Override
  public String toString() {
    return latitude + " " + longitude + " " + destinationSignCode + " " + timeReceived;
  }

  public void setSpeed(short speed) {
    this.speed = speed;
  }

  public short getSpeed() {
    return speed;
  }

  public String getRunId() {
    if(runRouteId == null || runNumber == null)
      return null;
    else
      return runRouteId + "_" + runNumber;
  }

  public void setRunNumber(String runNumber) {
    this.runNumber = runNumber;
  }
}
