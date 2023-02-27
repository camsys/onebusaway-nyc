package org.onebusaway.nyc.transit_data_federation.impl.queue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.transit_data.model.trips.CancelledTripBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class HTTPListenerTask{


    protected static Logger _log = LoggerFactory.getLogger(HTTPListenerTask.class);

    public static final int DEFAULT_REFRESH_INTERVAL = 15 * 1000;

    private ThreadPoolTaskScheduler _taskScheduler;

    @Autowired
    public void setTaskScheduler(ThreadPoolTaskScheduler scheduler) {
        _taskScheduler = scheduler;
    }

    private ConfigurationService _configurationService;

    public ConfigurationService getConfig() {
        return _configurationService;
    }

    @Autowired
    public void setConfig(ConfigurationService config) {
        _configurationService = config;
    }


    private Integer _refreshInterval;


    private HTTPListenerTask.UpdateThread updateThread;

    private Boolean _isEnabled;

    @PostConstruct
    public void setup(){
        refreshConfig();
        createConfigThread();
        createUpdateThread();
    }

    abstract Integer getRefreshInterval();
    abstract boolean getIsEnabled();


    protected void refreshConfig() {
        _refreshInterval = getRefreshInterval();
        _isEnabled =  getIsEnabled();
    }

    private void createConfigThread(){
        if(_taskScheduler != null) {
            HTTPListenerTask.ConfigThread configThread = new HTTPListenerTask.ConfigThread(this);
            _taskScheduler.scheduleWithFixedDelay(configThread, 60 * 1000);
        } else {
            _log.warn("Unable to create config thread, task scheduler unavailable");
        }
    }

    public boolean allowDataCollection() {
        if (!isEnabled()){
            _log.warn(this.getClass() + " HTTPListener is not enabled in configuration");
            return false;
        }
        if (getLocation() == null){
            _log.warn(this.getClass() + " HTTPListener url is not defined");
            return false;
        }
        return true;
    }

    private void createUpdateThread(){
        if (allowDataCollection() && updateThread == null && _taskScheduler != null) {
            updateThread = new HTTPListenerTask.UpdateThread(this);
            _log.info("configuring update thread now");
            _taskScheduler.scheduleWithFixedDelay(updateThread, _refreshInterval);
        }
    }

    protected void refreshUpdateThreadPostConfig(){
        // Only creates if does not already exist
        createUpdateThread();
    }

    public Boolean isEnabled() {
        return _isEnabled;
    }

    abstract String getLocation();


    abstract void updateData();

    public static class UpdateThread implements Runnable {

        private HTTPListenerTask resource;

        public UpdateThread(HTTPListenerTask resource) {
            this.resource = resource;
        }

        @Override
        public void run() {
            if (resource.allowDataCollection()) {
                _log.debug("refreshing...");
                resource.updateData();
                _log.debug("refresh complete");
            } else {
                _log.debug("disabled refresh");
            }
        }
    }

    public static class ConfigThread implements Runnable {
        private HTTPListenerTask resource;
        public ConfigThread(HTTPListenerTask resource) {
            this.resource = resource;
        }
        @Override
        public void run() {
            resource.refreshConfig();
            resource.refreshUpdateThreadPostConfig();
        }
    }
}
