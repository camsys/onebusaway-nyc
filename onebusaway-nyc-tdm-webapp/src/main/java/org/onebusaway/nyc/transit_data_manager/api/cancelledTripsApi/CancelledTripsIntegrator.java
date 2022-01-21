package org.onebusaway.nyc.transit_data_manager.api.cancelledTripsApi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.onebusaway.nyc.transit_data.model.NycCancelledTripBean;
import org.onebusaway.nyc.transit_data_manager.config.ConfigurationDatastoreInterface;
import org.onebusaway.nyc.transit_data_manager.config.model.jaxb.ConfigItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.datatype.joda.JodaModule;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

@Component
public class CancelledTripsIntegrator {

    /**
     * pulls in data from Cancelledtrips API, generates beans, and pushes to queue
     *
     * @author caylasavitzky
     *
     */


//    todo: finish setup configs & update configs source

    public static final int DEFAULT_CONNECTION_TIMEOUT = 5 * 1000;
    public static final int DEFAULT_REFRESH_INTERVAL = 15 * 1000;

    private static Logger _log = LoggerFactory.getLogger(CancelledTripsIntegrator.class);

    private ThreadPoolTaskScheduler _taskScheduler;
    @Autowired
    private ConfigurationDatastoreInterface _config;

    // make sure we only initialize once
    private static boolean _initialized = false;

    private int _refreshIntervalMillis = DEFAULT_REFRESH_INTERVAL;
    private int _connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;

    private CancelledTripsOutputQueueSenderServiceImpl _outputQueueSenderService;

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

    public StringBuffer getCancelledTripsData(){
        long start = System.currentTimeMillis();
        StringBuffer sb = new StringBuffer();
        HttpURLConnection connection = null;
        if (getUrl() == null) {
            _log.warn("Cancelled trips url not configured, exiting");
            return null;
        }
        try {
            connection = (HttpURLConnection) new URL(_url).openConnection();
            connection.setConnectTimeout(_connectionTimeout);
            connection.setReadTimeout(_connectionTimeout);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(connection.getInputStream(), baos);

            sb.append(baos.toString());
            _log.debug("retrieved " + getUrl() + " in " + (System.currentTimeMillis() - start) + " ms");
            return sb;
        } catch (Exception any) {
            _log.error("issue retrieving " + getUrl() + ", " + any.toString());
            return null;
        }
    }

    public List<NycCancelledTripBean>  makeCancelledTripBeansFromCapiOutput(StringBuffer buffer) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        objectMapper.setTimeZone(Calendar.getInstance().getTimeZone());
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
        private CancelledTripsIntegrator _resource;
        public InitThread(CancelledTripsIntegrator resource) {
            this._resource = resource;
        }

        public void run() {
        }
    }

    public static class UpdateThread implements Runnable {

        private CancelledTripsIntegrator _resource;
        public UpdateThread(CancelledTripsIntegrator resource) {
            this._resource = resource;
        }

        @Override
        public void run() {
            try {
                StringBuffer cancelledTripData = _resource.getCancelledTripsData();
                List<NycCancelledTripBean>  cancelledTripBeans = _resource.makeCancelledTripBeansFromCapiOutput(cancelledTripData);
                _resource.enqueueBeans(cancelledTripBeans);
            } catch (Exception e) {
                // bury
            }
        }
    }
}