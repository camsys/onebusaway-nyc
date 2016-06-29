package org.onebusaway.nyc.transit_data_federation.services.vtw;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.onebusaway.gtfs.model.AgencyAndId;

import tcip_final_4_0_0.ObaSchPullOutList;
import tcip_final_4_0_0.SCHPullInOutInfo;

/**
 * Service interface for getting pullout info for a vehicle
 * 
 * @author dhaskin
 */
public interface VehiclePulloutService {

  /**
   * Get most recent pullout for given vehicle.
   * 
   * @param vehicleId The vehicle identifier to request pullout for.
   */
  public SCHPullInOutInfo getVehiclePullout(AgencyAndId vehicle);

  public String getAssignedBlockId(AgencyAndId vehicleId);
  
  public ObaSchPullOutList getFromXml(String xml) throws XMLStreamException, JAXBException;
  
  public String getAsXml(ObaSchPullOutList o) throws JAXBException;
}
