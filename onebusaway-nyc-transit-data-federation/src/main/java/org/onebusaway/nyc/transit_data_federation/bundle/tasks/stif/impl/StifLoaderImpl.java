package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.impl;

import java.io.File;
import java.util.List;
import java.util.Map;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.GtfsMutableRelationalDao;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.NonRevenueStopData;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.RawRunData;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.StifTrip;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.StifTripLoader;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.StifTripLoaderSupport;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.GeographyRecord;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.ServiceCode;
import org.onebusaway.nyc.transit_data_federation.model.nyc.RunData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StifLoaderImpl {

	private Logger _log = LoggerFactory.getLogger(StifLoaderImpl.class);

	private AbnormalStifDataLoggerImpl _abnormalDataLogger;
	public void setAbnormalStifDataLoggerImpl(AbnormalStifDataLoggerImpl a){
		_abnormalDataLogger = a;
	}
	
	// for unit tests
	private StifTripLoader _loader = null;
	public void setStifTripLoader(StifTripLoader loader) {
		_loader = loader;
	}

	private GtfsMutableRelationalDao _gtfsMutableRelationalDao;

	public void setGtfsMutableRelationalDao(GtfsMutableRelationalDao gtfsMutableRelationalDao) {
		_gtfsMutableRelationalDao = gtfsMutableRelationalDao;
	}

	public GtfsMutableRelationalDao getGtfsMutableRelationalDao(){
		return _gtfsMutableRelationalDao;
	}

	private Boolean _excludeNonRevenue = true;
	public void setExcludeNonRevenue(Boolean excludeNonRevenue) {
		_excludeNonRevenue = excludeNonRevenue;
	}

	public void load(List<File> stifPaths){
		if (_loader == null) {
			// we let the unit tests inject a custom loader
			_log.warn("creating loader with gtfs= " + _gtfsMutableRelationalDao + " and logger=" + _abnormalDataLogger.getLogger());
			_loader = new StifTripLoader();
			_loader.setGtfsDao(_gtfsMutableRelationalDao);
			_loader.setExcludeNonRevenue(_excludeNonRevenue);
			_loader.setLogger(_abnormalDataLogger.getLogger());

			for (File path : stifPaths) {
				loadStif(path, _loader);
			}

		}
	}

	public void loadStif(File path, StifTripLoader loader) {
		// Exclude files and directories like .svn
		if (path.getName().startsWith("."))
			return;

		if (path.isDirectory()) {
			for (String filename : path.list()) {
				File contained = new File(path, filename);
				loadStif(contained, loader);
			}
		} else {
			loader.run(path);
		}
	}

	public Map<Trip, RawRunData> getRawRunDataByTrip(){
		return _loader.getRawRunDataByTrip();
	}

	public Map<ServiceCode, List<StifTrip>> getRawStifData(){
		return _loader.getRawStifData();
	}

	public StifTripLoaderSupport getSupport(){
		return _loader.getSupport();
	}

	public Map<AgencyAndId, RunData> getRunsForTrip(){
		return _loader.getRunsForTrip();
	}
	
	public Map<AgencyAndId, GeographyRecord> getGeographyRecordsByBoxId(){
		return _loader.getGeographyRecordsByBoxId();
	}
	
	public Map<AgencyAndId, List<NonRevenueStopData>> getNonRevenueStopDataByTripId(){
		return _loader.getNonRevenueStopDataByTripId();
	}
	
	public Map<String, List<AgencyAndId>> getTripMapping(){
		return _loader.getTripMapping();
	}
	
	public int getTripsWithoutMatchCount(){
		return _loader.getTripsWithoutMatchCount();
	}
	
	public int getTripsCount(){
		return _loader.getTripsCount();
	}
	
	public void warnOnMissingTrips() {
		for (Trip t : _gtfsMutableRelationalDao.getAllTrips()) {
			String blockId = t.getBlockId();
			if (blockId == null || blockId.equals("")) {
				_log.warn("When matching GTFS to STIF, failed to find block in STIF for "
						+ t.getId());
			}
		}
	}
	
	public void loadStifBlocks() {
		Map<Trip, RawRunData> rawData = this.getRawRunDataByTrip();
		for (Map.Entry<Trip, RawRunData> entry : rawData.entrySet()) {
			Trip trip = entry.getKey();
			if (trip.getBlockId() == null || trip.getBlockId().length() == 0) {
				RawRunData data = entry.getValue();
				trip.setBlockId(trip.getServiceId().getId() + "_STIF_" + data.getDepotCode() + "_" + data.getBlock());
				_gtfsMutableRelationalDao.updateEntity(trip);
			}
		}
	}

}
