package org.onebusaway.nyc.transit_data_federation_webapp.siri;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.datatype.Duration;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.realtime.api.VehicleLocationListener;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.onebusaway.siri.core.ESiriModuleType;
import org.onebusaway.siri.core.SiriClientRequest;
import org.onebusaway.siri.core.SiriCommon;
import org.onebusaway.siri.core.SiriLibrary;
import org.onebusaway.siri.core.versioning.SiriVersioning;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import uk.org.siri.siri.LocationStructure;
import uk.org.siri.siri.Siri;
import uk.org.siri.siri.VehicleActivityStructure;
import uk.org.siri.siri.VehicleActivityStructure.MonitoredVehicleJourney;
import uk.org.siri.siri.VehicleMonitoringDeliveryStructure;
import uk.org.siri.siri.VehicleRefStructure;

public class UtaSiriFeed extends SiriCommon {
  
  private static Logger _log = LoggerFactory.getLogger(UtaSiriFeed.class);
  private VehicleLocationListener _vehicleLocationListener;
  private BlockCalendarService _blockCalendarService;
  /**
   * Time, in minutes,
   */
  private int _blockInstanceSearchWindow = 30;
  @Autowired
  public void set(VehicleLocationListener vehicleLocationListener) {
    _vehicleLocationListener = vehicleLocationListener;
  }
  @Autowired
  public void setBlockCalendarService(BlockCalendarService blockCalendarService) {
    _blockCalendarService = blockCalendarService;
  }
  
  
  @PostConstruct
  public void start()  {
    try {
    _log.error("making siri request");
    
    _client= new DefaultHttpClient();
    SiriVersioning versioning = SiriVersioning.getInstance();
    SiriClientRequest request = new SiriClientRequest();
    request.setSubscribe(false);
    String url = "http://api.rideuta.com/SIRI/SIRI.svc/VehicleMonitor/ByRoute?route=2&onwardcalls=true&usertoken=UPBML0P0ZO0";
    request.setTargetUrl(url);

    HttpResponse response = sendHttpRequestWithResponse(url, "");
    if (response == null) {
      _log.error("no response");
      
    }
    HttpEntity entity = response.getEntity();
    String responseContent = null;
    Object responseData = null;

    Reader responseReader = new InputStreamReader(entity.getContent());

    _log.error("response content length: {}", entity.getContentLength());

    if (entity.getContentLength() != 0) {
      responseData = unmarshall(responseReader);
      responseData = versioning.getPayloadAsVersion(responseData,
          versioning.getDefaultVersion());
    }
    
    Siri siri = (Siri) responseData;
    
    handleUtaResponse(siri, false, request);
    } catch (Exception any) {
      _log.error("start broke:", any);
    }
  }

  private void handleUtaResponse(Siri siri, boolean b, SiriClientRequest request) {
    List<VehicleLocationRecord> records = new ArrayList<VehicleLocationRecord>();
    
    _log.error("service delivery=" + siri.getServiceDelivery());
    
    VehicleMonitoringDeliveryStructure deliveryForModule =
        (VehicleMonitoringDeliveryStructure) SiriLibrary.getServiceDeliveriesForModule(
        siri.getServiceDelivery(), ESiriModuleType.VEHICLE_MONITORING);
    
    Date now = new Date();
    long timeFrom = now.getTime() - _blockInstanceSearchWindow * 60 * 1000;
    long timeTo = now.getTime() + _blockInstanceSearchWindow * 60 * 1000;

     _log.error("extensions=" + siri.getExtensions());
     _log.error("delivery=" + deliveryForModule);
     
     for (VehicleActivityStructure vehicleActivity : deliveryForModule.getVehicleActivity()) {

       Date time = vehicleActivity.getRecordedAtTime();
       if (time == null)
         time = now;

       MonitoredVehicleJourney mvj = vehicleActivity.getMonitoredVehicleJourney();

       Duration delay = mvj.getDelay();
       if (delay == null)
         continue;

       VehicleRefStructure vehicleRef = mvj.getVehicleRef();
       if (vehicleRef == null || vehicleRef.getValue() == null)
         continue;

       BlockEntry block = null;
//       BlockEntry block = getBlockForMonitoredVehicleJourney(mvj,
//           endpointDetails);
//       if (block == null) {
//         TripEntry trip = getTripForMonitoredVehicleJourney(mvj, endpointDetails);
//         if (trip != null)
//           block = trip.getBlock();
//       }

       if (block == null)
         continue;

       List<BlockInstance> instances = _blockCalendarService.getActiveBlocks(
           block.getId(), timeFrom, timeTo);

       // TODO : We currently assume that a block won't overlap with itself
       if (instances.size() != 1)
         continue;

       BlockInstance instance = instances.get(0);

       VehicleLocationRecord r = new VehicleLocationRecord();
       r.setTimeOfRecord(time.getTime());
       r.setServiceDate(instance.getServiceDate());
       r.setBlockId(block.getId());

       String agencyId = block.getId().getAgencyId();
       r.setVehicleId(new AgencyAndId(agencyId, vehicleRef.getValue()));

       r.setScheduleDeviation(delay.getTimeInMillis(now) / 1000);

       LocationStructure location = mvj.getVehicleLocation();
       if (location != null) {
         r.setCurrentLocationLat(location.getLatitude().doubleValue());
         r.setCurrentLocationLon(location.getLongitude().doubleValue());
       }

       records.add(r);
     }

     if (!records.isEmpty())
       _vehicleLocationListener.handleVehicleLocationRecords(records);
  }
}
