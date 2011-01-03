package org.onebusaway.nyc.vehicle_tracking.services;

import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;

public interface DestinationSignCodeService {

  public List<AgencyAndId> getTripIdsForDestinationSignCode(
      String destinationSignCode);

  public String getDestinationSignCodeForTripId(AgencyAndId tripId);

  public boolean isOutOfServiceDestinationSignCode(String destinationSignCode);
  
  public boolean isMissingDestinationSignCode(String destinationSignCode);
  
  public boolean isUnknownDestinationSignCode(String destinationSignCode);
}
