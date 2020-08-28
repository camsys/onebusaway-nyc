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

package org.onebusaway.nyc.transit_data_manager.adapters.tools;

public class SpearDepotsMappingTool extends TcipMappingTool {

  public Long getAgencyIdFromAgency(int value) {
    Long agencyId;
    if (1 == value) {
      agencyId = MTA_NYCT_AGENCY_ID;
    } else if (2 == value) {
      agencyId = MTA_BUS_CO_AGENCY_ID;
    } else if (3 == value) {
      agencyId = MTA_LI_BUS_AGENCY_ID;
    } else {
      agencyId = new Long(-1);
    }
    
    return agencyId;
  }
}
