package org.onebusaway.nyc.presentation.impl;

import org.onebusaway.nyc.presentation.service.ConfigurationBean;
import org.onebusaway.nyc.presentation.service.NycConfigurationService;
import org.onebusaway.presentation.model.ArrivalDepartureBeanListFilter;
import org.onebusaway.transit_data.model.ArrivalAndDepartureBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ArrivalDepartureBeanListFilterImpl implements
    ArrivalDepartureBeanListFilter {

  private boolean debug = false;

  @Autowired
  private NycConfigurationService configurationService;

  @Override
  public List<ArrivalAndDepartureBean> filter(
      List<ArrivalAndDepartureBean> beans) {

    ArrayList<ArrivalAndDepartureBean> output = new ArrayList<ArrivalAndDepartureBean>();
    for (ArrivalAndDepartureBean arrivalAndDepartureBean : beans) {
      TripBean tripBean = arrivalAndDepartureBean.getTrip();
      TripStatusBean tripStatusBean = arrivalAndDepartureBean.getTripStatus();
      String headsign = tripBean.getTripHeadsign();
      String routeId = tripBean.getRoute().getId();
      String directionId = tripBean.getDirectionId();

      if (routeId == null || headsign == null || directionId == null) {
        if (debug) {
          System.out.println("missing one of route, headsign, or direction "
              + arrivalAndDepartureBean.getVehicleId());
        }
        continue;
      }

      // hide non-realtime arrivals and departures
      if (tripStatusBean == null || tripStatusBean.isPredicted() == false
          || tripStatusBean.getVehicleId() == null
          || tripStatusBean.getVehicleId().equals("")) {
        if (debug) {
          System.out.println("missing status, predicted, or vehicle id"
              + arrivalAndDepartureBean.getVehicleId());
        }
        continue;
      }

      // hide buses that left the stop recently
      if (arrivalAndDepartureBean.getDistanceFromStop() < 0) {
        if (debug) {
          System.out.println("skip buses that just left this stop "
              + arrivalAndDepartureBean.getVehicleId());
        }
        continue;
      } else {
      // or ones that are farther away than the entire route's length
		if(tripStatusBean != null) {
			if(arrivalAndDepartureBean.getDistanceFromStop() > tripStatusBean.getDistanceAlongTrip()) {
		        if (debug) {
		            System.out.println("skip buses that are farther away than the entire route "
		                + arrivalAndDepartureBean.getVehicleId());
		        }
				continue;
			}
		}    	  
      }
      
      if (tripBean != null && tripStatusBean != null) {
        String phase = tripStatusBean.getPhase();
        TripBean activeTrip = tripStatusBean.getActiveTrip();

        /* if vehicle is in a layover */
        if (phase != null
            && (phase.toLowerCase().equals("layover_before") || phase.toLowerCase().equals(
                "layover_during"))) {

          double distanceAlongTrip = tripStatusBean.getDistanceAlongTrip();
          double totalDistanceAlongTrip = tripStatusBean.getTotalDistanceAlongTrip();
          if (Double.isNaN(distanceAlongTrip) != true
              && Double.isNaN(totalDistanceAlongTrip) != true) {
            double ratio = distanceAlongTrip / totalDistanceAlongTrip;
            if (activeTrip != null
                && !tripBean.getId().equals(activeTrip.getId())
                && ((arrivalAndDepartureBean.getBlockTripSequence() - 1) != tripStatusBean.getBlockTripSequence() && ratio > 0.50)) {
              if (debug) {
                System.out.println("some nonsense about ratio"
                    + arrivalAndDepartureBean.getVehicleId());
              }
              continue;
            }
          }
        } else {
          if (activeTrip != null
              && !tripBean.getId().equals(activeTrip.getId())) {
            if (debug) {
              System.out.println("the princess is on another trip id"
                  + tripBean.getId() + " bus: "
                  + arrivalAndDepartureBean.getVehicleId());
            }
            continue;
          }
        }
      }

      if (Double.isNaN(tripStatusBean.getDistanceAlongTrip())) {
        if (debug) {
          System.out.println("the distance along trip is nan"
              + arrivalAndDepartureBean.getVehicleId());
          continue;
        }
      }

      String status = tripStatusBean.getStatus();
      String phase = tripStatusBean.getPhase();

      // hide disabled vehicles (row 7)
      if (status != null && status.toLowerCase().compareTo("disabled") == 0) {
        if (debug) {
          System.out.println("the bus is disabled"
              + arrivalAndDepartureBean.getVehicleId());
        }
        continue;
      }

      // hide deadheading vehicles (except within a block) (row 3)
      // hide vehicles at the depot (row 1)
      if (phase != null && phase.toLowerCase().compareTo("in_progress") != 0
          && phase.toLowerCase().compareTo("layover_before") != 0
          && phase.toLowerCase().compareTo("layover_during") != 0) {
        if (debug) {
          System.out.println("the bus is deadheading"
              + arrivalAndDepartureBean.getVehicleId());
        }
        continue;
      }

      // hide data >= (hide timeout) minutes old (row 5)
      ConfigurationBean config = configurationService.getConfiguration();

      if (new Date().getTime() - tripStatusBean.getLastUpdateTime() >= 1000 * config.getHideTimeout()) {
        if (debug) {
          System.out.println("the bus has passed the timeout"
              + arrivalAndDepartureBean.getVehicleId());
        }
        continue;
      }
      output.add(arrivalAndDepartureBean);

    }
    return output;
  }

  public void setDebug(boolean debug) {
    this.debug = debug;
  }

  public boolean getDebug() {
    return debug;
  }
}
