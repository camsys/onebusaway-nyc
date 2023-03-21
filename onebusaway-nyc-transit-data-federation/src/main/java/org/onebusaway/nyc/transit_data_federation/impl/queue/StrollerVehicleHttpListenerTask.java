package org.onebusaway.nyc.transit_data_federation.impl.queue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data_federation.services.StrollerVehicleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class StrollerVehicleHttpListenerTask extends HTTPListenerTask{


    protected static Logger _log = LoggerFactory.getLogger(StrollerVehicleHttpListenerTask.class);
    public static final int DEFAULT_REFRESH_INTERVAL = 15 * 60 * 1000;
    public static final String DEFAULT_STROLLER_URL = "http://tdm.dev.obanyc.com/api/stroller/list";


    public static final int DEFAULT_CONNECTION_TIMEOUT = 5 * 1000;
    public int _connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
    public String _strollerUrl = DEFAULT_STROLLER_URL;

    public void setConnectionTimeout(int timeout) {
        _connectionTimeout = timeout;
    }

    @Override
    boolean getIsEnabled() {
        return getConfig().getConfigurationValueAsBoolean("tds.enableStrollerVehicles", Boolean.FALSE);
    }
    @Override
    Integer getRefreshInterval() {
        return getConfig().getConfigurationValueAsInteger( "tds.strollerVehiclesRefreshInterval", DEFAULT_REFRESH_INTERVAL);
    }

    @Override
    @Refreshable(dependsOn = {"tds.strollerVehiclesUrl","tds.strollerVehiclesRefreshInterval","tds.enableStrollerVehicles"})
    protected void refreshConfig() {
        _strollerUrl = getConfig().getConfigurationValueAsString("tds.strollerVehiclesUrl", _strollerUrl);
        super.refreshConfig();    }

    @Override
    public String getLocation() {
        return _strollerUrl;
    }

    private NycTransitDataService _tds;
    @Autowired
    public void setNycTransitDataService(NycTransitDataService tds){
        _tds = tds;
    }

    private StrollerVehicleService _strollerVehicleService;
    @Autowired
    public void setStrollerVehicleService(StrollerVehicleService strollerVehicleService){
        _strollerVehicleService = strollerVehicleService;
    }

    private ObjectMapper _objectMapper = new ObjectMapper();

    @Override
    public void updateData()  {
        try {
            InputStream inputStream = getInputData();
            Set<AgencyAndId> cancelledTripsSet = convertStrollerVehiclesSet(inputStream);
            _strollerVehicleService.updateStrollerVehicles(cancelledTripsSet);
        } catch (Exception e){
            _log.error("Unable to process cancelled trip beans from {}", getLocation());
        }
    }

    private InputStream getInputData(){
        HttpURLConnection connection;
        if (getLocation() == null) {
            _log.warn("StrollerVehicle url not configured, exiting");
            return null;
        }
        try {
            connection = (HttpURLConnection) new URL(getLocation()).openConnection();
            connection.setConnectTimeout(_connectionTimeout);
            connection.setReadTimeout(_connectionTimeout);
            return connection.getInputStream();
        } catch (Exception any) {
            _log.error("issue retrieving " + getLocation() + ", " + any.toString());
            return null;
        }
    }

    private Set<AgencyAndId> convertStrollerVehiclesSet(InputStream input) {
        try {
            Set<AgencyAndId> list = _objectMapper.readValue(input,
                    new TypeReference<Set<AgencyAndId>>() {});
            return list;
        } catch (Exception any) {
            _log.error("issue parsing json: " + any, any);
        }
        return Collections.EMPTY_SET;
    }


}
