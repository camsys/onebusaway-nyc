package org.onebusaway.nyc.transit_data_federation.impl.queue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.services.capi.CapiDao;
import org.onebusaway.transit_data.model.trips.CancelledTripBean;
import org.onebusaway.transit_data_federation.services.CancelledTripService;
import org.onebusaway.transit_data_federation.services.KneelingVehicleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class KneelingBusHttpListenerTask extends HTTPListenerTask{

    protected static Logger _log = LoggerFactory.getLogger(KneelingBusHttpListenerTask.class);
    public static final int DEFAULT_REFRESH_INTERVAL = 15 * 60 * 1000;
    public static final String DEFAULT_KNEELING_URL = "";

    @Override
    boolean getIsEnabled() {
        return getConfig().getConfigurationValueAsBoolean("tds.enableKneeling", Boolean.FALSE);
    }
    @Override
    Integer getRefreshInterval() {
        return getConfig().getConfigurationValueAsInteger( "tds.kneelingRefreshInterval", DEFAULT_REFRESH_INTERVAL);
    }

    @Override
    @Refreshable(dependsOn = {"tds.enableKneeling","tds.kneelingRefreshInterval"})
    protected void refreshConfig() {
        super.refreshConfig();    }

    @Override
    public String getLocation() {
        return "";
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

    private ObjectMapper _objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setDateFormat(new SimpleDateFormat("yyyy-MM-dd"))
            .setTimeZone(Calendar.getInstance().getTimeZone());

    @Override
    public void updateData()  {
        try {
            InputStream inputStream = getInputData();
            Set<AgencyAndId> cancelledTripsSet = convertKneelingVehiclesSet(inputStream);
            cancelledTripsSet = filterForValidVehicles(cancelledTripsSet);
            _kneelingVehicleService.updateKneelingVehicles(cancelledTripsSet);
        } catch (Exception e){
            _log.error("Unable to process cancelled trip beans from {}", getLocation());
        }
    }

    private InputStream getInputData(){
        //todo: get an input stream
        return null;
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

    private Set<AgencyAndId> filterForValidVehicles(Set<AgencyAndId> kneelingVehicles){
        return kneelingVehicles.stream().filter((AgencyAndId vehicleId) -> isValidKneelingVehicle(vehicleId)).collect(Collectors.toSet());
    }

    private boolean isValidKneelingVehicle(AgencyAndId kneelingVehicle) {
        try {
            Long vehicleAgency = Long.parseLong(kneelingVehicle.getAgencyId());
            if (_tds.getVehicleForAgency(kneelingVehicle.getId(),vehicleAgency) == null) {
                _log.debug("unknown Vehicle= " + kneelingVehicle);
                return false;
            } else {
                return true;
            }
        } catch (ServiceException e) {
            _log.warn("Error retrieving kneelingVehicle", e);
        }
        return false;
    }
}
