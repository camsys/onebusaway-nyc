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
                ListBean<VehicleStatusBean>  vehicles = _transitDataService.getAllVehiclesForAgency(agencyId, System.currentTimeMillis());
                for (VehicleStatusBean vehicle : vehicles.getList()) {
                    String vehicleId = vehicle.getVehicleId();
                    String routeId = vehicle.getTrip().getRoute().getId();
                    String directionId = vehicle.getTrip().getDirectionId();

                    VehicleOccupancyRecord vor = _transitDataService.getVehicleOccupancyRecordForVehicleIdAndRoute(
                            AgencyAndIdLibrary.convertFromString(vehicleId), routeId, directionId);

                    if (vor == null) {
                        _log.warn("No occupancy record found for vehicle {} on route {}.", vehicleId, routeId);
                        continue;
                    }

                    OccupancyStatus currentStatus = vor.getOccupancyStatus();
                    OccupancyStatus calculatedStatus = _apcLoadLevelCalculator.determineOccupancyStatus(
                            vor.getRawCount(), vor.getCapacity(), AgencyAndIdLibrary.convertFromString(vor.getRouteId()));

                    if (currentStatus != calculatedStatus) {
                        _log.info("Updating occupancy status for vehicle {}: current status {}, calculated status {}",
                                vehicleId, currentStatus, calculatedStatus);
                        _log.debug("Vehicle ID: {}, Count: {}, Max Occupancy: {}, Route ID: {}, Express Status: {}",
                                vehicleId, vor.getRawCount(), vor.getCapacity(), routeId, _nycRouteTypeService.isRouteExpress(AgencyAndIdLibrary.convertFromString(vor.getRouteId())));

                        vor.setOccupancyStatus(calculatedStatus);
                        _transitDataService.addVehicleOccupancyRecord(vor);
                    }
                }
            }
        } catch (Exception e) {
            _log.error("Error during APC verification task", e);
        }
    }
}
