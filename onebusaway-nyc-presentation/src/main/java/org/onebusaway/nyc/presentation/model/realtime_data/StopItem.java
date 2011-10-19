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
package org.onebusaway.nyc.presentation.model.realtime_data;

import java.util.List;

import org.onebusaway.nyc.presentation.impl.WebappSupportLibrary;
import org.onebusaway.transit_data.model.StopBean;

/**
 * data transfer object wrapping a stop bean
 */
public class StopItem {

  private final String id;

  private final String name;

  private final Double latitude;
  
  private final Double longitude;
  
  private final List<DistanceAway> distanceAways;

  private static final WebappSupportLibrary idParser = new WebappSupportLibrary();

  public StopItem(StopBean stopBean, List<DistanceAway> distanceAways) {
    this(idParser.parseIdWithoutAgency(stopBean.getId()), stopBean.getName(), 
        stopBean.getLat(), stopBean.getLon(), distanceAways);
  }

  public StopItem(String id, String name, Double latitude, Double longitude, 
      List<DistanceAway> distanceAways) {
    this.id = id;
    this.name = name;
    this.latitude = latitude;
    this.longitude = longitude;
    this.distanceAways = distanceAways;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }
  
  public Double getLatitude() {
    return latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public List<DistanceAway> getDistanceAways() {
    return distanceAways;
  }

  public String getDistanceAwaysAsString() {
    if (distanceAways == null || distanceAways.size() == 0)
      return "";

    StringBuilder b = new StringBuilder();

    for (DistanceAway distanceAway : distanceAways) {    	
      if(b.length() > 0)
        b.append(", ");

      b.append(distanceAway.getPresentableDistance());
    }

    b.insert(0, " ");

    return b.toString();
  }
}
