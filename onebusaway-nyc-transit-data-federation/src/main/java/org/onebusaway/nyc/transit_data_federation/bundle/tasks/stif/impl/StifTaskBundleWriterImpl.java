package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.bundle.model.NycFederatedTransitDataBundle;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.NonRevenueStopData;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.StifTrip;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.GeographyRecord;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.ServiceCode;
import org.onebusaway.nyc.transit_data_federation.model.nyc.RunData;
import org.onebusaway.utility.ObjectSerializationLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class StifTaskBundleWriterImpl {

	private Logger _log = LoggerFactory.getLogger(StifTaskBundleWriterImpl.class);
	
	@Autowired 
	private NycFederatedTransitDataBundle _bundle;

	public void storeTripRunData(StifLoaderImpl loader){
		//store trip run mapping in bundle
		Map<AgencyAndId, RunData> runsForTrip = loader.getRunsForTrip();
		try {
			if (_bundle != null) // for unit tests
				ObjectSerializationLibrary.writeObject(_bundle.getTripRunDataPath(), runsForTrip);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

		// non revenue moves
		serializeNonRevenueMoveData(loader.getRawStifData(), loader.getGeographyRecordsByBoxId());

		// non revenue stops
		serializeNonRevenueStopData(loader.getNonRevenueStopDataByTripId());
	}

	private void serializeNonRevenueMoveData(Map<ServiceCode, List<StifTrip>> nonRevenueMovesByServiceCode, 
			Map<AgencyAndId, GeographyRecord> nonRevenueMoveLocationsByBoxId) {
		try {
			if (_bundle != null) { // for unit tests
				ObjectSerializationLibrary.writeObject(_bundle.getNonRevenueMovePath(), 
						nonRevenueMovesByServiceCode);
				ObjectSerializationLibrary.writeObject(_bundle.getNonRevenueMoveLocationsPath(), 
						nonRevenueMoveLocationsByBoxId);
			}
		} catch (IOException e) {
			throw new IllegalStateException("error serializing non-revenue move/STIF data", e);
		}	  
	}

	private void serializeNonRevenueStopData(Map<AgencyAndId, List<NonRevenueStopData>> nonRevenueStopDataByTripId) {
		try {
			if (_bundle != null) // for unit tests
				ObjectSerializationLibrary.writeObject(_bundle.getNonRevenueStopsPath(), 
						nonRevenueStopDataByTripId);
		} catch (IOException e) {
			throw new IllegalStateException("error serializing non-revenue move/STIF data", e);
		}
	}
	
	public void serializeDSCData(Map<String, List<AgencyAndId>> dscToTripMap,
			Map<AgencyAndId, String> tripToDscMap, Set<String> inServiceDscs, Set<String> notInServiceDscs) {
		for (String notInServiceDsc : notInServiceDscs) {
			if (inServiceDscs.contains(notInServiceDsc))
				_log.warn("overlap between in-service and not-in-service dscs: "
						+ notInServiceDsc);

			// clear out trip mappings for out of service DSCs
			dscToTripMap.put(notInServiceDsc, new ArrayList<AgencyAndId>());
		}

		try {
			if (_bundle != null) { // for unit tests
				ObjectSerializationLibrary.writeObject(_bundle.getNotInServiceDSCs(), 
						notInServiceDscs);

				ObjectSerializationLibrary.writeObject(_bundle.getTripsForDSCIndex(), 
						tripToDscMap);

				ObjectSerializationLibrary.writeObject(_bundle.getDSCForTripIndex(),
						dscToTripMap);
			}
		} catch (IOException e) {
			throw new IllegalStateException("error serializing DSC/STIF data", e);
		}
	}

}
