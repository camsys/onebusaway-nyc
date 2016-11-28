package org.onebusaway.nyc.transit_data_federation.impl.vtw;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.services.vtw.VehiclePulloutService;
import org.onebusaway.util.services.configuration.ConfigurationService;
import org.onebusaway.nyc.util.impl.vtw.PullOutApiLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import tcip_final_4_0_0.CPTVehicleIden;
import tcip_final_4_0_0.ObaSchPullOutList;
import tcip_final_4_0_0.ObjectFactory;
import tcip_final_4_0_0.SCHPullInOutInfo;

public class VehiclePulloutServiceImpl implements VehiclePulloutService {
	
  private JAXBContext context = null;

  private static Logger _log = LoggerFactory.getLogger(VehiclePulloutServiceImpl.class);

  private ScheduledFuture<VehiclePulloutServiceImpl.UpdateThread> _updateTask = null;
  
  private boolean _enabled;

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

  
  public synchronized void refreshData() throws Exception {

    _vehicleIdToPullouts.clear();

    try {
      String response = _pullOutApiLibrary.getContentsOfUrlAsString("uts","active","tcip","");
      
      ObaSchPullOutList schPulloutList = getFromXml(response);
     
      String errorCode = schPulloutList.getErrorCode();
      if (errorCode != null && !errorCode.equals("0")){
        if (errorCode.equalsIgnoreCase("1")) {
          _log.debug(response);
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
        _vehicleIdToPullouts.put(new AgencyAndId(v.getAgdesig(), v.getId()), pullInOutInfo);
      }
    } catch (Exception e) {
      _log.error(e.getMessage());
      throw e;
    }
  }
  
  @PostConstruct
  public void setup(){
	  setupJaxbContext();
	  startUpdateProcess();
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
    if (_updateTask != null) {
      _updateTask.cancel(true);
    }
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

  @Refreshable(dependsOn = {"tdm.vehiclePipoRefreshInterval", "tdm.vehiclePipoServiceEnabled"})
  private void configChanged() {
    Integer updateIntervalSecs = _configurationService.getConfigurationValueAsInteger("tdm.vehiclePipoRefreshInterval", null);
    _enabled = Boolean.parseBoolean(_configurationService.getConfigurationValueAsString("tdm.vehiclePipoServiceEnabled", "false"));
    
    if (updateIntervalSecs != null) {
      setUpdateFrequency(updateIntervalSecs);
    }
  }

  
  @Override
  public SCHPullInOutInfo getVehiclePullout(AgencyAndId vehicle) {
    if (vehicle==null)
      return null;
    return _vehicleIdToPullouts.get(vehicle);
  }

  @Override
  public String getAssignedBlockId(AgencyAndId vehicleId) {
    SCHPullInOutInfo info = getVehiclePullout(vehicleId);
    if (info==null || info.getBlock()==null) {
      return null;
    }    
    return info.getBlock().getId();
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
  
  public ObaSchPullOutList getFromXml(String xml) throws XMLStreamException, JAXBException{      
	  XMLInputFactory xmlInputFact = XMLInputFactory.newInstance();
	  XMLStreamReader reader = xmlInputFact.createXMLStreamReader(
              new StringReader(xml));
      
	  Unmarshaller u = context.createUnmarshaller();
	  
      JAXBElement<ObaSchPullOutList> doc = (JAXBElement<ObaSchPullOutList>) u.unmarshal(reader,ObaSchPullOutList.class);
      return doc.getValue();
  }
}
