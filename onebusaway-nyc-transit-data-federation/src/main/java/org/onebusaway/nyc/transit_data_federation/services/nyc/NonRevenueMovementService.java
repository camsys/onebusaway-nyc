package org.onebusaway.nyc.transit_data_federation.services.nyc;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.model.nyc.NonRevenueMoveData;

public interface NonRevenueMovementService {
	  
	/**
	 * Given a block ID and service date and a time, find what phase the bus is supposed to be in given the STIF schedule. 
	 * 
	 * Results can be a NonRevenueMoveData structure tagged with PULLIN, PULLOUT or DEADHEAD--if the bus is in a revenue state, null is returned.
	 */
	public NonRevenueMoveData findNonRevenueMovementsForBlockAndTime(AgencyAndId blockId, long blockServiceDate, long time);

}
