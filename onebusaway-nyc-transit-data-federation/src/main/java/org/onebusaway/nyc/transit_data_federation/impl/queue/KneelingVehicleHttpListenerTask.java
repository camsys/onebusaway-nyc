package org.onebusaway.nyc.transit_data_federation.impl.queue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data_federation.services.KneelingVehicleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class KneelingVehicleHttpListenerTask extends HTTPListenerTask{


    protected static Logger _log = LoggerFactory.getLogger(KneelingVehicleHttpListenerTask.class);
    public static final int DEFAULT_REFRESH_INTERVAL = 15 * 60 * 1000;
    public static final String DEFAULT_KNEELING_URL = "http://tdm.dev.obanyc.com/api/kneeling/list";


    public static final int DEFAULT_CONNECTION_TIMEOUT = 5 * 1000;
    public int _connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
    public String _kneelingUrl = DEFAULT_KNEELING_URL;

    public void setConnectionTimeout(int timeout) {
        _connectionTimeout = timeout;
    }

    @Override
    boolean getIsEnabled() {
        return getConfig().getConfigurationValueAsBoolean("tds.enableKneelingVehicles", Boolean.FALSE);
    }
    @Override
    Integer getRefreshInterval() {
        return getConfig().getConfigurationValueAsInteger( "tds.kneelingVehiclesRefreshInterval", DEFAULT_REFRESH_INTERVAL);
    }

    @Override
    @Refreshable(dependsOn = {"tds.kneelingVehiclesUrl","tds.kneelingVehiclesRefreshInterval","tds.enableKneelingVehicles"})
    protected void refreshConfig() {
        _kneelingUrl = getConfig().getConfigurationValueAsString("tds.kneelingVehiclesUrl", _kneelingUrl);
        super.refreshConfig();    }

    @Override
    public String getLocation() {
        return _kneelingUrl;
    }

    private NycTransitDataService _tds;
    @Autowired
    public void setNycTransitDataService(NycTransitDataService tds){
        _tds = tds;
    }

    private KneelingVehicleService _kneelingVehicleService;
    @Autowired
    public void setKneelingVehicleService(KneelingVehicleService kneelingVehicleService){
        _kneelingVehicleService = kneelingVehicleService;
    }

    private ObjectMapper _objectMapper = new ObjectMapper();

    @Override
    public void updateData()  {
        try {
            InputStream inputStream = getInputData();
            Set<AgencyAndId> cancelledTripsSet = convertKneelingVehiclesSet(inputStream);
            _kneelingVehicleService.updateKneelingVehicles(cancelledTripsSet);
        } catch (Exception e){
            _log.error("Unable to process cancelled trip beans from {}", getLocation());
        }
    }

    private InputStream getInputData(){
        HttpURLConnection connection;
        if (getLocation() == null) {
            _log.warn("KneelingVehicle url not configured, exiting");
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

    private Set<AgencyAndId> convertKneelingVehiclesSet(InputStream input) {
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
