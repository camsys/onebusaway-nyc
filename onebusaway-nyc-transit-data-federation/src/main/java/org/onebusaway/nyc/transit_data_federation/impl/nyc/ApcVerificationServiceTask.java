package org.onebusaway.nyc.transit_data_federation.impl.nyc;

import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.realtime.api.VehicleOccupancyRecord;
import org.onebusaway.transit_data.model.AgencyWithCoverageBean;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.realtime.api.OccupancyStatus;
import org.onebusaway.util.AgencyAndIdLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ApcVerificationServiceTask implements Runnable {

    private static final Logger _log = LoggerFactory.getLogger(ApcVerificationServiceTask.class);

    @Autowired
    private NycTransitDataService _transitDataService;

    @Autowired
    private ApcLoadLevelCalculator _apcLoadLevelCalculator;

    @Autowired
    private NycRouteTypeService _nycRouteTypeService;

    @Autowired
    private ThreadPoolTaskScheduler _taskScheduler;

    @PostConstruct
    public void setup() {
        _taskScheduler.scheduleWithFixedDelay(this, 5000);
    }

    @Override
    public void run() {
        try {
            List<AgencyWithCoverageBean> agenciesWithCoverage = _transitDataService.getAgenciesWithCoverage();
            List<String> agencyIds = agenciesWithCoverage.stream()
                    .map((agency) -> agency.getAgency().getId()).collect(Collectors.toList());
            for (String agencyId : agencyIds) {
                int vehiclesVerified = 0;
                int vehiclesWithoutRoute = 0;
                int vehiclesWithoutOccupancy = 0;
                int correctedVehicles = 0;
                int vorsWhichCouldHaveRouteAdded = 0;
                ListBean<VehicleStatusBean>  vehicles = _transitDataService.getAllVehiclesForAgency(agencyId, System.currentTimeMillis());
                for (VehicleStatusBean vehicle : vehicles.getList()) {
                    String vehicleId = vehicle.getVehicleId();
                    if (vehicle.getTrip() == null || vehicle.getTrip().getRoute() == null) {
                        _log.trace("Unable to verify occupancy information because vehicle {} has no trip or route information.", vehicleId);
                        vehiclesWithoutRoute++;
                        continue;
                    }
                    String routeId = vehicle.getTrip().getRoute().getId();
                    String directionId = vehicle.getTrip().getDirectionId();

                    VehicleOccupancyRecord vor = _transitDataService.getVehicleOccupancyRecordForVehicleIdAndRoute(
                            AgencyAndIdLibrary.convertFromString(vehicleId), routeId, directionId);

                    if (vor == null) {
                        vor = _transitDataService.getLastVehicleOccupancyRecordForVehicleId(
                                AgencyAndIdLibrary.convertFromString(vehicleId));
                        if(vor == null) {
                            _log.trace("No occupancy record found for vehicle {} on route {}.", vehicleId, routeId);
                            vehiclesWithoutOccupancy++;
                            continue;
                        } else{
                            _log.debug("Using last occupancy record for vehicle {}: {}", vehicleId, vor);
                            vorsWhichCouldHaveRouteAdded++;
                        }
                    }

                    OccupancyStatus currentStatus = vor.getOccupancyStatus();
                    OccupancyStatus calculatedStatus = _apcLoadLevelCalculator.determineOccupancyStatus(
                            vor.getRawCount(), vor.getCapacity(), AgencyAndIdLibrary.convertFromString(routeId));
                    vehiclesVerified++;
                    if (currentStatus != calculatedStatus) {

                        if(_log.isDebugEnabled()){
                            _log.warn("Updating occupancy status for vehicle {}: current status {}, calculated status {}, Count: {}, Max Occupancy: {}, Route ID: {}, Express Status: {}",
                                    vehicleId, currentStatus, calculatedStatus, vor.getRawCount(), vor.getCapacity(), routeId, _nycRouteTypeService.isRouteExpress(AgencyAndIdLibrary.convertFromString(vor.getRouteId())));
                        }
                        correctedVehicles++;
                        vor.setOccupancyStatus(calculatedStatus);
                        _transitDataService.addVehicleOccupancyRecord(vor);
                    }
                }
                _log.debug("APC verification task completed for agency {}: verified {} vehicles,  {} corrected, {} without occupancy info, {} vehicles without route info",
                        agencyId, vehiclesVerified, correctedVehicles, vehiclesWithoutOccupancy, vehiclesWithoutRoute);
                if (vorsWhichCouldHaveRouteAdded > 0) {
                    _log.debug("{} vehicle occupancy records could have had route information added but did not. This may indicate a data issue, as those vors are only half discoverable.",
                            vorsWhichCouldHaveRouteAdded);
                }
            }
        } catch (Exception e) {
            _log.error("Error during APC verification task", e);
        }
    }
}
