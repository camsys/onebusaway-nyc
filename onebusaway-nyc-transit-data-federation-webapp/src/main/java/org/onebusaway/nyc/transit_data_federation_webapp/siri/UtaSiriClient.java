package org.onebusaway.nyc.transit_data_federation_webapp.siri;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.siri.SiriXmlSerializer;
import org.onebusaway.realtime.api.VehicleLocationListener;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import uk.org.siri.siri.BlockRefStructure;
import uk.org.siri.siri.FramedVehicleJourneyRefStructure;
import uk.org.siri.siri.LocationStructure;
import uk.org.siri.siri.Siri;
import uk.org.siri.siri.VehicleActivityStructure;
import uk.org.siri.siri.VehicleActivityStructure.MonitoredVehicleJourney;
import uk.org.siri.siri.VehicleMonitoringDeliveryStructure;
import uk.org.siri.siri.VehicleRefStructure;

public class UtaSiriClient {

  private static Logger _log = LoggerFactory.getLogger(UtaSiriClient.class);
  private SiriXmlSerializer _siriXmlSerializer = new SiriXmlSerializer();
  protected DefaultHttpClient _client;
  private TransitGraphDao _transitGraphDao;
  private VehicleLocationListener _vehicleLocationListener;
  private BlockCalendarService _blockCalendarService;
  private ScheduledExecutorService _refreshExecutor;
  /**
   * Time, in minutes,
   */
  private int _blockInstanceSearchWindow = 30;

  @Autowired
  public void setTransitGraphDao(TransitGraphDao transitGraphDao) {
    _transitGraphDao = transitGraphDao;
  }

  @Autowired
  public void set(VehicleLocationListener vehicleLocationListener) {
    _vehicleLocationListener = vehicleLocationListener;
  }

  @Autowired
  public void setBlockCalendarService(BlockCalendarService blockCalendarService) {
    _blockCalendarService = blockCalendarService;
  }

  @PostConstruct
  public void start() throws Exception, IOException {
    _client = new DefaultHttpClient();
    _refreshExecutor = Executors.newSingleThreadScheduledExecutor();
    _refreshExecutor.scheduleAtFixedRate(new RefreshTransitData(this), 0l, 30l,
        TimeUnit.SECONDS);

  }

  public void refresh() throws Exception {
    List<VehicleLocationRecord> records = new ArrayList<VehicleLocationRecord>();
//    refresh("11");
//    refresh("17");
//    refresh("2");
//    refresh("200");
//    refresh("201");
//    refresh("205");
//    refresh("209");
//    refresh("21");
    records.addAll(refresh("213"));
    if (!records.isEmpty()) {
      _log.error("found " + records.size() + " records");
      _vehicleLocationListener.handleVehicleLocationRecords(records);
    }
  }

  public List<VehicleLocationRecord> refresh(String route) throws Exception {
    String url = "http://api.rideuta.com/SIRI/SIRI.svc/VehicleMonitor/ByRoute?route="
        + route + "&onwardcalls=false&usertoken=UPBML0P0ZO0";
    HttpGet get = new HttpGet(url);
    HttpResponse response = _client.execute(get);
    HttpEntity entity = response.getEntity();
    Reader responseReader = new InputStreamReader(entity.getContent());
    Siri siri = _siriXmlSerializer.fromXml(filter(responseReader));
    List<VehicleLocationRecord> records = handleUtaResponse(siri, false);
    return records;
  }

  private static class RefreshTransitData implements Runnable {

    UtaSiriClient _client;

    public RefreshTransitData(UtaSiriClient client) {
      _client = client;
    }

    public void run() {
      try {
        _client.refresh();
      } catch (Exception any) {
        _log.error("any=", any);
      }
    }

  }

  private List<VehicleLocationRecord> handleUtaResponse(Siri siri, boolean b) {
    List<VehicleLocationRecord> records = new ArrayList<VehicleLocationRecord>();
    Date now = new Date();
    long timeFrom = now.getTime() - _blockInstanceSearchWindow * 60 * 1000;
    long timeTo = now.getTime() + _blockInstanceSearchWindow * 60 * 1000;

    _log.info("vms=" + siri.getServiceDelivery().getVehicleMonitoringDelivery().size());
    
    for (VehicleMonitoringDeliveryStructure vehicleDelivery : siri.getServiceDelivery().getVehicleMonitoringDelivery()) {
      _log.info("vas=" + vehicleDelivery.getVehicleActivity().size());
      for (VehicleActivityStructure vehicleActivity : vehicleDelivery.getVehicleActivity()) {
        Date time = vehicleActivity.getRecordedAtTime();
        if (time == null) {
          time = now;
        }
        
        MonitoredVehicleJourney mvj = vehicleActivity.getMonitoredVehicleJourney();

        // TODO ask about schedule deviation!
        // Duration delay = mvj.getDelay();
        // if (delay == null) {
        // _log.error("missing delay!");
        // continue;
        // }

        VehicleRefStructure vehicleRef = mvj.getVehicleRef();
        if (vehicleRef == null || vehicleRef.getValue() == null) {
          _log.error("missing vehicleRef!");
          continue;
        }
        _log.info("vehicleId=" + vehicleRef.getValue());
        List<String> defaultAgencyIds = new ArrayList<String>();
        defaultAgencyIds.add("Utah Transit Authority");
        defaultAgencyIds.add("1");

        BlockEntry block = getBlockForMonitoredVehicleJourney(mvj,
            defaultAgencyIds);
        if (block == null) {
          TripEntry trip = getTripForMonitoredVehicleJourney(mvj,
              defaultAgencyIds);
          if (trip != null) {
            block = trip.getBlock();
          } else {
            _log.error("couldn't find trip either!");
          }
        }

        if (block == null) {
          _log.error("could not find block!");
          continue;
        }

        List<BlockInstance> instances = _blockCalendarService.getActiveBlocks(
            block.getId(), timeFrom, timeTo);

        // TODO : We currently assume that a block won't overlap with itself
        if (instances.size() != 1) {
          _log.error("block overlaps!");
          continue;
        }

        BlockInstance instance = instances.get(0);

        VehicleLocationRecord r = new VehicleLocationRecord();
        r.setTimeOfRecord(time.getTime());
        r.setServiceDate(instance.getServiceDate());
        r.setBlockId(block.getId());

        String agencyId = block.getId().getAgencyId();
        r.setVehicleId(new AgencyAndId(agencyId, vehicleRef.getValue()));

        // what to do here? TODO
        // r.setScheduleDeviation(delay.getTimeInMillis(now) / 1000);

        LocationStructure location = mvj.getVehicleLocation();
        if (location != null) {
          r.setCurrentLocationLat(location.getLatitude().doubleValue());
          r.setCurrentLocationLon(location.getLongitude().doubleValue());
        }

        _log.info("adding r=" + r);
        records.add(r);
      }
    }
    return records;
  }

  private BlockEntry getBlockForMonitoredVehicleJourney(
      MonitoredVehicleJourney mvj, List<String> defaultAgencyIds) {

    BlockRefStructure blockRef = mvj.getBlockRef();
    if (blockRef == null || blockRef.getValue() == null) {
//      _log.error("no block refs");
      return null;
    }

    for (String agencyId : defaultAgencyIds) {
      AgencyAndId blockId = new AgencyAndId(agencyId, blockRef.getValue());
      BlockEntry blockEntry = _transitGraphDao.getBlockEntryForId(blockId);
      if (blockEntry != null)
        return blockEntry;
    }

    /**
     * Try parsing the id itself
     */
    try {
      AgencyAndId blockId = AgencyAndId.convertFromString(blockRef.getValue());
      return _transitGraphDao.getBlockEntryForId(blockId);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private TripEntry getTripForMonitoredVehicleJourney(
      MonitoredVehicleJourney mvj, List<String> defaultAgencyIds) {
    FramedVehicleJourneyRefStructure fvjRef = mvj.getFramedVehicleJourneyRef();
    if (fvjRef == null || fvjRef.getDatedVehicleJourneyRef() == null) {
      _log.error("no framed vehicles");
      return null;
    }

    for (String agencyId : defaultAgencyIds) {
      AgencyAndId tripId = new AgencyAndId(agencyId,
          fvjRef.getDatedVehicleJourneyRef());
      TripEntry tripEntry = _transitGraphDao.getTripEntryForId(tripId);
      if (tripEntry != null)
        return tripEntry;
    }

    /**
     * Try parsing the id itself
     */
    try {
      AgencyAndId tripId = AgencyAndId.convertFromString(fvjRef.getDatedVehicleJourneyRef());
      return _transitGraphDao.getTripEntryForId(tripId);
    } catch (IllegalArgumentException ex) {
      _log.error("unparseable:", ex);
      return null;
    }
  }

  private Reader filter(Reader responseReader) {
    try {
      StringBuilder input = new StringBuilder();
      StringBuilder output = new StringBuilder();
      copyReaderToStringBuilder(responseReader, input);
      int startPosition = input.indexOf("<VehicleMonitoringDelivery");
      int stopPosition = input.indexOf("</Siri");
      if (startPosition > 0 && stopPosition > startPosition) {
        // GOTCHA: appending the xmlns is key to getting this to parse!
        output.append("<Siri version=\"1.3\" xmlns=\"http://www.siri.org.uk/siri\"><ServiceDelivery>");
        output.append(input.subSequence(startPosition, stopPosition));
        output.append("</ServiceDelivery></Siri>");
        return new StringReader(output.toString());
      }
    } catch (Exception any) {
      _log.error("filter blew:", any);
    }
    _log.error("did not find vm");
    return null;
  }

  protected Reader copyReaderToStringBuilder(Reader responseReader,
      StringBuilder b) throws IOException {
    char[] buffer = new char[1024];
    while (true) {
      int rc = responseReader.read(buffer);
      if (rc == -1)
        break;
      b.append(buffer, 0, rc);
    }

    responseReader.close();
    responseReader = new StringReader(b.toString());
    return responseReader;
  }

}
