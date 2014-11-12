package org.onebusaway.nyc.transit_data_federation.impl.tdm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.PostConstruct;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.services.tdm.VehiclePulloutService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.util.impl.tdm.TransitDataManagerApiLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import tcip_final_4_0_0.CPTVehicleIden;
import tcip_final_4_0_0.ObaSchPullOutList;
import tcip_final_4_0_0.SCHPullInOutInfo;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import com.google.gson.JsonObject;

@Component
public class VehiclePulloutServiceImpl implements VehiclePulloutService {

  private static Logger _log = LoggerFactory.getLogger(VehiclePulloutServiceImpl.class);

  private ScheduledFuture<VehiclePulloutServiceImpl.UpdateThread> _updateTask = null;

  @Autowired
  private ThreadPoolTaskScheduler _taskScheduler;

  private ConfigurationService _configurationService;

  @Autowired
  private TransitDataManagerApiLibrary _transitDataManagerApiLibrary = null;

  private Map<AgencyAndId, SCHPullInOutInfo> _vehicleIdToPullouts = new HashMap<AgencyAndId, SCHPullInOutInfo>();

  @Autowired
  public void setConfigurationService(ConfigurationService configurationService) {
    this._configurationService = configurationService;
    configChanged();
  }

  public void setTransitDataManagerApiLibrary(
      TransitDataManagerApiLibrary apiLibrary) {
    this._transitDataManagerApiLibrary = apiLibrary;
  }

  
  public synchronized void refreshData() throws Exception {
    
    JaxbAnnotationModule module = new JaxbAnnotationModule();
    ObjectMapper m = new ObjectMapper();
    m.registerModule(module);
    m.setSerializationInclusion(Include.NON_NULL);

    _vehicleIdToPullouts.clear();

    try {
      List<JsonObject> items = _transitDataManagerApiLibrary.getItemsForRequestNoCheck("pullouts", "realtime", "list");
      if (items.isEmpty()) {
        String message = "realtime pullouts API call result should not be empty.";
        _log.error(message);
        return;
      }
      if (items.size() != 1) {
        String message = "realtime pullouts API call should have returned only one item, got " + items.size();
        _log.error(message);
        throw new RuntimeException(message);
      }
      JsonObject item = items.get(0);
      String s = item.toString();
      ObaSchPullOutList schPulloutList = m.readValue(s, ObaSchPullOutList.class);
      String errorCode = schPulloutList.getErrorCode();
      if (errorCode != null) {
        if (errorCode.equalsIgnoreCase("1")) {
          _log.warn("Pullout list contained no pullouts.");
          return;
        }
        String message = "Pullout list API returned error " + errorCode + ", description: " + schPulloutList.getErrorDescription();
        _log.error(message);
        throw new RuntimeException(message);
      }
      List<SCHPullInOutInfo> pulloutList = schPulloutList.getPullOuts().getPullOut();
      for (SCHPullInOutInfo pullInOutInfo: pulloutList) {
        CPTVehicleIden v = pullInOutInfo.getVehicle();
        _vehicleIdToPullouts.put(AgencyAndId.convertFromString(v.getAgdesig() + "_" + v.getId()), pullInOutInfo);
      }
    } catch (Exception e) {
      _log.error(e.getMessage());
      throw e;
    }
  }

  private class UpdateThread extends TimerTask {
    @Override
    public void run() {
      try {
        refreshData();
      } catch (Exception e) {
        _log.error("refreshData() failed: " + e.getMessage()); 
        e.printStackTrace();
      }
    }
  }

  @Refreshable(dependsOn = "tdm.vehicleAssignmentRefreshInterval")
  private void configChanged() {
    Integer updateInterval = _configurationService.getConfigurationValueAsInteger("tdm.vehicleAssignmentRefreshInterval", null);

    if (updateInterval != null) {
      setUpdateFrequency(updateInterval);
    }
  }

  @SuppressWarnings("unchecked")
  private void setUpdateFrequency(int seconds) {
    if (_updateTask != null) {
      _updateTask.cancel(true);
    }
    _updateTask = _taskScheduler.scheduleWithFixedDelay(new UpdateThread(), seconds * 1000);
  }

  @PostConstruct
  private void startUpdateProcess() {
    if (_updateTask==null) {
      setUpdateFrequency(5 * 60); // 5m
    }
  }
  
  @Override
  public SCHPullInOutInfo getVehiclePullout(AgencyAndId vehicle) {
    if (vehicle==null)
      return null;
    String lookup = vehicle.getAgencyId() + " " + vehicle.getId();    
    return _vehicleIdToPullouts.get(lookup);
  }

  @Override
  public String getAssignedBlockId(AgencyAndId vehicleId) {
    SCHPullInOutInfo info = getVehiclePullout(vehicleId);
    if (info==null || info.getBlock()==null) {
      return null;
    }    
    return info.getBlock().getId();
  }
}
