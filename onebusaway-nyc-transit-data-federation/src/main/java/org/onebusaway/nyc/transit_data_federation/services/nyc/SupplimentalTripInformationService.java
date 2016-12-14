package org.onebusaway.nyc.transit_data_federation.services.nyc;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.model.nyc.SupplimentalTripInformation;

/**
 * Get additional non-customer information for a trip. 
 * Includes Direction, Trip Type, and Bus Type 
 * @author laidig
 *
 */
public interface SupplimentalTripInformationService {
	public SupplimentalTripInformation getSupplimentalTripInformation(String tripId);
	public SupplimentalTripInformation getSupplimentalTripInformation(AgencyAndId tripId);
}
