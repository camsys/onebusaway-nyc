package org.onebusaway.nyc.gtfsrt.impl;

import org.onebusaway.nyc.gtfsrt.service.TripDetailsService;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripForVehicleQueryBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class TripDetailsServiceImpl implements TripDetailsService {
    private TransitDataService _transitDataService;

    @Autowired
    @Qualifier("nycTransitDataServiceImpl")
    public void setTransitDataService(TransitDataService service) {
        _transitDataService = service;
    }

    @Override
    public TripDetailsBean getTripDetailsForVehicleStatus(VehicleStatusBean bean) {
        // potentially could cache this for performance reasons if necessary
        TripForVehicleQueryBean query = new TripForVehicleQueryBean();
        query.setVehicleId(bean.getVehicleId());
        //query.setTime(new Date(bean.getTimeOfLocationUpdate()));
        return _transitDataService.getTripDetailsForVehicleAndTime(query);
    }

}
