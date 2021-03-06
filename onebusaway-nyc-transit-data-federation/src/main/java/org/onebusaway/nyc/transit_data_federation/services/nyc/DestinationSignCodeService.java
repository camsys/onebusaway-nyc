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
package org.onebusaway.nyc.transit_data_federation.services.nyc;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.onebusaway.gtfs.model.AgencyAndId;

/**
 * Map of DSC to trips (individual and collections of) and vice-versa.
 * @author jmaki
 *
 */
public interface DestinationSignCodeService {

  public List<AgencyAndId> getTripIdsForDestinationSignCode(
      String destinationSignCode, String agencyId);
  
  public String getDestinationSignCodeForTripId(AgencyAndId tripId);

  public boolean isOutOfServiceDestinationSignCode(String destinationSignCode);
  
  public boolean isMissingDestinationSignCode(String destinationSignCode);
  
  public boolean isUnknownDestinationSignCode(String destinationSignCode, String agencyId);

  public Set<AgencyAndId> getRouteCollectionIdsForDestinationSignCode(
      String destinationSignCode, String agencyId);

}
