package org.onebusaway.nyc.transit_data_manager.api.config;

import com.sun.jersey.api.spring.Autowire;
import org.apache.commons.io.IOUtils;
import org.onebusaway.nyc.transit_data_manager.config.ConfigurationDatastoreInterface;
import org.onebusaway.nyc.transit_data_manager.config.model.jaxb.ConfigItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Proxy GhostBus API.
 * Currently this does no validation.
 */
@Path("/ghostbus")
@Component
@Autowire
@Scope("singleton")
public class GhostBusResource {
    public static final int DEFAULT_CONNECTION_TIMEOUT = 5 * 1000;
    public static final int DEFAULT_REFRESH_INTERVAL = 15 * 1000;
    private static final String oldPreamble = "{\"records\":[";
    private static final String newPreamble = "{\"status\":\"OK\",\"message-text\":null, \"records\":[";
    private static Logger _log = LoggerFactory.getLogger(GhostBusResource.class);

    private ThreadPoolTaskScheduler _taskScheduler;
    @Autowired
    private ConfigurationDatastoreInterface _config;

    // make sure we only initialize once
    private static boolean _initialized = false;

    // this needs to be static as Spring creates a new instance on invocation
    private static StringBuffer _response = new StringBuffer();

    private int _refreshIntervalMillis = DEFAULT_REFRESH_INTERVAL;
    private int _connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;

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

    @Path("/list")
    @GET
    @Produces("application/json")
    public String getGhostBusList() throws Exception {
        if (_response != null)
            return addStatus(_response.toString());
        // return a valid preamble so as to confirm empty message
        return "{\"status\":\"OK\",\"message-text\":null, \"config\":[]}";
    }

    private String addStatus(String content) {
        return content.replace(oldPreamble, newPreamble);

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

    // setup recurring thread once we have proper endpoint configured
    protected void completeInitialization(String url) {
        setUrl(url);
        _log.info("GhostBusResource configured to " + url);

        final UpdateThread updateThread = new UpdateThread(this);
        _taskScheduler.scheduleWithFixedDelay(updateThread, 60 * 1000);
    }

    private StringBuffer getGhostBuses() throws Exception {
        long start = System.currentTimeMillis();
        StringBuffer sb = new StringBuffer();
        HttpURLConnection connection = null;
        if (getUrl() == null) {
            _log.warn("ghost buses not configured, exiting");
            return sb;
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

    protected void setResponse(StringBuffer response) {
        _response = response;
    }

    public static class InitThread implements Runnable {
        private GhostBusResource _resource;
        public InitThread(GhostBusResource resource) {
            this._resource = resource;
        }

        public void run() {
            if (_resource.getConfig() == null) {
                _log.warn("missing config service, bailing");
                return;
            }
            int nTries = 20;
            int tries = 0;
            while (tries < nTries) {
                tries++;
                ConfigItem item = _resource.getConfig().getConfigItemByComponentKey("vtw", "vtw.unassignedVehicleServiceUrl");
                if (item != null) {
                    _resource.completeInitialization(item.getValue());
                    return;
                }
                try {
                    _log.info("Sleeping on configuration");
                    Thread.sleep(30 * 1000);
                } catch (InterruptedException ie) {
                    return;
                }
            }
            _log.error("giving up on configuration!  GhostBus API will not be available");
        }
    }

    public static class UpdateThread implements Runnable {

        private GhostBusResource _resource;
        public UpdateThread(GhostBusResource resource) {
            this._resource = resource;
        }

        @Override
        public void run() {
            try {
                StringBuffer serverResponse = _resource.getGhostBuses();
                _resource.setResponse(serverResponse);
            } catch (Exception e) {
                // bury
            }
        }
    }
}
