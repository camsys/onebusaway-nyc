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

package org.onebusaway.nyc.webapp.actions.api.model;

import org.apache.commons.codec.digest.DigestUtils;
import org.onebusaway.transit_data.model.StopGroupBean;

import java.util.Comparator;
import java.util.List;

/**
 * Route destination
 * @author jmaki
 *
 */
public class RouteDirectionV2 {

  private String directionId;

  private String destination;

  private List<PolylineWithStatus> polylines;

  private List<StopOnRoute> stops;

  private Boolean hasUpcomingScheduledService;

  public RouteDirectionV2(StopGroupBean stopGroup, List<PolylineWithStatus> polylines,
                          List<StopOnRoute> stops, Boolean hasUpcomingScheduledService) {
    this.directionId = stopGroup.getId();
    this.destination = stopGroup.getName().getName();
    this.polylines = polylines;
    this.stops = stops;
    this.hasUpcomingScheduledService = hasUpcomingScheduledService;
  }

  public String getDirectionId() {
    return directionId;
  }

  public String getDestination() {
    return destination;
  }

  public List<PolylineWithStatus> getPolylines() {
    return polylines;
  }

  public List<StopOnRoute> getStops() {
    return stops;
  }
  
  public Boolean getHasUpcomingScheduledService() {
    return hasUpcomingScheduledService;
  }

  public String getDirPolyAndStopsHash() {
    StringBuilder sb = new StringBuilder();

    // 2. Append the unique direction identifier
    sb.append("dir:").append(this.getDirectionId());

    // 3. Append the polyline
    sb.append("|poly:").append(this.getPolylines().stream().map(PolylineWithStatus::getLine).reduce("", String::concat));

    // 4. Append all stop IDs (assuming Stop objects have a getId() or similar)
    sb.append("|stops:");
    for (StopOnRoute stop : this.getStops()) {
      sb.append(stop.getId()).append(",");
    }


    // 5. Hash the resulting string to create a fixed-length ETag
    // Using a standard hash like MD5 or SHA-256 is recommended for headers
    return DigestUtils.md5Hex(sb.toString());
  }

}
