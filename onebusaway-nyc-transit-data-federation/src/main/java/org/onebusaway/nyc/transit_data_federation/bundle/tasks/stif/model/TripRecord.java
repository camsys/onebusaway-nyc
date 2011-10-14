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
package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class TripRecord implements StifRecord {
  private String signCode;
  private String blockNumber;
  private int originTime;
  private int tripType;
  private int destinationTime;
  private String originLocation;
  private String signCodeRoute;
  private String reliefRun;
  private String runNumber;
  private String previousRunNumber;
  private int reliefTime;
  private String reliefRunRoute;
  private String runRoute;

  public void setSignCode(String signCode) {
    this.signCode = signCode;
  }

  public String getSignCode() {
    return signCode;
  }

  public void setBlockNumber(String blockNumber) {
    this.blockNumber = blockNumber;
  }

  public String getBlockNumber() {
    return blockNumber;
  }

  public void setOriginTime(int seconds) {
    this.originTime = seconds;
  }

  public int getOriginTime() {
    return originTime;
  }

  public void setTripType(int tripType) {
    this.tripType = tripType;
  }

  public int getTripType() {
    return tripType;
  }

  public void setDestinationTime(int destinationTime) {
    this.destinationTime = destinationTime;
  }

  public int getDestinationTime() {
    return destinationTime;
  }

  public void setOriginLocation(String originLocation) {
    this.originLocation = originLocation;
  }

  public String getOriginLocation() {
    return originLocation;
  }

  public String getSignCodeRoute() {
    return signCodeRoute;
  }

  public void setSignCodeRoute(String signCodeRoute) {
    this.signCodeRoute = signCodeRoute;
  }

  public String getReliefRunNumber() {
    if (reliefRun == null) {
      return runNumber;
    }
    return reliefRun;
  }

  public void setReliefRunNumber(String run) {
    this.reliefRun = run;
  }

  public String getRunNumber() {
    return runNumber;
  }

  public void setRunNumber(String run) {
    this.runNumber = run;
  }

  public String getPreviousRunNumber() {
    if (previousRunNumber == null) {
      return runNumber;
    }
    return previousRunNumber;
  }

  public void setPreviousRun(String previousRun) {
    this.previousRunNumber = previousRun;
  }

  public void setReliefTime(int reliefTime) {
    this.reliefTime = reliefTime;
  }
  
  public int getReliefTime() {
    return reliefTime;
  }

  public String getRunRoute() {
    return runRoute;
  }

  public String getReliefRunRoute() {
    return reliefRunRoute;
  }

  public String getRunId() {
    return RunTripEntry.createId(getRunRoute(), getRunNumber());
  }

  public String getReliefRunId() {
    return RunTripEntry.createId(getReliefRunRoute(), getReliefRunNumber());
  }

  public void setReliefRunRoute(String reliefRunRoute) {
    this.reliefRunRoute = reliefRunRoute;
  }

  public void setRunRoute(String runRoute) {
    this.runRoute = runRoute;
  }
}
