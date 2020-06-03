/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onebusaway.nyc.transit_data_manager.adapters.output.model.json;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * A model class representing a single operator - run assignment. For use in
 * creating JSON with Gson.
 * 
 * @author sclark
 * 
 */
public class OperatorAssignment {
  private static final DateTimeFormatter DATE_PATTERN = ISODateTimeFormat.dateTimeNoMillis();

  public OperatorAssignment() {

  }

  private String agencyId;
  private String passId;
  private String runRoute;
  private String runNumber;
  private String runId;
  private String depot;
  private String serviceDate;
  private String updated;

  public void setAgencyId(String agencyId) {
    this.agencyId = agencyId;
  }
  
  public String getAgencyId() {
    return agencyId;
  }

  public void setPassId(String passId) {
    this.passId = passId;
  }

  public String getPassId() {
    return passId;
  }
  
  public void setRunRoute(String runRoute) {
    this.runRoute = runRoute;
  }

  public String getRunRoute() {
    return runRoute;
  }

  public void setRunNumber(String runNumber) {
    this.runNumber = runNumber;
  }

  public String getRunNumber() {
    return runNumber;
  }
  
  public void setDepot(String depot) {
    this.depot = depot;
  }

  public String getDepot() {
    return depot;
  }
  
  public void setServiceDate(String serviceDate) {
    this.serviceDate = serviceDate;
  }

  public String getServiceDate() {
      return serviceDate;
  }
  
  public void setUpdated(String updated) {
    this.updated = updated;
  }
  
  public String getUpdated() {
    return updated;
  }
  
  public String getRunId() {
	  return runId;
  }

  public void setRunId(String runId) {
	  this.runId = runId;
  }
  
  public int compareTo(OperatorAssignment oa) {
    DateTime incomingRecord = DATE_PATTERN.parseDateTime(oa.getUpdated());
    DateTime currentRecord = DATE_PATTERN.parseDateTime(getUpdated());
    return currentRecord.compareTo(incomingRecord);
  }

}
