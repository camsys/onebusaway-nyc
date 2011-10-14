package org.onebusaway.nyc.transit_data_federation.services.tdm;

import java.util.ArrayList;

import org.onebusaway.gtfs.model.AgencyAndId;

/**
 * Service interface for getting which vehicles are assigned to a depot and vice-versa.
 * 
 * @author jmaki
 * @author dhaskin
 */
public interface VehicleAssignmentService {

  /**
   * Get a list of vehicles assigned to the named depot.
   * 
   * @param depotIdentifier The depot identifier to request vehicles for.
   */
  public ArrayList<AgencyAndId> getAssignedVehicleIdsForDepot(String depotIdentifier)
    throws Exception;

  /**
   * Get depot for the given vehicleId, assuming the depot has been previously requested
   * with getAssignedVehicleIdsForDepot().
   * 
   * @param vehicleId The vehicle identifier to request depot for.
   */
  public String getAssignedDepotForVehicleId(AgencyAndId vehicle);

}
