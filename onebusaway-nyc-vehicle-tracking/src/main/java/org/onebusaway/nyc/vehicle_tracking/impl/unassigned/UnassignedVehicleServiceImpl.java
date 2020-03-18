package org.onebusaway.nyc.vehicle_tracking.impl.unassigned;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.VehicleInferenceInstance;
import org.onebusaway.nyc.vehicle_tracking.model.unassigned.Records;
import org.onebusaway.nyc.vehicle_tracking.model.unassigned.UnassignedVehicleRecord;
import org.onebusaway.nyc.vehicle_tracking.services.inference.VehicleLocationInferenceService;
import org.onebusaway.nyc.vehicle_tracking.services.queue.OutputQueueSenderService;
import org.onebusaway.nyc.vehicle_tracking.services.unassigned.UnassignedVehicleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ScheduledFuture;

@Component
public class UnassignedVehicleServiceImpl implements UnassignedVehicleService {

    private static Logger _log = LoggerFactory.getLogger(UnassignedVehicleServiceImpl.class);

    private ScheduledFuture<?> _updateTask = null;

    private boolean _enabled;

    @Autowired
    private ThreadPoolTaskScheduler _taskScheduler;

    private VehicleLocationInferenceService _vehicleLocationInferenceService;

    private ConfigurationService _configurationService;

    private OutputQueueSenderService _outputQueueSenderService;

    private ObjectMapper _mapper;

    private URL _url;

    Integer _maxActiveVehicleAgeSecs;

    private static final UnassignedVehicleRecord[] EMPTY_RECORDS = {};


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
        _enabled = Boolean.parseBoolean(_configurationService.getConfigurationValueAsString("vtw.unassignedVehicleServiceEnabled", "false"));
        _maxActiveVehicleAgeSecs = _configurationService.getConfigurationValueAsInteger("vtw.maxActiveVehicleAgeSecs", 120);

        try {
            setUrl(new URL(url));
        } catch (MalformedURLException e) {
            _log.error("Unable to reach unassigned vehicle service URL {}", url, e);
            _enabled = false;
        }

        if (updateIntervalSecs != null) {
            setUpdateFrequency(updateIntervalSecs);
        }
    }


    public UnassignedVehicleRecord[] getUnassignedVehicleRecords(URL url) {
        try {
            Records records = _mapper.readValue(url, Records.class);
            return records.getUnassignedVehicleRecords();
        } catch (Exception e) {
            _log.error("Unable to retrive unassigned vehicle records from {}", url, e);
            return EMPTY_RECORDS;
        }
    }

    @PreDestroy
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
                UnassignedVehicleRecord[] unassignedVehicleRecords = getUnassignedVehicleRecords(getUrl());
                processUnassignedVehicles(unassignedVehicleRecords);
            } catch (Exception e) {
                _log.error("refreshData() failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void processUnassignedVehicles(UnassignedVehicleRecord[] unassignedVehicleRecords) {
        for(UnassignedVehicleRecord record : unassignedVehicleRecords){
            if(StringUtils.isNotBlank(record.getAgencyId()) && StringUtils.isNotBlank(record.getVehicleId())){
                NycQueuedInferredLocationBean inferredLocationBean = toNycQueueInferredLocationBean(record);
                AgencyAndId vehicleId = new AgencyAndId(record.getAgencyId(), record.getVehicleId());
                if(!vehicleActive(vehicleId, System.currentTimeMillis())) {

//                    if ((System.currentTimeMillis()/60000)%5 == 0) {
//                        inferredLocationBean.setScheduleDeviation(-500);
//                        inferredLocationBean.setPhase("IN_PROGRESS");
//                        inferredLocationBean.setDistanceAlongBlock(1000.0);
//                        _vehicleLocationInferenceService.putInstance(vehicleId);
//                        _outputQueueSenderService.enqueue(inferredLocationBean);
//                        _log.info("send spoof of: " + inferredLocationBean.getVehicleId());
//                        continue;
//                    }




                    VehicleInferenceInstance instance = _vehicleLocationInferenceService.getInstanceByVehicleId(vehicleId);
                    /* *
                     * get scheduleDeviation from most recent realtime update
                     * apply that to this unassigned update to minimize discrepancies between realtime and unassigned
                     * */
                    try {
                        inferredLocationBean.setScheduleDeviation((int)instance.getCurrentState().getActualScheduleDeviation());
//                        _log.info("set actual for: " + vehicleId);
                    } catch (Exception e) {
                        try {
                            inferredLocationBean.setScheduleDeviation((int)instance.getCurrentState().getInferredScheduleDeviation());
//                            _log.info("set inferred for: " + vehicleId);
                        } catch (Exception f){
                            inferredLocationBean.setScheduleDeviation(0); //default to 0
//                            _log.info("Did not apply ScheduleDeviation to inferredLocationBean for vehicle: " + inferredLocationBean.getVehicleId());
                        }
                    }
                    try {
                        if(inferredLocationBean.getDistanceAlongBlock() > instance.getCurrentState().getActualDistanceAlongBlock())
                            inferredLocationBean.setDistanceAlongBlock(instance.getCurrentState().getActualDistanceAlongBlock());
                    } catch (Exception e) {
                        try {
                            if(inferredLocationBean.getDistanceAlongBlock() > instance.getCurrentState().getInferredDistanceAlongBlock())
                                inferredLocationBean.setDistanceAlongBlock(instance.getCurrentState().getInferredDistanceAlongBlock());
                        } catch (Exception f) {
                            inferredLocationBean.setDistanceAlongBlock(0.0);//placeholder
                        }
                    }
                    _outputQueueSenderService.enqueue(inferredLocationBean);

                } else {
                    _log.info("CONFLICT Vehicle with id {} marked as unassigned but has been recently updated.", vehicleId);
                }

//                if ((System.currentTimeMillis()/60000)%5 == 0) {
//                    NycQueuedInferredLocationBean spoof = inferredLocationBean;
//                    spoof.setPhase("SPOOF");
//                    spoof.setScheduleDeviation(-500);
//                    try {
//                        VehicleInferenceInstance instance = _vehicleLocationInferenceService.getInstanceByVehicleId(new AgencyAndId(record.getAgencyId(), record.getVehicleId()));
//
//                        spoof.setRecordTimestamp(inferredLocationBean.getRecordTimestamp()-60000);
//                        spoof.setDistanceAlongBlock(0.0); //placeholder
//                        spoof.setDistanceAlongBlock(instance.getCurrentState().getActualDistanceAlongBlock());
//                        spoof.setDistanceAlongBlock(instance.getCurrentState().getInferredDistanceAlongBlock());
//                    } catch (Exception e) {
//
//                    }
//                    _outputQueueSenderService.enqueue(spoof);
//                    _log.info("SPOOFING " + spoof.getVehicleId() + " sd: " + spoof.getScheduleDeviation());
//                }
            }
        }
    }

    public boolean vehicleActive(AgencyAndId vehicleId, long currentTime) {
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
