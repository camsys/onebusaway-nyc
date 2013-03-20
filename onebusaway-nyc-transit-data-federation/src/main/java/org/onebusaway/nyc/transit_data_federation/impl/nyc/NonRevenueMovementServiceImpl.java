package org.onebusaway.nyc.transit_data_federation.impl.nyc;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.bundle.model.NycFederatedTransitDataBundle;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.StifTrip;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.StifTripType;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.GeographyRecord;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.ServiceCode;
import org.onebusaway.nyc.transit_data_federation.impl.bundle.NycRefreshableResources;
import org.onebusaway.nyc.transit_data_federation.model.nyc.NonRevenueMoveData;
import org.onebusaway.nyc.transit_data_federation.services.nyc.NonRevenueMovementService;
import org.onebusaway.utility.ObjectSerializationLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.vividsolutions.jts.geom.Coordinate;

@Component
public class NonRevenueMovementServiceImpl implements NonRevenueMovementService {

	private Logger _log = LoggerFactory.getLogger(NonRevenueMovementServiceImpl.class);

	@Autowired
	private NycFederatedTransitDataBundle _bundle;
	
	// these keyed by service code, NOT ID, and actually include revenue moves from the bundle
	protected Map<ServiceCode, List<StifTrip>> _nonRevenueMovesByServiceCode = null;

	protected Map<AgencyAndId, GeographyRecord> _nonRevenueMoveLocationsByBoxId = null;
	
	private Multimap<AgencyAndId, NonRevenueMoveData> _nonRevenueMovesByBlockId = HashMultimap.create();
			
	@Autowired
	public void setBundle(NycFederatedTransitDataBundle bundle) {
		_bundle = bundle;
	}

	@PostConstruct
	@Refreshable(dependsOn = NycRefreshableResources.NON_REVENUE_MOVES_DATA)
	public void setup() throws IOException, ClassNotFoundException {
		File path = _bundle.getNonRevenueMovePath();
		if (path.exists()) {
			_log.info("loading non-revenue moves data");
			_nonRevenueMovesByServiceCode = ObjectSerializationLibrary.readObject(path);
		} else
			return;

		path = _bundle.getNonRevenueMoveLocationsPath();
		if (path.exists()) {
			_log.info("loading non-revenue move locations data");
			_nonRevenueMoveLocationsByBoxId = ObjectSerializationLibrary.readObject(path);
		} else
			return;

		buildIndices();
	}

	public void buildIndices() {
		for(ServiceCode sc : _nonRevenueMovesByServiceCode.keySet()) {
			List<StifTrip> stifTrips = _nonRevenueMovesByServiceCode.get(sc);
			for(StifTrip trip : stifTrips) {
				if(trip.type == StifTripType.REVENUE)
					continue;

				AgencyAndId blockId = new AgencyAndId(trip.agencyId, trip.blockId);
				GeographyRecord fromLocation = null;
				if(trip.firstStop != null)
					fromLocation = _nonRevenueMoveLocationsByBoxId.get(new AgencyAndId(trip.agencyId, trip.firstStop));
				GeographyRecord toLocation = null;
				if(trip.lastStop != null)
					toLocation = _nonRevenueMoveLocationsByBoxId.get(new AgencyAndId(trip.agencyId, trip.lastStop));
								
				NonRevenueMoveData nrmd = new NonRevenueMoveData();
				nrmd.setMoveType(trip.type);
				nrmd.setDepotCode(trip.depot);
				nrmd.setRunId(new AgencyAndId(trip.agencyId, trip.runId));
				if(fromLocation != null)
					nrmd.setStartLocation(new Coordinate(fromLocation.getLatitude(), fromLocation.getLongitude()));
				if(toLocation != null)
					nrmd.setEndLocation(new Coordinate(toLocation.getLatitude(), toLocation.getLongitude()));
				nrmd.setStartTime(trip.firstStopTime);				
				nrmd.setEndTime(trip.lastStopTime);
				_nonRevenueMovesByBlockId.put(blockId, nrmd);
			}
		}
	}

	@Override
	public NonRevenueMoveData findNonRevenueMovementsForBlockAndTime(AgencyAndId blockId, long blockServiceDate, long time) {
		long secondsPastMidnight = (time - blockServiceDate) / 1000;
		for(NonRevenueMoveData nrmd : _nonRevenueMovesByBlockId.get(blockId)) {
			if(nrmd.getStartTime() <= secondsPastMidnight && secondsPastMidnight <= nrmd.getEndTime()) {
				return nrmd;
			}
		}
		
		return null;
	}
}
