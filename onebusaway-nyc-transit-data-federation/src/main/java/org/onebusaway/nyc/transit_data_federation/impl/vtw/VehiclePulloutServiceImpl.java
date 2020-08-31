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

import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.services.vtw.VehiclePulloutService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.util.impl.vtw.PullOutApiLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import tcip_final_4_0_0.CPTVehicleIden;
import tcip_final_4_0_0.ObaSchPullOutList;
import tcip_final_4_0_0.ObjectFactory;
import tcip_final_4_0_0.SCHPullInOutInfo;

public class VehiclePulloutServiceImpl implements VehiclePulloutService {

  private JAXBContext context = null;

  private static Logger _log = LoggerFactory.getLogger(VehiclePulloutServiceImpl.class);

  private ScheduledFuture<?> _updateTask = null;

  private boolean _enabled;

  private ObjectMapper _mapper;

  private URL _url;

  @Autowired
  private ThreadPoolTaskScheduler _taskScheduler;

  private ConfigurationService _configurationService;

  @Autowired
  private PullOutApiLibrary _pullOutApiLibrary = null;

  private Map<AgencyAndId, SCHPullInOutInfo> _vehicleIdToPullouts = new HashMap<AgencyAndId, SCHPullInOutInfo>();

  @Autowired
  public void setConfigurationService(ConfigurationService configurationService) {
    this._configurationService = configurationService;
    configChanged();
  }

  public void setTransitDataManagerApiLibrary(
          PullOutApiLibrary apiLibrary) {
    this._pullOutApiLibrary = apiLibrary;
  }

  public URL getUrl() {
    return _url;
  }

  public void setUrl(URL url) {
    _url = url;
  }

  @PostConstruct
  public void setup(){
    setupMapper();
    setupJaxbContext();
    startUpdateProcess();
  }

  @Refreshable(dependsOn = {"tdm.vehiclePipoRefreshInterval", "tdm.vehiclePipoServiceEnabled", "tdm.vehiclePipoUrl"})
  private void configChanged() {
    String url = _configurationService.getConfigurationValueAsString("vtw.unassignedVehicleServiceUrl", null);

    Integer updateIntervalSecs = _configurationService.getConfigurationValueAsInteger("tdm.vehiclePipoRefreshInterval", null);
    _enabled = Boolean.parseBoolean(_configurationService.getConfigurationValueAsString("tdm.vehiclePipoServiceEnabled", "false"));

    try {
      setUrl(new URL(url));
    } catch (MalformedURLException e) {
      _log.error("Unable to reach vehicle pipo service URL {}", url, e);
      _enabled = false;
    }

    if (updateIntervalSecs != null) {
      setUpdateFrequency(updateIntervalSecs);
    } else {
      cancelTask();
    }
  }

  private void cancelTask(){
    if(_updateTask != null){
      _updateTask.cancel(true);
    }
  }

  public void setupMapper(){
    _mapper = new ObjectMapper();
    _mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public void setupJaxbContext(){
    try {
      context = JAXBContext.newInstance(
              ObaSchPullOutList.class);
    } catch(Exception e) {
      _log.error("Failed to Serialize ObaSchPullOutList to XML", e);
    }
  }

  private void startUpdateProcess() {
    Integer updateIntervalSecs = _configurationService.getConfigurationValueAsInteger("tdm.vehiclePipoRefreshInterval", 60);
    if (_updateTask==null) {
      setUpdateFrequency(updateIntervalSecs);
    }
  }

  @SuppressWarnings("unchecked")
  private void setUpdateFrequency(int seconds) {
    cancelTask();
    if(_enabled){
      _updateTask = _taskScheduler.scheduleWithFixedDelay(new UpdateThread(), seconds * 1000);
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

  public synchronized void refreshData() throws Exception {
    _vehicleIdToPullouts.clear();
    try {
      ObaSchPullOutList schPulloutList = getPullOutListFromXml(_url);
      processVehiclePipoList(schPulloutList, _vehicleIdToPullouts);
    } catch (Exception e) {
      _log.error("Unable to process vehicle pipo information", e);
      throw e;
    }
  }

  public void processVehiclePipoList(ObaSchPullOutList schPulloutList, Map<AgencyAndId, SCHPullInOutInfo> vehicleIdToPullouts){
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
      vehicleIdToPullouts.put(new AgencyAndId(agencyId, vehicleId), pullInOutInfo);
    }
  }

  public ObaSchPullOutList getPullOutListFromJson() throws Exception {
    if (_mapper == null) {
      _enabled = false;
      cancelTask();
      throw new Exception("Object Mapper unavailable, disabling vehicle pullout service");
    }
    ObaSchPullOutList response = _mapper.readValue(_url, ObaSchPullOutList.class);
    return response;
  }

  public String getAsJson(ObaSchPullOutList o) throws JsonProcessingException {
    return _mapper.writeValueAsString(o);
  }

  public ObaSchPullOutList getPullOutListFromXml(URL url) throws JAXBException, MalformedURLException {
    JAXBContext jaxbContext  = JAXBContext.newInstance(ObaSchPullOutList.class);
    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    ObaSchPullOutList list = (ObaSchPullOutList) jaxbUnmarshaller.unmarshal(url);
    return list;
  }

  public ObaSchPullOutList getPullOutListFromXml() throws Exception {
    String response = _pullOutApiLibrary.getContentsOfUrlAsString("uts","active","tcip","");
    return getFromXml(response);
  }

  public ObaSchPullOutList getFromXml(String xml) throws XMLStreamException, JAXBException{
    XMLInputFactory xmlInputFact = XMLInputFactory.newInstance();
    XMLStreamReader reader = xmlInputFact.createXMLStreamReader(
            new StringReader(xml));

    Unmarshaller u = context.createUnmarshaller();

    JAXBElement<ObaSchPullOutList> doc = (JAXBElement<ObaSchPullOutList>) u.unmarshal(reader,ObaSchPullOutList.class);
    return doc.getValue();
  }

  public String getAsXml(ObaSchPullOutList o) throws JAXBException{
    ObjectFactory f = new ObjectFactory();
    JAXBElement<ObaSchPullOutList> pullOutListElement = f.createObaSchPullOutList(o);
    Marshaller m = JAXBContext.newInstance(ObjectFactory.class).createMarshaller();
    m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

    StringWriter writer = new StringWriter();

    m.marshal(pullOutListElement, writer);
    return writer.toString();
  }

  @Override
  public String getAssignedBlockId(AgencyAndId vehicleId) {
    SCHPullInOutInfo info = getVehiclePullout(vehicleId);
    String agency = vehicleId.getAgencyId();
    if (info==null || info.getBlock()==null || agency == null) {
      return null;
    }
    return agency + AgencyAndId.ID_SEPARATOR + info.getBlock().getId();
  }

  @Override
  public SCHPullInOutInfo getVehiclePullout(AgencyAndId vehicle) {
    if (vehicle==null)
      return null;
    return _vehicleIdToPullouts.get(vehicle);
  }

  @PreDestroy
  public void destroy() {
    _log.info("destroy");
    if (_taskScheduler != null) {
      _taskScheduler.shutdown();
    }
  }




}
