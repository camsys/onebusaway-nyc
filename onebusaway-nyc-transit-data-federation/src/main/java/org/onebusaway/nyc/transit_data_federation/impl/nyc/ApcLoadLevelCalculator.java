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
import org.onebusaway.realtime.api.VehicleOccupancyRecord;
import org.onebusaway.util.AgencyAndIdLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Component
/**
 * Translate estimate load / esitimated capacity to a load level per
 * configuration.
 */
public class ApcLoadLevelCalculator {
  protected static Logger _log = LoggerFactory.getLogger(ApcLoadLevelCalculator.class);

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

  @Autowired
  private ThreadPoolTaskScheduler _taskScheduler;

  private long _updateInterval = 60 * 1000;

  @PostConstruct
    public void setup() {
      if(_taskScheduler != null) {
        UpdateThread updateThread = new UpdateThread(this);
        _taskScheduler.scheduleWithFixedDelay(updateThread, _updateInterval);
      } else {
        _log.warn("Unable to create thread to regularly update ApcLoadLevelCalculator, task scheduler unavailable");
        updateApcLoadLevelCalculatorConfigs();
      }
    }

  public void updateApcLoadLevelCalculatorConfigs() {
    xFactor = Float.parseFloat(_config.getConfigurationValueAsString("tds.apcLoadLevelCalculator.xFactor", Float.toString(DEFAULT_X_FACTOR)));
    yFactor = Float.parseFloat(_config.getConfigurationValueAsString("tds.apcLoadLevelCalculator.yFactor", Float.toString(DEFAULT_Y_FACTOR)));
    zFactor = Float.parseFloat(_config.getConfigurationValueAsString("tds.apcLoadLevelCalculator.zFactor", Float.toString(DEFAULT_Z_FACTOR)));
    xExpressFactor = Float.parseFloat(_config.getConfigurationValueAsString("tds.apcLoadLevelCalculator.xExpressFactor", Float.toString(DEFAULT_EXPRESS_X_FACTOR)));
    yExpressFactor = Float.parseFloat(_config.getConfigurationValueAsString("tds.apcLoadLevelCalculator.yExpressFactor", Float.toString(DEFAULT_EXPRESS_Y_FACTOR)));
    zExpressFactor = Float.parseFloat(_config.getConfigurationValueAsString("tds.apcLoadLevelCalculator.zExpressFactor", Float.toString(DEFAULT_EXPRESS_Z_FACTOR)));
  }


  public OccupancyStatus determineOccupancyStatus(Integer load, Integer capacity, AgencyAndId routeId) {
    if (load == null || capacity == null) {
      return null;
    }

    OccupancyStatus occupancyStatus = null;

    if (capacity > 0) {
      if (load == 0) {
        occupancyStatus = OccupancyStatus.MANY_SEATS_AVAILABLE;
      } 
      else {
        double loadFactor = load / (double) capacity;

        if (_nycRouteTypeService.isRouteExpress(routeId)) {
          if (loadFactor > zExpressFactor) {
            occupancyStatus = OccupancyStatus.FULL;
          } else if (loadFactor >= yExpressFactor) {
            occupancyStatus = OccupancyStatus.STANDING_ROOM_ONLY;
          } else if (loadFactor >= xExpressFactor) {
            occupancyStatus = OccupancyStatus.FEW_SEATS_AVAILABLE;
          } else {
            occupancyStatus = OccupancyStatus.MANY_SEATS_AVAILABLE;
          }
        } else {
          if (loadFactor > zFactor) {
            occupancyStatus = OccupancyStatus.FULL;
          } else if (loadFactor >= yFactor) {
            occupancyStatus = OccupancyStatus.STANDING_ROOM_ONLY;
          } else if (loadFactor >= xFactor) {
            occupancyStatus = OccupancyStatus.FEW_SEATS_AVAILABLE;
          } else {
            occupancyStatus = OccupancyStatus.MANY_SEATS_AVAILABLE;
          }
        }
      }
    }
    return occupancyStatus;
  }



  public OccupancyStatus toOccupancyStatus(NycVehicleLoadBean message) {
    if (useLoadFactor) return message.getLoad(); // the MTA will own this value
    OccupancyStatus occupancyStatus = determineOccupancyStatus(message.getEstLoad(),message.getEstCapacity(),AgencyAndIdLibrary.convertFromString(message.getRoute()));
    if (_log.isDebugEnabled()) {
      _log.debug("Vehicle ID: {}, Count: {}, Max Occupancy: {}, Route ID: {}, Express Status: {}, result: {}",
          message.getVehicleId(), message.getEstLoad(), message.getEstCapacity(), message.getRoute(), _nycRouteTypeService.isRouteExpress(AgencyAndIdLibrary.convertFromString(message.getRoute())), occupancyStatus);
    }
    return occupancyStatus;
  }

  public OccupancyStatus toOccupancyStatus(ApcLoadData message, AgencyAndId routeId) {
    OccupancyStatus occupancyStatus =  determineOccupancyStatus(message.getEstLoadAsInt(), message.getEstCapacityAsInt(), routeId);
    if(_log.isDebugEnabled()){
      _log.debug("Vehicle ID: {}, Count: {}, Max Occupancy: {}, Route ID: {}, Express Status: {}, result: {}",
          message.getVehicleAsId(), message.getEstLoadAsInt(), message.getEstCapacityAsInt(), routeId, _nycRouteTypeService.isRouteExpress(routeId), occupancyStatus);
    }
    return occupancyStatus;
  }


  public static class UpdateThread implements Runnable {

    private ApcLoadLevelCalculator resource;

    public UpdateThread(ApcLoadLevelCalculator resource) {
      this.resource = resource;
    }

    @Override
    public void run() {
        resource.updateApcLoadLevelCalculatorConfigs();
    }
  }
}
