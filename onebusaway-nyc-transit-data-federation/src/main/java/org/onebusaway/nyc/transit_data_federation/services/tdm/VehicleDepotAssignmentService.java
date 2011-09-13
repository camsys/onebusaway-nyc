package org.onebusaway.nyc.transit_data_federation.services.tdm;

import java.util.ArrayList;

import org.onebusaway.gtfs.model.AgencyAndId;

/**
 * Service interface for getting which vehicles are assigned to a depot.
 * 
 * @author jmaki
 */
public interface VehicleDepotAssignmentService {

  /**
   * Get a list of vehicles assigned to the named depot.
   * 
   * @param depotIdentifier The depot identifier to request vehicles for.
   */
  public ArrayList<AgencyAndId> getAssignedVehicleIdsForDepot(String depotIdentifier);

}
