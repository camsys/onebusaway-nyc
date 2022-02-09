package org.onebusaway.nyc.transit_data_manager.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.onebusaway.container.refresh.Refreshable;
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

    private static Logger _log = LoggerFactory.getLogger(CancelledTripsResource.class);

    private ThreadPoolTaskScheduler _taskScheduler;
    @Autowired
    private ConfigurationService _configurationService;

    private int _connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
    private List<NycCancelledTripBean> _cancelledTrips = null;
    private ObjectMapper _mapper;
    private UpdateThread updateThread;
    private String _url = null;


    protected ConfigurationService getConfig() {
        return _configurationService;
    }
    public void setConnectionTimeout(int timeout) {
        _connectionTimeout = timeout;
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
        setupObjectMapper();
        if (_taskScheduler != null) {
            ConfigThread configThread = new ConfigThread(this);
            _taskScheduler.scheduleWithFixedDelay(configThread, 60 * 1000);
        }
    }

    @Refreshable(dependsOn = {"tdm.CAPIUrl", "tdm.CAPIConnectionTimeout", "tdm.CAPIRefreshInterval"})
    protected void refreshConfig() {
        _url = getConfig().getConfigurationValueAsString("tdm.CAPIUrl", null);
        Integer refreshInterval = getConfig().getConfigurationValueAsInteger( "tdm.CAPIRefreshInterval", DEFAULT_REFRESH_INTERVAL);
        _connectionTimeout = getConfig().getConfigurationValueAsInteger("tdm.CAPIConnectionTimeout", 1000);
        if (_url != null && updateThread == null) {
            updateThread = new UpdateThread(this);
            if (_taskScheduler != null) {
                _log.info("confiuring update thread now");
                _taskScheduler.scheduleWithFixedDelay(updateThread, refreshInterval);
            }
        }
    }

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

        _log.debug("Returning response ok for url=" + _url);

        return response;
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
            _log.error("exception updating: " + e, e);
        }
    }


    public static class ConfigThread implements Runnable {

        private CancelledTripsResource resource;
        public ConfigThread(CancelledTripsResource resource) {
            this.resource = resource;
        }

        @Override
        public void run() {
            resource.refreshConfig();
        }
    }

    public static class UpdateThread implements Runnable {

        private CancelledTripsResource resource;
        public UpdateThread(CancelledTripsResource resource) {
            this.resource = resource;
        }

        @Override
        public void run() {

            if (resource.getUrl() != null) {
                _log.debug("refreshing...");
                resource.update();
                _log.debug("refresh complete");
            } else {
                _log.debug("disabled refresh");
                resource.setCancelledTripsBeans(new ArrayList<>());
            }
        }
    }
}