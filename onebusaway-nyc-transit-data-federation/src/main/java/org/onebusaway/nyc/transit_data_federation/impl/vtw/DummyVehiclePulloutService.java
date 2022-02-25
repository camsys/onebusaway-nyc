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

package org.onebusaway.nyc.transit_data_federation.impl.vtw;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.services.vtw.VehiclePulloutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tcip_final_4_0_0.ObaSchPullOutList;
import tcip_final_4_0_0.SCHBlockIden;
import tcip_final_4_0_0.SCHPullInOutInfo;

public class DummyVehiclePulloutService implements
    VehiclePulloutService {

  private static Logger _log = LoggerFactory.getLogger(DummyVehiclePulloutService.class);

  private Map<AgencyAndId, SCHPullInOutInfo> _vehicleIdToPullouts = new HashMap<AgencyAndId, SCHPullInOutInfo>();

  @Override
  public SCHPullInOutInfo getVehiclePullout(AgencyAndId vehicle) {
    return _vehicleIdToPullouts.get(vehicle);
  }

  @Override
  public String getAssignedBlockId(AgencyAndId vehicle) {
    SCHPullInOutInfo info = getVehiclePullout(vehicle);
    String agency = vehicle.getAgencyId();
    String id = null;
    if (info != null && info.getBlock() != null && info.getBlock().getId() != null && agency != null) {
      id = agency + AgencyAndId.ID_SEPARATOR + info.getBlock().getId();
    }
    _log.info("getAssignedBlockId for " + vehicle + " returning " + id);
    return id;
  }

  public void setVehiclePullout(AgencyAndId vehicleId, String assignedBlockId) {
    SCHPullInOutInfo p = new SCHPullInOutInfo();
    SCHBlockIden blockIden = new SCHBlockIden();
    p.setBlock(blockIden);
    p.getBlock().setId(assignedBlockId);
    _vehicleIdToPullouts.put(vehicleId, p);
  }

}
