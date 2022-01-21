package org.onebusaway.nyc.transit_data_manager.api.canceledTripsApi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.onebusaway.nyc.transit_data.model.NycCancelledTripBean;
import org.onebusaway.nyc.transit_data_manager.config.ConfigurationDatastoreInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.List;

@Component
public class CanceledTripsIntegrator {

    /**
     * pulls in data from Cancledtrips API, generates beans, and pushes to queue
     *
     * @author caylasavitzky
     *
     */


//    todo: finish setup configs & update configs source

    public static final int DEFAULT_CONNECTION_TIMEOUT = 5 * 1000;
    public static final int DEFAULT_REFRESH_INTERVAL = 15 * 1000;

    private static Logger _log = LoggerFactory.getLogger(CanceledTripsIntegrator.class);

    private ThreadPoolTaskScheduler _taskScheduler;
    @Autowired
    private ConfigurationDatastoreInterface _config;

    // make sure we only initialize once
    private static boolean _initialized = false;

    private int _refreshIntervalMillis = DEFAULT_REFRESH_INTERVAL;
    private int _connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;

    private CanceledTripsOutputQueueSenderServiceImpl _outputQueueSenderService;

    private String _url = null;

    protected ConfigurationDatastoreInterface getConfig() {
        return _config;
    }
    public void setConnectionTimeout(int timeout) {
        _connectionTimeout = timeout;
    }
    public void setRefreshIntervalMillis(int refresh) {
        _refreshIntervalMillis = refresh;
    }
    public int getRefreshIntervalMillis() {
        return _refreshIntervalMillis;
    }
    public void setUrl(String url) {
        _url = url;
    }
    public String getUrl() {
        return _url;
    }
    @Autowired
    public void setTaskScheduler(ThreadPoolTaskScheduler scheduler) {
        _taskScheduler = scheduler;
    }

    @PostConstruct
    public void setup() {
        if (!_initialized) {
            _log.info("setting up...");
            // move configuration to background thread to allow TDM to startup
            final InitThread initThread = new InitThread(this);
            new Thread(initThread).start();
            _initialized = true;
        }
    }

    public StringBuffer getCanceledTripsData(){
        return new StringBuffer();
    }

    public List<NycCancelledTripBean>  makeCancelledTripBeansFromCapiOutput(StringBuffer buffer) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
        objectMapper.setDateFormat(df);
        NycCancelledTripBeansContainer beansContainer = objectMapper.readValue(buffer.toString(), new TypeReference<NycCancelledTripBeansContainer>(){});
        return beansContainer.getBeans();
    }

    public void enqueueBeans(List<NycCancelledTripBean> beans){
    }


    public void completeInitialization(String url){
        final UpdateThread updateThread = new UpdateThread(this);
        _taskScheduler.scheduleWithFixedDelay(updateThread, _refreshIntervalMillis);
    }

    public static class InitThread implements Runnable {
        private CanceledTripsIntegrator _resource;
        public InitThread(CanceledTripsIntegrator resource) {
            this._resource = resource;
        }

        public void run() {
        }
    }

    public static class UpdateThread implements Runnable {

        private CanceledTripsIntegrator _resource;
        public UpdateThread(CanceledTripsIntegrator resource) {
            this._resource = resource;
        }

        @Override
        public void run() {
            try {
                StringBuffer cancelledTripData = _resource.getCanceledTripsData();
                List<NycCancelledTripBean>  canceledTripBeans = _resource.makeCancelledTripBeansFromCapiOutput(cancelledTripData);
                _resource.enqueueBeans(canceledTripBeans);
            } catch (Exception e) {
                // bury
            }
        }
    }
}