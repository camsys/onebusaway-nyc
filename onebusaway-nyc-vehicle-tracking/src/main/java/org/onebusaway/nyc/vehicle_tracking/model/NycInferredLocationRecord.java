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

import java.io.InputStream;
import java.io.Serializable;
import java.util.Date;

import org.onebusaway.gtfs.csv.schema.annotations.CsvField;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.serialization.mappings.StopTimeFieldMappingFactory;
import org.onebusaway.nyc.vehicle_tracking.model.csv.AgencyIdFieldMappingFactory;
import org.onebusaway.nyc.vehicle_tracking.model.csv.DateTimeFieldMappingFactory;

public class NycInferredLocationRecord implements Serializable {

  private static final long serialVersionUID = 1L;

  private static final int MISSING_VALUE = -999;

  /****
   * Raw Observation Values
   ****/

  @CsvField(name = "vehicle", mapping = AgencyIdFieldMappingFactory.class)
  private AgencyAndId vehicleId;

  private double lat;

  private double lon;

  @CsvField(name = "timestamp", mapping = DateTimeFieldMappingFactory.class)
  private long timestamp;

  @CsvField(optional = true)
  private String dsc;

  /****
   * Inferred Values
   ****/
  @CsvField(optional = true)
  private String inferredBlockId;

  @CsvField(optional = true)
  private String inferredTripId;

  @CsvField(optional = true)
  private long inferredServiceDate;

  @CsvField(optional = true)
  private double inferredDistanceAlongBlock = Double.NaN;

  @CsvField(optional = true, mapping = StopTimeFieldMappingFactory.class)
  private int inferredScheduleTime = MISSING_VALUE;

  @CsvField(optional = true)
  private String inferredDsc;

  @CsvField(optional = true)
  private double inferredLat = Double.NaN;

  @CsvField(optional = true)
  private double inferredLon = Double.NaN;

  @CsvField(optional = true)
  private double inferredBlockLat = Double.NaN;

  @CsvField(optional = true)
  private double inferredBlockLon = Double.NaN;

  @CsvField(optional = true)
  private String inferredPhase = null;

  @CsvField(optional = true)
  private String inferredStatus = null;

  /****
   * Ground Truth Information
   ****/
  @CsvField(optional = true)
  private String actualBlockId;

  @CsvField(optional = true)
  private String actualTripId;

  @CsvField(optional = true)
  private long actualServiceDate;

  @CsvField(optional = true)
  private double actualDistanceAlongBlock = Double.NaN;

  @CsvField(optional = true, mapping = StopTimeFieldMappingFactory.class)
  private int actualScheduleTime = MISSING_VALUE;

  @CsvField(optional = true)
  private String actualDsc;

  @CsvField(optional = true)
  private double actualLat = Double.NaN;

  @CsvField(optional = true)
  private double actualLon = Double.NaN;

  @CsvField(optional = true)
  private double actualBlockLat = Double.NaN;

  @CsvField(optional = true)
  private double actualBlockLon = Double.NaN;

  @CsvField(optional = true)
  private String actualPhase = null;

  @CsvField(optional = true)
  private String actualStatus = null;

  public void setVehicleId(AgencyAndId vehicleId) {
    this.vehicleId = vehicleId;
  }

  public AgencyAndId getVehicleId() {
    return vehicleId;
  }

  public void setLat(double lat) {
    this.lat = lat;
  }

  public double getLat() {
    return lat;
  }

  public void setLon(double lon) {
    this.lon = lon;
  }

  public double getLon() {
    return lon;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public Date getTimestampAsDate() {
    return new Date(timestamp);
  }

  public void setDsc(String dsc) {
    this.dsc = dsc;
  }

  public String getDsc() {
    return dsc;
  }

  /****
   * Inferred Values
   ****/
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

  public boolean isInferredServiceDateSet() {
    return inferredServiceDate > 0;
  }

  public long getInferredServiceDate() {
    return inferredServiceDate;
  }

  public void setInferredServiceDate(long inferredServiceDate) {
    this.inferredServiceDate = inferredServiceDate;
  }

  public boolean isInferredDistanceAlongBlockSet() {
    return !Double.isNaN(inferredDistanceAlongBlock);
  }

  public double getInferredDistanceAlongBlock() {
    return inferredDistanceAlongBlock;
  }

  public void setInferredDistanceAlongBlock(double inferredDistanceAlongBlock) {
    this.inferredDistanceAlongBlock = inferredDistanceAlongBlock;
  }

  public boolean isInferredScheduleTimeSet() {
    return inferredScheduleTime != MISSING_VALUE;
  }

  public int getInferredScheduleTime() {
    return inferredScheduleTime;
  }

  public void setInferredScheduleTime(int inferredScheduleTime) {
    this.inferredScheduleTime = inferredScheduleTime;
  }

  public String getInferredDsc() {
    return inferredDsc;
  }

  public void setInferredDsc(String inferredDsc) {
    this.inferredDsc = inferredDsc;
  }

  public boolean isInferredLatSet() {
    return !Double.isNaN(inferredLat);
  }

  public double getInferredLat() {
    return inferredLat;
  }

  public void setInferredLat(double inferredLat) {
    this.inferredLat = inferredLat;
  }

  public boolean isInferredLonSet() {
    return !Double.isNaN(inferredLon);
  }

  public double getInferredLon() {
    return inferredLon;
  }

  public void setInferredLon(double inferredLon) {
    this.inferredLon = inferredLon;
  }

  public boolean isInferredBlockLatSet() {
    return !Double.isNaN(inferredBlockLat);
  }

  public double getInferredBlockLat() {
    return inferredBlockLat;
  }

  public void setInferredBlockLat(double inferredBlockLat) {
    this.inferredBlockLat = inferredBlockLat;
  }

  public boolean isInferredBlockLonSet() {
    return !Double.isNaN(inferredBlockLon);
  }

  public double getInferredBlockLon() {
    return inferredBlockLon;
  }

  public void setInferredBlockLon(double inferredBlockLon) {
    this.inferredBlockLon = inferredBlockLon;
  }

  public String getInferredPhase() {
    return inferredPhase;
  }

  public void setInferredPhase(String inferredPhase) {
    this.inferredPhase = inferredPhase;
  }

  public String getInferredStatus() {
    return inferredStatus;
  }

  public void setInferredStatus(String inferredStatus) {
    this.inferredStatus = inferredStatus;
  }

  public void clearInferredValues() {
    inferredBlockId = null;
    inferredBlockLat = Double.NaN;
    inferredBlockLon = Double.NaN;
    inferredDistanceAlongBlock = Double.NaN;
    inferredDsc = null;
    inferredLat = Double.NaN;
    inferredLon = Double.NaN;    
    inferredPhase = null;
    inferredScheduleTime = MISSING_VALUE;
    inferredServiceDate = 0;
    inferredStatus = null;
    inferredTripId = null;
    
  }

  /****
   * Ground Truth Data
   ****/
  public String getActualBlockId() {
    return actualBlockId;
  }

  public void setActualBlockId(String actualBlockId) {
    this.actualBlockId = actualBlockId;
  }

  public String getActualTripId() {
    return actualTripId;
  }

  public void setActualTripId(String actualTripId) {
    this.actualTripId = actualTripId;
  }

  public boolean isActualServiceDateSet() {
    return actualServiceDate > 0;
  }

  public long getActualServiceDate() {
    return actualServiceDate;
  }

  public void setActualServiceDate(long actualServiceDate) {
    this.actualServiceDate = actualServiceDate;
  }

  public boolean isActualDistanceAlongBlockSet() {
    return !Double.isNaN(actualDistanceAlongBlock);
  }

  public double getActualDistanceAlongBlock() {
    return actualDistanceAlongBlock;
  }

  public void setActualDistanceAlongBlock(double actualDistanceAlongBlock) {
    this.actualDistanceAlongBlock = actualDistanceAlongBlock;
  }

  public boolean isActualScheduleTimeSet() {
    return actualScheduleTime != MISSING_VALUE;
  }

  public int getActualScheduleTime() {
    return actualScheduleTime;
  }

  public void setActualScheduleTime(int actualScheduleTime) {
    this.actualScheduleTime = actualScheduleTime;
  }

  public String getActualDsc() {
    return actualDsc;
  }

  public void setActualDsc(String actualDsc) {
    this.actualDsc = actualDsc;
  }

  public boolean isActualLatSet() {
    return !Double.isNaN(actualLat);
  }

  public double getActualLat() {
    return actualLat;
  }

  public void setActualLat(double actualLat) {
    this.actualLat = actualLat;
  }

  public boolean isActualLonSet() {
    return !Double.isNaN(actualLon);
  }

  public double getActualLon() {
    return actualLon;
  }

  public void setActualLon(double actualLon) {
    this.actualLon = actualLon;
  }

  public boolean isActualBlockLatSet() {
    return !Double.isNaN(actualBlockLat);
  }

  public double getActualBlockLat() {
    return actualBlockLat;
  }

  public void setActualBlockLat(double actualBlockLat) {
    this.actualBlockLat = actualBlockLat;
  }

  public boolean isActualBlockLonSet() {
    return !Double.isNaN(actualBlockLon);
  }

  public double getActualBlockLon() {
    return actualBlockLon;
  }

  public void setActualBlockLon(double actualBlockLon) {
    this.actualBlockLon = actualBlockLon;
  }

  public String getActualPhase() {
    return actualPhase;
  }

  public void setActualPhase(String actualPhase) {
    this.actualPhase = actualPhase;
  }

  public String getActualStatus() {
    return actualStatus;
  }

  public void setActualStatus(String actualStatus) {
    this.actualStatus = actualStatus;
  }

  public static InputStream getTestData() {
    return NycInferredLocationRecord.class.getResourceAsStream("ivn-dsc.csv");
  }

  /**
   * Location data is considered missing if the values are NaN or if both are
   * zero
   */
  public boolean locationDataIsMissing() {
    return (Double.isNaN(this.lat) || Double.isNaN(this.lon))
        || (this.lat == 0.0 && this.lon == 0.0);
  }
}
