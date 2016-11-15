/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.impl;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.impl.AbnormalStifDataLoggerImpl;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.impl.DSCOverrideHandler;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.impl.DSCServiceManager;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.impl.StifAggregatorImpl;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.impl.StifLoaderImpl;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.impl.StifTaskBundleWriterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Load STIF data, including the mapping between destination sign codes and trip
 * ids, into the database
 * 
 * @author bdferris, Philip Matuskiewicz
 * 
 */
public class StifTaskControllerImpl implements Runnable {

	
	private StifLoaderImpl _stifLoader;
	private StifAggregatorImpl _stifAggregator;
	private StifTaskBundleWriterImpl _stifBundleWriter;
	private DSCOverrideHandler _dscOverrideHandler;
	private AbnormalStifDataLoggerImpl _abnormalDataLogger;
	private DSCServiceManager _dscSvcMgr;
	
	public void set_stifLoader(StifLoaderImpl _stifLoader) {
		this._stifLoader = _stifLoader;
	}

	public void set_stifAggregator(StifAggregatorImpl _stifAggregator) {
		this._stifAggregator = _stifAggregator;
	}

	public void set_stifBundleWriter(StifTaskBundleWriterImpl _stifBundleWriter) {
		this._stifBundleWriter = _stifBundleWriter;
	}

	public void set_dscOverrideHandler(DSCOverrideHandler _dscOverrideHandler) {
		this._dscOverrideHandler = _dscOverrideHandler;
	}

	public void set_abnormalDataLogger(AbnormalStifDataLoggerImpl _abnormalDataLogger) {
		this._abnormalDataLogger = _abnormalDataLogger;
	}

	public void set_dscSvcMgr(DSCServiceManager _dscSvcMgr) {
		this._dscSvcMgr = _dscSvcMgr;
	}

	

	private Logger _log = LoggerFactory.getLogger(StifTaskControllerImpl.class);


	private boolean fallBackToStifBlocks = false;
	public void setFallBackToStifBlocks(boolean fallBackToStifBlocks) {
		this.fallBackToStifBlocks = fallBackToStifBlocks;
	}

	/**
	 * The path of the directory containing STIF files to process
	 */
	private List<File> _stifPaths = new ArrayList<File>();
	public void setStifPath(File path) {
		_stifPaths.add(path);
	}

	public void setStifPaths(List<File> paths) {
		_stifPaths.addAll(paths);
	}


	public void run() {
		
		_stifLoader.load(_stifPaths);
		_stifAggregator.setStifLoader(_stifLoader);
		_stifAggregator.computeBlocksFromRuns();
		_stifLoader.warnOnMissingTrips();

		if (fallBackToStifBlocks) {
			_stifLoader.loadStifBlocks();
		}

		//bundletodo
		_stifBundleWriter.storeTripRunData(_stifLoader);

		// dsc to trip map
		Map<String, List<AgencyAndId>> dscToTripMap = _stifLoader.getTripMapping();

		// Read in trip to dsc overrides if they exist
		dscToTripMap = _dscOverrideHandler.handleOverride(_stifLoader, dscToTripMap);

		Map<AgencyAndId, String> tripToDscMap = createTripToDSCMapping(dscToTripMap);

		Set<String> inServiceDscs = new HashSet<String>();
		_abnormalDataLogger.logDSCStatistics(dscToTripMap, tripToDscMap, _stifAggregator.getRoutesByDSC());

		int withoutMatch = _stifLoader.getTripsWithoutMatchCount();
		int total = _stifLoader.getTripsCount();

		_log.info("stif trips without match: " + withoutMatch + " / " + total);

		_dscSvcMgr.readNotInServiceDscs();
		_stifBundleWriter.serializeDSCData(dscToTripMap, tripToDscMap, inServiceDscs, _dscSvcMgr.getNotInServiceDSCs());
		_stifBundleWriter.serializeSupplimentalTripInformation(_stifAggregator.getSupplimentalTripInfo());
	}


	public Map<AgencyAndId, String> createTripToDSCMapping(Map<String, List<AgencyAndId>> dscToTripMap){
		Map<AgencyAndId, String> tripToDscMap = new HashMap<AgencyAndId, String>();
		// Populate tripToDscMap based on dscToTripMap
		for (Map.Entry<String, List<AgencyAndId>> entry : dscToTripMap.entrySet()) {
			String destinationSignCode = entry.getKey();
			List<AgencyAndId> tripIds = entry.getValue();
			for (AgencyAndId tripId : tripIds) {
				tripToDscMap.put(tripId, destinationSignCode);
			}
		}
		return tripToDscMap;
	}

}