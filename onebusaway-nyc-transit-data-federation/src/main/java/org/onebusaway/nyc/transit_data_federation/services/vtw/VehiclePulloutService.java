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

package org.onebusaway.nyc.transit_data_federation.services.vtw;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.onebusaway.gtfs.model.AgencyAndId;

import tcip_final_4_0_0.ObaSchPullOutList;
import tcip_final_4_0_0.SCHPullInOutInfo;

/**
 * Service interface for getting pullout info for a vehicle
 * 
 * @author dhaskin
 */
public interface VehiclePulloutService {

  /**
   * Get most recent pullout for given vehicle.
   * 
   * @param vehicleId The vehicle identifier to request pullout for.
   */
  public SCHPullInOutInfo getVehiclePullout(AgencyAndId vehicle);

  public String getAssignedBlockId(AgencyAndId vehicleId);
  
  public ObaSchPullOutList getFromXml(String xml) throws XMLStreamException, JAXBException;
  
  public String getAsXml(ObaSchPullOutList o) throws JAXBException;
}
