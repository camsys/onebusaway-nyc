package org.onebusaway.nyc.vehicle_tracking.impl.unassigned;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.queue.model.RealtimeEnvelope;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.util.impl.tdm.TransitDataManagerApiLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.VehicleInferenceInstance;
import org.onebusaway.nyc.vehicle_tracking.model.unassigned.Records;
import org.onebusaway.nyc.vehicle_tracking.model.unassigned.UnassignedVehicleRecord;
import org.onebusaway.nyc.vehicle_tracking.services.inference.VehicleLocationInferenceService;
import org.onebusaway.nyc.vehicle_tracking.services.queue.OutputQueueSenderService;
import org.onebusaway.nyc.vehicle_tracking.services.unassigned.UnassignedVehicleService;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import tcip_final_3_0_5_1.CPTVehicleIden;
import tcip_final_3_0_5_1.CcLocationReport;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
/**
 * Integrate with an external feed of depot pull-out vehicles.  If we don't have real-time
 * records for these vehicles we have SPOOKY buses (aka zombie buses) that are in service
 * but are not reporting.  Put an inference record on the queue to represent this to downstream
 * services.
 */
public class UnassignedVehicleServiceImpl implements UnassignedVehicleService {

    private static Logger _log = LoggerFactory.getLogger(UnassignedVehicleServiceImpl.class);
    private static int LATENT_WINDOW_START_SECONDS = 90 * 1000;
    private static int LATENT_WINDOW_END_SECONDS = 300 * 1000;

    private ScheduledFuture<?> _updateTask = null;

    private boolean _enabled;

    private boolean _debug = false;

    private boolean _freshenTimestamps = true;
    /**
     * BusTrek timestamps often fall behind at peak.  If set, keep them fresh within reason
     */
    public void setFreshenTimestamps(boolean flag) {
        _freshenTimestamps = flag;
    }

    private boolean _preserveScheduleDeviation = true;
    /**
     * Carry over the last observation's schedule deviation when set
     */
    public void setPreserveScheduleDeviation(boolean flag) {
        _preserveScheduleDeviation = flag;
    }
    private boolean _enforceMatchingTripsForScheduleDeviation = true;
    /**
     * Only carry over schedule deviation when _preserveScheduleDeviation is set
     * AND observation trip match unassigned trip.
     *
     */
    public void setEnforceMatchingTripsForScheduleDeviation(boolean flag) {
        _enforceMatchingTripsForScheduleDeviation = flag;
    }

    private ArrayBlockingQueue<NycQueuedInferredLocationBean> _testingUnassignedQueue = new ArrayBlockingQueue<NycQueuedInferredLocationBean>(1000);

    @Autowired
    private TransitDataManagerApiLibrary _transitDataManagerApiLibrary = null;

    @Autowired
    private ThreadPoolTaskScheduler _taskScheduler;

    @Autowired
    private TransitGraphDao _transitGraphDao;

    private VehicleLocationInferenceService _vehicleLocationInferenceService;

    private ConfigurationService _configurationService;

    private OutputQueueSenderService _outputQueueSenderService;

    private ObjectMapper _mapper;

    private URL _url;

    Integer _maxActiveVehicleAgeSecs;

    private static final List<UnassignedVehicleRecord> EMPTY_RECORDS = Collections.unmodifiableList(new ArrayList<UnassignedVehicleRecord>());


    @Autowired
    public void setConfigurationService(ConfigurationService configurationService) {
        this._configurationService = configurationService;
        configChanged();
    }

    @Autowired
    public void setVehicleLocationInferenceService(VehicleLocationInferenceService vehicleLocationInferenceService){
        _vehicleLocationInferenceService = vehicleLocationInferenceService;
    }

    @Autowired
    public void setOutputQueueSenderService(OutputQueueSenderService outputQueueSenderService){
        _outputQueueSenderService = outputQueueSenderService;
    }

    public URL getUrl(){
        return _url;
    }

    @Override
    public void setUrl(URL url){
        _url = url;
    }

    public Integer getMaxActiveVehicleAgeSecs() {
        return _maxActiveVehicleAgeSecs;
    }

    public void setMaxActiveVehicleAgeSecs(Integer maxActiveVehicleAgeSecs) {
        _maxActiveVehicleAgeSecs = maxActiveVehicleAgeSecs;
    }

    @PostConstruct
    @Override
    public void setup(){
        setupMapper();
    }

    public void setupMapper() {
        _mapper = new ObjectMapper();
        _mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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


    @Refreshable(dependsOn = {"vtw.unassignedVehicleServiceUrl", "vtw.unassignedVehicleServiceEnabled", "vtw.unassignedVehicleServiceRefreshInterval", "vtw.maxActiveVehicleAgeSecs"})
    private void configChanged() {
        String url = _configurationService.getConfigurationValueAsString("vtw.unassignedVehicleServiceUrl", null);
        Integer updateIntervalSecs = _configurationService.getConfigurationValueAsInteger("vtw.unassignedVehicleServiceRefreshInterval", 30);
        boolean previousEnablement = _enabled;
        _enabled = Boolean.parseBoolean(_configurationService.getConfigurationValueAsString("vtw.unassignedVehicleServiceEnabled", "true"));
        _maxActiveVehicleAgeSecs = _configurationService.getConfigurationValueAsInteger("vtw.maxActiveVehicleAgeSecs", 120);

        try {
            setUrl(new URL(url));
        } catch (MalformedURLException e) {
            _log.error("Unable to reach unassigned vehicle service URL {}", url, e);
            _enabled = false;
        }

        if (previousEnablement && !_enabled && _updateTask != null) {
            _log.warn("shutting down per configuration!");
            _updateTask.cancel(true);
        }
        if (updateIntervalSecs != null) {
            setUpdateFrequency(updateIntervalSecs);
        }
    }


    @Override
    /**
     * take a raw bus record and convert to UnassignedRecord for testing purposes.
     */
    public void enqueueTestRecord(RealtimeEnvelope envelope) {
        final CcLocationReport message = envelope.getCcLocationReport();
        final CPTVehicleIden vehicleIdent = message.getVehicle();
        final AgencyAndId vehicleId = new AgencyAndId(
                vehicleIdent.getAgencydesignator(), vehicleIdent.getVehicleId()
                + "");

        VehicleInferenceInstance instance = _vehicleLocationInferenceService.getInstanceByVehicleId(vehicleId);
        if (instance != null) {
            NycQueuedInferredLocationBean state = instance.getCurrentStateAsNycQueuedInferredLocationBean();
            if (state != null) {
                state.setPhase(EVehiclePhase.SPOOKING.toLabel().toUpperCase());
                long observationTimestamp = state.getRecordTimestamp();
                // freshen the record so it isn't ignored
                state.setRecordTimestamp(System.currentTimeMillis());
                state.setDistanceAlongBlock(0.0);
                state.setDistanceAlongTrip(0.0);

                if (state.getVehicleId() == null) {
                    state.setVehicleId(vehicleId.getId());
                }
                try {
                    _log.info("queue spooking state for " + state.getVehicleId()
                            + " on trip " + state.getTripId()
                            + " with obs " + (System.currentTimeMillis() - observationTimestamp)/1000 +"s old");
                    _testingUnassignedQueue.offer(state, 1, TimeUnit.SECONDS);
                } catch (InterruptedException ie) {
                    // bury
                }
            }

        }

    }

    // package private for unit tests
    List<UnassignedVehicleRecord> getUnassignedVehicleRecordsDirect(URL url) {
        try {
            if (_mapper == null) {
                _log.error("mapper not present, still starting up?  ");
                return EMPTY_RECORDS;
            }
            Records records = _mapper.readValue(url, Records.class);
            if (records == null) return EMPTY_RECORDS;
            List<UnassignedVehicleRecord> output = new ArrayList<>();
            for (UnassignedVehicleRecord unassignedVehicleRecord : records.getUnassignedVehicleRecords()) {
                output.add(unassignedVehicleRecord);
            }
            return output;
        } catch (Exception e) {
            _log.error("Unable to retrieve unassigned vehicle records from {}", url, e);
            return EMPTY_RECORDS;
        }
    }

    @PreDestroy
    @Override
    public void destroy() {
        _log.info("destroy");
        if (_taskScheduler != null) {
            _taskScheduler.shutdown();
        }
    }

    private class UpdateThread extends TimerTask {
        @Override
        public void run() {
            try {
                if (!_debug) {
                    // the TDM proxies this info
                    processUnassignedVehicles(filterRecords(getUnassignedVehicleRecordsViaTDM()));
                } else {
                    // in debug mode we go direct to BusTrek
                    processUnassignedVehicles(filterRecords(getUnassignedVehicleRecordsDirect(getUrl())));
                }
            } catch (Exception e) {
                _log.error("refreshData() failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private List<UnassignedVehicleRecord> getUnassignedVehicleRecordsViaTDM() {
        try {
            List<JsonObject> operatorAssignments = _transitDataManagerApiLibrary.getItemsForRequest(
                    "ghostbus", "list");
            if (operatorAssignments == null) return EMPTY_RECORDS;
            List<UnassignedVehicleRecord> records = new ArrayList<>();
            for (JsonObject obj : operatorAssignments) {
                UnassignedVehicleRecord vr = toRecord(obj);
                if (vr != null) {
                    records.add(vr);
                }
            }
            return records;
        } catch (Exception any) {
            _log.warn("issue retrieving ghostbuses: " + any, any);
            return EMPTY_RECORDS;
        }
    }

    private UnassignedVehicleRecord toRecord(JsonObject obj) {
        UnassignedVehicleRecord vr = new UnassignedVehicleRecord();

        vr.setAgencyId(nullSafeGetAsString(obj.get("agency-id")));
        vr.setBlockId(nullSafeGetAsString(obj.get("inferred-block-id")));
        vr.setDistanceAlongBlock(nullSafeGetAsDouble(obj.get("distance-along-block")));
        vr.setLatitude(nullSafeGetAsDouble(obj.get("inferred-latitude")));
        vr.setLongitude(nullSafeGetAsDouble(obj.get("inferred-longitude")));
        vr.setPhase(nullSafeGetAsString(obj.get("inferred-phase")));
        String dateStr = obj.get("service-date").getAsString();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-DD");
            Date parse = sdf.parse(dateStr);
            vr.setServiceDate(parse.getTime());
        } catch (ParseException pe) {
            _log.info("invalid service date of " + dateStr);
        }
        vr.setStatus(nullSafeGetAsString(obj.get("inferred-status")));
        String trStr = obj.get("time-received").getAsString();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX'Z'");
            Date parse = sdf.parse(trStr);
            vr.setTimeReceived(parse.getTime());
        } catch (ParseException pe) {
            _log.info("invalid time received " + trStr);
        }

        vr.setTripId(nullSafeGetAsString(obj.get("inferred-trip-id")));
        vr.setVehicleId(nullSafeGetAsString(obj.get("vehicle-id")));
        if (obj.has("schedule-deviation")) {
            vr.setScheduleDeviation(nullSafeGetAsInt(obj.get("schedule-deviation")));
        }
        if (vr.getScheduleDeviation() == null) {
            vr.setScheduleDeviation(0); // default it
        }
        return vr;
    }

    private int nullSafeGetAsInt(JsonElement jsonElement) {
        if (jsonElement.isJsonNull()) return 0;
        if (jsonElement.isJsonPrimitive()) return jsonElement.getAsJsonPrimitive().getAsInt();
        if (jsonElement.isJsonObject()) return jsonElement.getAsInt();
        throw new IllegalStateException("unexpected element " + jsonElement);

    }

    private Double nullSafeGetAsDouble(JsonElement jsonElement) {
        if (jsonElement.isJsonNull()) return null;
        if (jsonElement.isJsonPrimitive()) return jsonElement.getAsJsonPrimitive().getAsDouble();
        if (jsonElement.isJsonObject()) return jsonElement.getAsDouble();
        throw new IllegalStateException("unexpected element " + jsonElement);
    }

    private String nullSafeGetAsString(JsonElement jsonElement) {
        if (jsonElement.isJsonNull()) return null;
        if (jsonElement.isJsonPrimitive()) return jsonElement.getAsJsonPrimitive().getAsString();
        if (jsonElement.isJsonObject()) return jsonElement.getAsString();
        throw new IllegalStateException("unexpected element " + jsonElement);
    }


    private List<UnassignedVehicleRecord> filterRecords(List<UnassignedVehicleRecord> unassignedVehicleRecords) {
        if (unassignedVehicleRecords == null) return EMPTY_RECORDS;
        if (!_debug) return unassignedVehicleRecords;
        // from here we are in debug mode -- create additional test records from testingUnassignedQueue
        List<UnassignedVehicleRecord> vehicleRecords = new ArrayList<>();
                vehicleRecords.addAll(unassignedVehicleRecords);
        List<UnassignedVehicleRecord> queuedRecords = dequeueTestUnassignedRecords();
        if (queuedRecords != null)
            vehicleRecords.addAll(queuedRecords);
        return vehicleRecords;
    }

    private List<UnassignedVehicleRecord> dequeueTestUnassignedRecords() {
        ArrayList<NycQueuedInferredLocationBean> records = new ArrayList<NycQueuedInferredLocationBean>();
        _testingUnassignedQueue.drainTo(records);
        return toRecords(records);

    }

    private List<UnassignedVehicleRecord> toRecords(ArrayList<NycQueuedInferredLocationBean> records) {
        List<UnassignedVehicleRecord> output = new ArrayList<>();
        if (records == null) return output;
        for (NycQueuedInferredLocationBean nyc : records) {
            UnassignedVehicleRecord r = new UnassignedVehicleRecord();
            AgencyAndId tripId = AgencyAndIdLibrary.convertFromString(nyc.getTripId());
            r.setAgencyId(tripId.getAgencyId());
            r.setBlockId(nyc.getBlockId());
            r.setDistanceAlongBlock(nyc.getDistanceAlongBlock());
            r.setLatitude(nyc.getInferredLatitude());
            r.setLongitude(nyc.getObservedLongitude());
            r.setPhase(nyc.getPhase());
            r.setServiceDate(nyc.getServiceDate());
            r.setStatus(nyc.getStatus());
            r.setTimeReceived(nyc.getRecordTimestamp());
            r.setTripId(nyc.getTripId());
            r.setVehicleId(nyc.getVehicleId());
            r.setScheduleDeviation(nyc.getScheduleDeviation());
            output.add(r);
        }
        return output;
    }

    private void processUnassignedVehicles(List<UnassignedVehicleRecord> unassignedVehicleRecords) {
        for(UnassignedVehicleRecord record : unassignedVehicleRecords){
            if(StringUtils.isNotBlank(record.getAgencyId()) && StringUtils.isNotBlank(record.getVehicleId())){
                NycQueuedInferredLocationBean inferredLocationBean = toNycQueueInferredLocationBean(record);
                _log.debug("considering unassigned v=" + record.getVehicleId());
                if (!isValid(inferredLocationBean)) {
                    _log.info("discarding update for vId=" + inferredLocationBean.getVehicleId()
                            + " with trip " + inferredLocationBean.getTripId());
                    continue;
                }
                AgencyAndId vehicleId = new AgencyAndId(record.getAgencyId(), record.getVehicleId());
                if(!vehicleActive(vehicleId, System.currentTimeMillis())) {
                    VehicleInferenceInstance instance = _vehicleLocationInferenceService.getInstanceByVehicleId(vehicleId);
                    NycQueuedInferredLocationBean ieBean = null;
                    if (instance != null) {
                         ieBean = instance.getCurrentStateAsNycQueuedInferredLocationBean();
                    }
                    /**
                     * get scheduleDeviation from most recent realtime update
                     * apply that to this unassigned update to minimize discrepancies between realtime and unassigned
                     * */
                    if (ieBean != null) {
                        if (_preserveScheduleDeviation && ieBean.getScheduleDeviation() != null) {
                            if (!_enforceMatchingTripsForScheduleDeviation
                                    || (ieBean.getTripId() != null
                                        && ieBean.getTripId().equals(record.getTripId()))) {
                                inferredLocationBean.setScheduleDeviation(ieBean.getScheduleDeviation());
                                _log.debug("set schDev for: " + vehicleId
                                        + " from obs " + (System.currentTimeMillis() - inferredLocationBean.getRecordTimestamp()) / 1000 + "s old");
                            } else {
                                // trips don't match or feature disabled, schedule deviation is meaningless
                                inferredLocationBean.setScheduleDeviation(0); //default
                                _log.debug("dropping schDev for: " + vehicleId
                                        + " from obs " + (System.currentTimeMillis() - inferredLocationBean.getRecordTimestamp()) / 1000
                                        + "s old as ie trip " + ieBean.getTripId() + " doesn't match " + record.getTripId()
                                        + " or preserveScheduleDeviation is false? " + _preserveScheduleDeviation);
                            }
                        }

                        if(ieBean.getDistanceAlongBlock() != null)
                            inferredLocationBean.setDistanceAlongBlock(ieBean.getDistanceAlongBlock());
                    } else {
                        // we don't have any state, default to 0 as these fields can't be empty
                        inferredLocationBean.setScheduleDeviation(0); //default to 0
                        inferredLocationBean.setDistanceAlongBlock(0.0);//placeholder
                    }

                    long now = System.currentTimeMillis();
                    long obs = inferredLocationBean.getRecordTimestamp();
                    if (_freshenTimestamps && now - obs > LATENT_WINDOW_START_SECONDS && now - obs < LATENT_WINDOW_END_SECONDS) {
                        // BusTrek can fall behind -- freshen timestamps within reason
                        _log.debug("freshening timestamp for " + vehicleId + " that was " + (now - obs)/1000 + "s old");
                        inferredLocationBean.setRecordTimestamp(now);
                    }

                    _log.info("vId=" + inferredLocationBean.getVehicleId()
                            + " on trip " + inferredLocationBean.getTripId()
                            + " with schDev " + inferredLocationBean.getScheduleDeviation()
                            + " that is " + (System.currentTimeMillis() - inferredLocationBean.getRecordTimestamp())/1000 + "s old");
                    _outputQueueSenderService.enqueue(inferredLocationBean);

                } else {
                    Long timeReceived = _vehicleLocationInferenceService.getTimeReceivedByVehicleId(vehicleId);
                    Integer schDev = null;
                    VehicleInferenceInstance instance = _vehicleLocationInferenceService.getInstanceByVehicleId(vehicleId);
                    if (instance != null) {
                        schDev = instance.getCurrentStateAsNycQueuedInferredLocationBean().getScheduleDeviation();
                    }
                    if (timeReceived != null) {
                        _log.info("CONFLICT Vehicle with id {} marked as unassigned but has been recently updated at {}s with max of {}s and schDev {}",
                                vehicleId,
                                ((System.currentTimeMillis() - timeReceived) / 1000),
                                getMaxActiveVehicleAgeSecs(),
                                schDev);
                    }
                }
            } else {
                _log.debug("missing vehicleId for record");
            }
        }
    }

    private boolean isValid(NycQueuedInferredLocationBean bean) {
        String tripId = bean.getTripId();
        // integration with an external system -- sometimes they will disagree on tripIds
        TripEntry tripEntryForId = _transitGraphDao.getTripEntryForId(AgencyAndId.convertFromString(tripId));
        if (tripEntryForId == null) {
            _log.debug("unmatched trip = " + tripId);
            return false;
        }
        return true;
    }

    public boolean vehicleActive(AgencyAndId vehicleId, long currentTime) {
        if (_vehicleLocationInferenceService.getTimeReceivedByVehicleId(vehicleId) ==  null) return false;

        Long timeReceived = _vehicleLocationInferenceService.getTimeReceivedByVehicleId(vehicleId);
        if(timeReceived != null && ((currentTime - timeReceived) / 1000) <= getMaxActiveVehicleAgeSecs()){
            return true;
        }
        return false;
    }

    public NycQueuedInferredLocationBean toNycQueueInferredLocationBean(UnassignedVehicleRecord record){
        NycQueuedInferredLocationBean inferredLocationBean = new NycQueuedInferredLocationBean();
        AgencyAndId vehicleId = new AgencyAndId(record.getAgencyId(), record.getVehicleId());
        inferredLocationBean.setVehicleId(AgencyAndId.convertToString(vehicleId));
        inferredLocationBean.setRecordTimestamp(record.getTimeReceived());
        inferredLocationBean.setTripId(record.getTripId());
        inferredLocationBean.setServiceDate(record.getServiceDate());
        inferredLocationBean.setInferredLatitude(record.getLatitude());
        inferredLocationBean.setInferredLongitude(record.getLongitude());
        inferredLocationBean.setPhase(record.getPhase());
        inferredLocationBean.setStatus(record.getStatus());
        return inferredLocationBean;
    }

}
