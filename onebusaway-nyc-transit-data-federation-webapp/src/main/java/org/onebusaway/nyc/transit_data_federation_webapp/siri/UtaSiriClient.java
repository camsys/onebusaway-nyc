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
		records.addAll(refresh("11"));
		records.addAll(refresh("17"));
		records.addAll(refresh("2"));
		records.addAll(refresh("200"));
		records.addAll(refresh("201"));
		records.addAll(refresh("205"));
		records.addAll(refresh("209"));
		records.addAll(refresh("21"));
		records.addAll(refresh("213"));
		records.addAll(refresh("217"));
		records.addAll(refresh("218"));
		records.addAll(refresh("220"));
		records.addAll(refresh("223"));
		records.addAll(refresh("227"));
		records.addAll(refresh("228"));
		records.addAll(refresh("232"));
		records.addAll(refresh("240"));
		records.addAll(refresh("248"));
		records.addAll(refresh("2X"));
		records.addAll(refresh("3"));
		records.addAll(refresh("307"));
		records.addAll(refresh("313"));
		records.addAll(refresh("320"));
		records.addAll(refresh("33"));
		records.addAll(refresh("35"));
		records.addAll(refresh("354"));
		records.addAll(refresh("35M"));
		records.addAll(refresh("39"));
		records.addAll(refresh("41"));
		records.addAll(refresh("45"));
		records.addAll(refresh("451"));
		records.addAll(refresh("453"));
		records.addAll(refresh("454"));
		records.addAll(refresh("455"));
		records.addAll(refresh("456"));
		records.addAll(refresh("460"));
		records.addAll(refresh("461"));
		records.addAll(refresh("462"));
		records.addAll(refresh("463"));
		records.addAll(refresh("47"));
		records.addAll(refresh("470"));
		records.addAll(refresh("471"));
		records.addAll(refresh("472"));
		records.addAll(refresh("473"));
		records.addAll(refresh("477"));
		records.addAll(refresh("500"));
		records.addAll(refresh("509"));
		records.addAll(refresh("513"));
		records.addAll(refresh("516"));
		records.addAll(refresh("519"));
		records.addAll(refresh("520"));
		records.addAll(refresh("525"));
		records.addAll(refresh("526"));
		records.addAll(refresh("54"));
		records.addAll(refresh("551"));
		records.addAll(refresh("6"));
		records.addAll(refresh("603"));
		records.addAll(refresh("604"));
		records.addAll(refresh("606"));
		records.addAll(refresh("608"));
		records.addAll(refresh("612"));
		records.addAll(refresh("613"));
		records.addAll(refresh("616"));
		records.addAll(refresh("62"));
		records.addAll(refresh("625"));
		records.addAll(refresh("626"));
		records.addAll(refresh("627"));
		records.addAll(refresh("630"));
		records.addAll(refresh("640"));
		records.addAll(refresh("645"));
		records.addAll(refresh("650"));
		records.addAll(refresh("664"));
		records.addAll(refresh("665"));
		records.addAll(refresh("667"));
		records.addAll(refresh("674"));
		records.addAll(refresh("675"));
		records.addAll(refresh("701"));
		records.addAll(refresh("703"));
		records.addAll(refresh("704"));
		records.addAll(refresh("710"));
		records.addAll(refresh("72"));
		records.addAll(refresh("720"));
		records.addAll(refresh("750"));
		records.addAll(refresh("805"));
		records.addAll(refresh("806"));
		records.addAll(refresh("807"));
		records.addAll(refresh("811"));
		records.addAll(refresh("821"));
		records.addAll(refresh("822"));
		records.addAll(refresh("830"));
		records.addAll(refresh("831"));
		records.addAll(refresh("832"));
		records.addAll(refresh("833"));
		records.addAll(refresh("834"));
		records.addAll(refresh("835"));
		records.addAll(refresh("836"));
		records.addAll(refresh("838"));
		records.addAll(refresh("840"));
		records.addAll(refresh("841"));
		records.addAll(refresh("842"));
		records.addAll(refresh("850"));
		records.addAll(refresh("862"));
		records.addAll(refresh("863"));
		records.addAll(refresh("880"));
		records.addAll(refresh("9"));
		records.addAll(refresh("901"));
		records.addAll(refresh("902"));
		records.addAll(refresh("919"));
		records.addAll(refresh("920"));
		records.addAll(refresh("951"));
		records.addAll(refresh("952"));
		records.addAll(refresh("953"));
		records.addAll(refresh("954"));
		records.addAll(refresh("960"));
		records.addAll(refresh("962"));
		records.addAll(refresh("990"));
		records.addAll(refresh("992"));
		records.addAll(refresh("F400"));
		records.addAll(refresh("F401"));
		records.addAll(refresh("F504"));
		records.addAll(refresh("F514"));
		records.addAll(refresh("F518"));
		records.addAll(refresh("F522"));
		records.addAll(refresh("F534"));
		records.addAll(refresh("F546"));
		records.addAll(refresh("F547"));
		records.addAll(refresh("F556"));
		records.addAll(refresh("F570"));
		records.addAll(refresh("F578"));
		records.addAll(refresh("F590"));
		records.addAll(refresh("F618"));
		records.addAll(refresh("F638"));
		records.addAll(refresh("F868"));
		records.addAll(refresh("F94"));

    // records.addAll(refresh("213"));
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

    _log.debug("vms=" + siri.getServiceDelivery().getVehicleMonitoringDelivery().size());
    
    for (VehicleMonitoringDeliveryStructure vehicleDelivery : siri.getServiceDelivery().getVehicleMonitoringDelivery()) {
      _log.debug("vas=" + vehicleDelivery.getVehicleActivity().size());
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

        if (mvj == null) continue;
        VehicleRefStructure vehicleRef = mvj.getVehicleRef();
        if (vehicleRef == null || vehicleRef.getValue() == null) {
          _log.error("missing vehicleRef!");
          continue;
        }
        _log.debug("vehicleId=" + vehicleRef.getValue());
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
            _log.debug("couldn't find trip either!");
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

        _log.debug("adding r=" + r);
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
      _log.debug("no framed vehicles");
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
      _log.debug("unparseable:", ex);
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
