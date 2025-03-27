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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.DateTime;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.impl.util.TcipUtil;
import org.onebusaway.nyc.transit_data_federation.services.vtw.VehiclePulloutService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.util.impl.*;
import org.onebusaway.nyc.util.property.PropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import tcip_final_4_0_0.*;

public class VehiclePulloutServiceImpl implements VehiclePulloutService {

  private static Logger _log = LoggerFactory.getLogger(VehiclePulloutServiceImpl.class);

  private ScheduledFuture<?> _updateTask = null;

  private boolean _enabled;

  private String dataPath;

  @Autowired
  private ThreadPoolTaskScheduler _taskScheduler;

  private ConfigurationService _configurationService;

  private TcipUtil _tcipUtil;

  private Map<AgencyAndId, SCHPullInOutInfo> _vehicleIdToPullouts = new ConcurrentHashMap<>(10000);

  private DataRetreivalService dataRetreivalService;

  private String _dataPath;

  @Autowired
  public void setTcipUtil(TcipUtil tcipUtil){
    _tcipUtil = tcipUtil;
  }

  @Autowired
  public void setConfigurationService(ConfigurationService configurationService) {
    this._configurationService = configurationService;
    configChanged();
  }



  public boolean getEnabled(){
    return _enabled;
  }

  public void setEnabled(boolean enabled) {
    _enabled = enabled;
  }

  public Map<AgencyAndId, SCHPullInOutInfo> getVehicleIdToPullouts(){
    return _vehicleIdToPullouts;
  }

  @PreDestroy
  public void destroy() {
    _log.info("destroy");
    if (_taskScheduler != null) {
      _taskScheduler.shutdown();
    }
  }

  @PostConstruct
  public void setup(){
    configChanged();
  }

  @Refreshable(dependsOn = {"tdm.vehiclePipoRefreshInterval", "tdm.vehiclePipoServiceEnabled", "tdm.vehiclePipoUrl"})
  private void configChanged() {
    _enabled = Boolean.parseBoolean(_configurationService.getConfigurationValueAsString("tdm.vehiclePipoServiceEnabled", "false"));
    if(_enabled) {
      initializeDataRetreivalService();
      Integer updateIntervalSecs = _configurationService.getConfigurationValueAsInteger("tdm.vehiclePipoRefreshInterval", 60);
      if (updateIntervalSecs != null && dataRetreivalService != null) {
        setUpdateFrequency(updateIntervalSecs);
      } else {
        cancelTask();
      }
    }
  }


  private void initializeDataRetreivalService() {
    _dataPath = _configurationService.getConfigurationValueAsString("tdm.vehiclePipoUrl", null);
    try {
      if (S3Utility.isS3Path(_dataPath)) {
        initializeS3DataRetreivalService(_dataPath);
      } else {
        initializeHttpDataRetreivalService();
      }
    } catch (Exception e){
      _log.error("Unable to instantiate Data Retreival Service from {}", _dataPath);
    }

  }

  private void initializeHttpDataRetreivalService() {
    dataRetreivalService = new HttpDataRetreivalService();
  }

  private void initializeS3DataRetreivalService(String path) throws Exception {
    String s3Username = PropertyUtil.getProperty("YardTrek.pipoAccessKey");
    String s3Password = PropertyUtil.getProperty("YardTrek.pipoSecretKey");
    if(s3Username != null && s3Password != null && path != null){
      dataRetreivalService = new S3DataRetreivalService(s3Username, s3Password, path);
    }
  }

  private void cancelTask(){
    if(_updateTask != null){
      _updateTask.cancel(true);
    }
  }

  @SuppressWarnings("unchecked")
  private void setUpdateFrequency(int seconds) {
    cancelTask();
    if(_enabled){
      _updateTask = _taskScheduler.scheduleWithFixedDelay(new UpdateThread(), seconds * 1000);
    }
  }

  @Override
  public String getAssignedBlockId(AgencyAndId vehicleId) {
//    if this is the only method that works great

    if(!_enabled){
      return null;
    }
    SCHPullInOutInfo info = getVehiclePullout(vehicleId);
    String agency = vehicleId.getAgencyId();
    if (info==null || info.getBlock()==null || agency == null) {
      return null;
    }
    return info.getBlock().getId();
  }

  @Override
  public SCHPullInOutInfo getVehiclePullout(AgencyAndId vehicle) {
    if (!_enabled || vehicle==null)
      return null;
    return _vehicleIdToPullouts.get(vehicle);
  }


  private class UpdateThread extends TimerTask {
    @Override
    public void run() {
      try {
        getAndRefreshData();
      } catch (Exception e) {
        _log.error("refreshData() failed: " + e.getMessage());
        e.printStackTrace();
      }
    }
  }

  public void getAndRefreshData() throws Exception {
    if(S3Utility.isS3Path(_dataPath)){
      String jsonData = getJsonDataFromS3();
      refreshDataFromJson(jsonData);
    }
    else{
      String xmlData = getXmlDataFromHttp(_dataPath);
      refreshDataFromXml(xmlData);
    }
  }

  public String getJsonDataFromS3() throws IOException {
    String json = dataRetreivalService.getAsString(dataPath);
    StringBuilder builder = new StringBuilder();
    builder.append("{\"data\":");
    builder.append(json);
    builder.append("}");
    json = builder.toString();
    return json;
  }

  public void refreshDataFromJson(String json) throws JSONException {
    Map<AgencyAndId, SCHPullInOutInfo> updatedVehicleIdToPullouts = new ConcurrentHashMap<>(10000);
    JSONObject jsonObject = new JSONObject(json);
    ObaSchPullOutList pullOutList = new ObaSchPullOutList();
    SchPullOutList.PullOuts pullOuts = new SchPullOutList.PullOuts();
    pullOutList.setPullOuts(pullOuts);
    JSONArray dataList = jsonObject.getJSONArray("data");
    for(int i =0; i< dataList.length(); i++){
      JSONObject datum = dataList.getJSONObject(i);
      SCHPullInOutInfo info = new SCHPullInOutInfo();
      SCHBlockIden block = new SCHBlockIden();
      block.setId(datum.getString("blockID"));
      info.setBlock(block);
      CPTTransitFacilityIden garage = new CPTTransitFacilityIden();
      garage.setAgdesig(datum.getString("authid"));
      info.setGarage(garage);
      CPTVehicleIden vehicle = new CPTVehicleIden();
      vehicle.setId(datum.getString("busnumber"));
      info.setVehicle(vehicle);

      SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SXXX");
//      info.setTime(simpleDateFormat.format(datum.getString("timestamp")));
      info.setTime(DateTime.parse(datum.getString("schedpo")));



      String agencyId = info.getGarage().getAgdesig();
      String vehicleId = info.getVehicle().getId();
      if(updatedVehicleIdToPullouts.get(new AgencyAndId(agencyId, vehicleId))!=null){
        SCHPullInOutInfo otherInfo = updatedVehicleIdToPullouts.get(new AgencyAndId(agencyId, vehicleId));
        if(otherInfo.getTime().isAfter(info.getTime())){
          continue;
        }
      }
      updatedVehicleIdToPullouts.put(new AgencyAndId(agencyId, vehicleId),info);
    }
    _vehicleIdToPullouts = updatedVehicleIdToPullouts;
  }

  private String getXmlDataFromHttp(String dataPath) throws IOException {
    return dataRetreivalService.getAsString(dataPath);
  }

  private void refreshDataFromXml(String xmlData) throws XMLStreamException, JAXBException {
    ObaSchPullOutList schPulloutList = _tcipUtil.getFromXml(xmlData);
    processVehiclePipoList(schPulloutList);
  }

  public void processVehiclePipoList(ObaSchPullOutList schPulloutList){
    if(!_enabled){
      return;
    }
    Map<AgencyAndId, SCHPullInOutInfo> updatedVehicleIdToPullouts = new ConcurrentHashMap<>(10000);
    String errorCode = schPulloutList.getErrorCode();
    if (errorCode != null && !errorCode.equals("0")){
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
      String agencyId = pullInOutInfo.getGarage().getAgdesig();
      String vehicleId = pullInOutInfo.getVehicle().getId();
      if(agencyId == null || vehicleId == null){
        _log.warn("Unable to add pipo info, missing agencyId {} or vehicleId {}", agencyId, vehicleId);
        continue;
      }
      updatedVehicleIdToPullouts.put(new AgencyAndId(agencyId, vehicleId), pullInOutInfo);
    }
    _vehicleIdToPullouts = updatedVehicleIdToPullouts;
  }

}
