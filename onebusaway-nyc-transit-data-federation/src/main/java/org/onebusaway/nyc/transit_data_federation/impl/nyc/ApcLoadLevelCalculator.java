/**
 * Copyright (C) 2024 Metropolitan Transportation Authority
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
package org.onebusaway.nyc.transit_data_federation.impl.nyc;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.model.NycVehicleLoadBean;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.realtime.api.OccupancyStatus;
import org.onebusaway.util.AgencyAndIdLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
/**
 * Translate estimate load / esitimated capacity to a load level per
 * configuration.
 */
public class ApcLoadLevelCalculator {

  // threshold between MANY_SEATS AVAILABLE and FEW_SEATS_AVAILABLE
  private static final float DEFAULT_X_FACTOR = 0.35f;
  // threshold for STANDING_ROOM_ONLY
  private static final float DEFAULT_Y_FACTOR = 0.75f;
  // threshold for FULL
  private static final float DEFAULT_Z_FACTOR = 1.0f;

  // threshold between MANY_SEATS AVAILABLE and FEW_SEATS_AVAILABLE
  private static final float DEFAULT_EXPRESS_X_FACTOR = 0.20f;
  // threshold for STANDING_ROOM_ONLY
  private static final float DEFAULT_EXPRESS_Y_FACTOR = 0.45f;
  // threshold for FULL
  private static final float DEFAULT_EXPRESS_Z_FACTOR = .75f;

  private boolean useLoadFactor = false;
  public void setUseLoadFactor(boolean useLoadFactor) {
    this.useLoadFactor = useLoadFactor;
  }
  private float xFactor = DEFAULT_X_FACTOR;
  public void setXFactor(float xFactor) {
    this.xFactor = xFactor;
  }
  private float yFactor = DEFAULT_Y_FACTOR;
  public void setYFactor(float yFactor) {
    this.yFactor = yFactor;
  }
  private float zFactor = DEFAULT_Z_FACTOR;
  public void setZFactor(float zFactor) {
    this.zFactor = zFactor;
  }

  private float xExpressFactor = DEFAULT_EXPRESS_X_FACTOR;
  public void setXExpressFactor(float xExpressFactor) {
    this.xExpressFactor = xExpressFactor;
  }
  private float yExpressFactor = DEFAULT_EXPRESS_Y_FACTOR;
  public void setYExpressFactor(float yExpressFactor) {
    this.yExpressFactor = yExpressFactor;
  }
  private float zExpressFactor = DEFAULT_EXPRESS_Z_FACTOR;
  public void setZExpressFactor(float zExpressFactor) {
    this.zExpressFactor = zExpressFactor;
  }

  @Autowired
  private ConfigurationService _config;

  @Autowired
  private NycRouteTypeService _nycRouteTypeService;

  @PostConstruct
    public void setup() {
        xFactor = Float.parseFloat(_config.getConfigurationValueAsString("tds.apcLoadLevelCalculator.xFactor", Float.toString(DEFAULT_X_FACTOR)));
        yFactor = Float.parseFloat(_config.getConfigurationValueAsString("tds.apcLoadLevelCalculator.yFactor", Float.toString(DEFAULT_Y_FACTOR)));
        zFactor = Float.parseFloat(_config.getConfigurationValueAsString("tds.apcLoadLevelCalculator.zFactor", Float.toString(DEFAULT_Z_FACTOR)));
        xExpressFactor = Float.parseFloat(_config.getConfigurationValueAsString("tds.apcLoadLevelCalculator.xExpressFactor", Float.toString(DEFAULT_EXPRESS_X_FACTOR)));
        yExpressFactor = Float.parseFloat(_config.getConfigurationValueAsString("tds.apcLoadLevelCalculator.yExpressFactor", Float.toString(DEFAULT_EXPRESS_Y_FACTOR)));
        zExpressFactor = Float.parseFloat(_config.getConfigurationValueAsString("tds.apcLoadLevelCalculator.zExpressFactor", Float.toString(DEFAULT_EXPRESS_Z_FACTOR)));
    }


  public OccupancyStatus toOccupancyStatus(NycVehicleLoadBean message) {
    if (useLoadFactor) return message.getLoad(); // the MTA will own this value
    if (message.getEstCapacity() > 0) {
      // we are responsible for the calculation
      if (message.getEstLoad() == 0)
        return OccupancyStatus.MANY_SEATS_AVAILABLE;
      double loadFactor = message.getEstLoad() / message.getEstCapacity();
      if(_nycRouteTypeService.isRouteExpress(AgencyAndIdLibrary.convertFromString(message.getRoute()))){
        if (loadFactor > zExpressFactor)
          return OccupancyStatus.FULL;
        if (loadFactor >= yExpressFactor)
          return OccupancyStatus.STANDING_ROOM_ONLY;
        if (loadFactor >= xExpressFactor)
          return OccupancyStatus.FEW_SEATS_AVAILABLE;
        // implicitly loadFactor < xFactor
      } else {
          if (loadFactor > zFactor)
            return OccupancyStatus.FULL;
          if (loadFactor >= yFactor)
            return OccupancyStatus.STANDING_ROOM_ONLY;
          if (loadFactor >= xFactor)
            return OccupancyStatus.FEW_SEATS_AVAILABLE;
          // implicitly loadFactor < xFactor
      }
      return OccupancyStatus.MANY_SEATS_AVAILABLE;
    }
    return null;
  }

  public OccupancyStatus toOccupancyStatus(ApcLoadData message, AgencyAndId routeId) {

    if (message.getEstLoadAsInt() != null) {
      // we are responsible for the calculation
      if (message.getEstCapacityAsInt() != null && message.getEstCapacityAsInt() > 0) {
        if (message.getEstLoadAsInt() == 0)
          return OccupancyStatus.MANY_SEATS_AVAILABLE;

        double loadFactor = Double.valueOf(message.getEstLoadAsInt()) / message.getEstCapacityAsInt();
        if(_nycRouteTypeService.isRouteExpress(routeId)){
          if (loadFactor > zExpressFactor)
            return OccupancyStatus.FULL;
          if (loadFactor >= yExpressFactor)
            return OccupancyStatus.STANDING_ROOM_ONLY;
          if (loadFactor >= xExpressFactor)
            return OccupancyStatus.FEW_SEATS_AVAILABLE;
          // implicitly loadFactor < xFactor
        } else {
          if (loadFactor > zFactor)
            return OccupancyStatus.FULL;
          if (loadFactor >= yFactor)
            return OccupancyStatus.STANDING_ROOM_ONLY;
          if (loadFactor >= xFactor)
            return OccupancyStatus.FEW_SEATS_AVAILABLE;
          // implicitly loadFactor < xFactor
        }
        return OccupancyStatus.MANY_SEATS_AVAILABLE;
      }
    }
    return null;
  }
}
