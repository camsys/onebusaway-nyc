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

package org.onebusaway.nyc.transit_data_manager.adapters.input.model;

public class MtaBusDepotAssignment {

  public MtaBusDepotAssignment() {

  }

  private String depot;
  private Long agencyId;
  private int busNumber;
  private boolean kneeling;

  public String getDepot() {
    return depot;
  }

  public Long getAgencyId() {
    return agencyId;
  }

  public int getBusNumber() {
    return busNumber;
  }

  public void setDepot(String depot) {
    this.depot = depot;
  }

  public void setAgencyId(Long agencyId) {
    this.agencyId = agencyId;
  }

  public void setBusNumber(int busNumber) {
    this.busNumber = busNumber;
  }

  public void setKneeling(boolean kneeling) {
    this.kneeling = kneeling;
  }

  public boolean isKneeling() {
    return kneeling;
  }
}
