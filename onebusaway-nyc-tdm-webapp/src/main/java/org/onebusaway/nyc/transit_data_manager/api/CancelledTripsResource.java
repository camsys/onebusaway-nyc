package org.onebusaway.nyc.transit_data_manager.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.onebusaway.nyc.transit_data.model.NycCancelledTripBean;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.datatype.joda.JodaModule;

import javax.annotation.PostConstruct;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Path("/cancelled-trips")
@Component
@Scope("singleton")
public class CancelledTripsResource {

    /**
     * pulls in data from Cancelledtrips API, generates beans, and pushes to queue
     *
     * @author caylasavitzky
     *
     */


    public static final int DEFAULT_REFRESH_INTERVAL = 15 * 1000;
    public static final int DEFAULT_CONNECTION_TIMEOUT = 5 * 1000;
    public static final int DEFAULT_QUEUE_PORT = 5577;
    public static final String DEFAULT_QUEUE_LOCATION = "";

    private static Logger _log = LoggerFactory.getLogger(CancelledTripsResource.class);

    private ThreadPoolTaskScheduler _taskScheduler;
    @Autowired
    private ConfigurationService _configurationService;

    // make sure we only initialize once
    private static boolean _initialized = false;
    // keep track of if we've logged the initialization
    private static boolean _initializedLogged = false;

    // this needs to be static as Spring creates a new instance on invocation
    private static StringBuffer _response = new StringBuffer();

    private int _refreshIntervalMillis = DEFAULT_REFRESH_INTERVAL;
    private int _connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
    private int _queuePort = DEFAULT_QUEUE_PORT;
    private String _queueLocation = DEFAULT_QUEUE_LOCATION;
    private List<NycCancelledTripBean> _cancelledTrips = null;
    private ObjectMapper _mapper;

    @Path("/list")
    @GET
    @Produces("application/json")
    public Response getCancelledTripsList() throws Exception {
        StringWriter writer = null;
        String output = null;
        try {
            writer = new StringWriter();
            if (_cancelledTrips == null)
                _cancelledTrips = new ArrayList<>();
            _mapper.writeValue(writer, _cancelledTrips);
            output = writer.toString();
        } catch (IOException e) {
            _log.error("exception parsing json " + e, e);
        } finally {
            try {
                writer.close();
            } catch (IOException e) {}
        }

        Response response = Response.ok(output).build();

        _log.info("Returning response ok for url=" + _url);

        return response;
    }


    private String _url = null;

    protected ConfigurationService getConfig() {
        return _configurationService;
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
    public void setConfig(ConfigurationService config) {
        _configurationService = config;
    }

    @Autowired
    public void setTaskScheduler(ThreadPoolTaskScheduler scheduler) {
        _taskScheduler = scheduler;
    }

    @PostConstruct
    public void setup() {
        try {
            if (!_initialized && _taskScheduler != null) {
                _log.info("setting up...");
                setupObjectMapper();
                // move configuration to background thread to allow TDM to startup
                final ConfigThread configThread = new ConfigThread(this);
                _taskScheduler.scheduleWithFixedDelay(configThread, 2 * 60 * 1000);

                final UpdateThread updateThread = new UpdateThread(this);
                if (_taskScheduler != null) {
                    _taskScheduler.scheduleWithFixedDelay(updateThread, _refreshIntervalMillis);
                }

            }
        } catch (Throwable t) {
            _log.error("exception setting up cancelledTripResource: "+ t, t);
        }
    }

    protected void setupObjectMapper(){
        _mapper = new ObjectMapper();

        _mapper.registerModule(new JodaModule());
        _mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        _mapper.setTimeZone(Calendar.getInstance().getTimeZone());
    }

    public InputStream getCancelledTripsData(){
        long start = System.currentTimeMillis();
        HttpURLConnection connection = null;
        if (getUrl() == null) {
            _log.warn("Cancelled trips url not configured, exiting");
            return null;
        }
        try {
            connection = (HttpURLConnection) new URL(_url).openConnection();
            connection.setConnectTimeout(_connectionTimeout);
            connection.setReadTimeout(_connectionTimeout);
            return connection.getInputStream();
        } catch (Exception any) {
            _log.error("issue retrieving " + getUrl() + ", " + any.toString());
            return null;
        }
    }

    public List<NycCancelledTripBean> makeCancelledTripBeansFromCapiOutput(InputStream input) throws IOException {
        _log.debug("reading from stream...");
        try {
            IncomingNycCancelledTripBeansContainer beansContainer = _mapper.readValue(input, IncomingNycCancelledTripBeansContainer.class);
            if (beansContainer != null && beansContainer.getBeans() != null) {
                _log.debug("parsed " + beansContainer.getBeans().size() + " records");
            } else {
                _log.debug("empty beanContainer");
            }
            return beansContainer.getBeans();
        } catch (Exception any) {
            _log.error("issue parsing json: " + any, any);
        }
        return null;
    }

    public void setCancelledTripsBeans(List<NycCancelledTripBean> beans){
        _cancelledTrips = beans;
    }

    public void update() {
        try {
            InputStream input = getCancelledTripsData();
            setCancelledTripsBeans(makeCancelledTripBeansFromCapiOutput(input));
        } catch (Exception e) {
            // bury
        }
    }

    public static class ConfigThread implements Runnable {
        private CancelledTripsResource _resource;
        public ConfigThread(CancelledTripsResource resource) {
            this._resource = resource;
        }

        public void run() {
            if (_resource.getConfig() == null) {
                _log.warn("missing config service, bailing");
                return;
            }
            String url = _resource.getConfig().getConfigurationValueAsString("tdm.CAPIUrl", null);
            Integer refreshInterval = _resource.getConfig().getConfigurationValueAsInteger( "tdm.CAPIRefreshInterval", 30*1000);
            Integer connectionTimeout = _resource.getConfig().getConfigurationValueAsInteger("tdm.CAPIConnectionTimeout", 1000);
            if (url != null) {
                if (refreshInterval != null) {
                    _resource.setRefreshIntervalMillis(refreshInterval);
                }
                if (connectionTimeout != null) {
                    _resource.setConnectionTimeout(connectionTimeout);
                }

                if (_resource.getUrl() != null && !_resource.getUrl().equals(url)) {
                    _log.info("tdm.CAPIUrl set to " + url);
                }
                _resource.setUrl(url);
                _initialized = true;
            } else {
                _resource.setUrl(null);
                if (!_initializedLogged) {
                    // only log CAPI disabled once to keep the logs tidy
                    _log.warn("tdm.CAPIUrl not set, capi disabled");
                    _initializedLogged = true;
                }
                _initialized = false;
            }
        }
    }

    public static class UpdateThread implements Runnable {

        private CancelledTripsResource _resource;
        public UpdateThread(CancelledTripsResource resource) {
            this._resource = resource;
        }

        @Override
        public void run() {

            if (_resource._initialized) {
                _log.info("refreshing...");
                _resource.update();
                _log.info("refresh complete");
            } else {
                _log.info("disabled refresh");
                _resource.setCancelledTripsBeans(new ArrayList<>());
            }
        }
    }
}