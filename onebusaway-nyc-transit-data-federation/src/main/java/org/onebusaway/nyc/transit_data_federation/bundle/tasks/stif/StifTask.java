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
package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.services.GtfsMutableRelationalDao;
import org.onebusaway.nyc.transit_data_federation.bundle.model.NycFederatedTransitDataBundle;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.MultiCSVLogger;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.impl.AbnormalStifDataLoggerImpl;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.impl.DSCOverrideHandler;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.impl.DSCServiceManager;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.impl.StifAggregatorImpl;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.impl.StifLoaderImpl;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.impl.StifTaskBundleWriterImpl;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.impl.StifTaskControllerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.File;
import java.util.ArrayList;
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
public class StifTask implements Runnable {

	private GtfsMutableRelationalDao _gtfsMutableRelationalDao;

	private StifTripLoader _loader = null;

	private List<File> _stifPaths = new ArrayList<File>();

	private String _tripToDSCOverridePath;

	private Set<String> _notInServiceDscs = new HashSet<String>();

	private File _notInServiceDscPath;

	@Autowired 
	private NycFederatedTransitDataBundle _bundle;

	private boolean fallBackToStifBlocks = false;

	private MultiCSVLogger csvLogger = null;

	private Boolean _excludeNonRevenue = true;

	@Autowired
	public void setLogger(MultiCSVLogger logger) {
		this.csvLogger = logger;
	}

	@Autowired
	public void setGtfsMutableRelationalDao(
			GtfsMutableRelationalDao gtfsMutableRelationalDao) {
		_gtfsMutableRelationalDao = gtfsMutableRelationalDao;
	}

	/**
	 * The path of the directory containing STIF files to process
	 */
	public void setStifPath(File path) {
		_stifPaths.add(path);
	}

	public void setStifPaths(List<File> paths) {
		_stifPaths.addAll(paths);
	}

	public void setNotInServiceDsc(String notInServiceDsc) {
		_notInServiceDscs.add(notInServiceDsc);
	}

	public void setTripToDSCOverridePath(String path) {
		_tripToDSCOverridePath = path;
	}

	public void setNotInServiceDscs(List<String> notInServiceDscs) {
		_notInServiceDscs.addAll(notInServiceDscs);
	}

	public void setNotInServiceDscPath(File notInServiceDscPath) {
		_notInServiceDscPath = notInServiceDscPath;
	}

	private StifLoaderImpl stifLoader = new StifLoaderImpl();
	private StifAggregatorImpl stifAggregator = new StifAggregatorImpl();
	private StifTaskBundleWriterImpl stifBundleWriter = new StifTaskBundleWriterImpl();
	private DSCOverrideHandler dscOverrideHandler = new DSCOverrideHandler();
	private AbnormalStifDataLoggerImpl abnormalDataLogger = new AbnormalStifDataLoggerImpl();
	private DSCServiceManager dscSvcMgr = new DSCServiceManager();
	private StifTaskControllerImpl stci = new StifTaskControllerImpl();

	public void init(){

		stci.set_abnormalDataLogger(abnormalDataLogger);
		stci.set_dscOverrideHandler(dscOverrideHandler);
		stci.set_dscSvcMgr(dscSvcMgr);
		stci.set_stifAggregator(stifAggregator);
		stci.set_stifBundleWriter(stifBundleWriter);
		stci.set_stifLoader(stifLoader);
		stci.setFallBackToStifBlocks(fallBackToStifBlocks);
		stci.setStifPaths(_stifPaths);

		stifLoader.setAbnormalStifDataLoggerImpl(abnormalDataLogger);
		stifLoader.setExcludeNonRevenue(_excludeNonRevenue);
		stifLoader.setGtfsMutableRelationalDao(_gtfsMutableRelationalDao);
		stifLoader.setStifTripLoader(_loader);

		stifAggregator.setAbnormalStifDataLoggerImpl(abnormalDataLogger);
		stifAggregator.setStifLoader(stifLoader);

		stifBundleWriter.setNycFederatedTransitDataBundle(_bundle);

		dscOverrideHandler.setTripToDSCOverridePath(_tripToDSCOverridePath);

		abnormalDataLogger.setLogger(csvLogger);

		dscSvcMgr.setNotInServiceDscs(_notInServiceDscs);
		dscSvcMgr.setNotInServiceDscPath(_notInServiceDscPath);

	}

	//for unit tests, passthroughs
	public void logDSCStatistics(Map<String, List<AgencyAndId>> dscToTripMap,
			Map<AgencyAndId, String> tripToDscMap) {
		abnormalDataLogger.setLogger(csvLogger);
		abnormalDataLogger.logDSCStatistics(dscToTripMap, tripToDscMap, stifAggregator.getRouteIdsByDsc());
	}

	//start the process!
	public void run() {
		//start up all subclasses and then run them since the bundle builder calls us a certain way! -Phil
		init();//set variables as per whoever called us
		stci.run();//run the stif task
	}

}