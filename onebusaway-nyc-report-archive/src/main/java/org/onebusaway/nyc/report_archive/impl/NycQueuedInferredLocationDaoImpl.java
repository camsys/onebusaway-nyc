package org.onebusaway.nyc.report_archive.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.SessionFactory;
import org.onebusaway.nyc.report_archive.model.ArchivedInferredLocationRecord;
import org.onebusaway.nyc.report_archive.model.CcAndInferredLocationRecord;
import org.onebusaway.nyc.report_archive.model.CcLocationReportRecord;
import org.onebusaway.nyc.report_archive.services.NycQueuedInferredLocationDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NycQueuedInferredLocationDaoImpl implements NycQueuedInferredLocationDao {

	protected static Logger _log = LoggerFactory.getLogger(NycQueuedInferredLocationDaoImpl.class);

	private static final String SPACE = " ";
	
	private HibernateTemplate _template;

	@Autowired
	private CcLocationCache _ccLocationCache;

	public void setCcLocationCache(CcLocationCache cache) {
		_ccLocationCache = cache;
	}

	@Autowired
	@Qualifier("sessionFactory")
	public void setSessionFactory(SessionFactory sessionFactory) {
		_template = new HibernateTemplate(sessionFactory);
	}

	public HibernateTemplate getHibernateTemplate() {
		return _template;
	}

	@Transactional(rollbackFor = Throwable.class)
	@Override
	public void saveOrUpdateRecord(ArchivedInferredLocationRecord record) {
		_template.saveOrUpdate(record);

		CcLocationReportRecord cc = findRealtimeRecord(record);
		if (cc != null) {
			CcAndInferredLocationRecord lastKnown = new CcAndInferredLocationRecord(
					record, cc);
			_template.saveOrUpdate(lastKnown);
		}

		_template.flush();
		_template.clear();
	}

	@Transactional(rollbackFor = Throwable.class)
	@Override
	public void saveOrUpdateRecords(ArchivedInferredLocationRecord... records) {
		List<ArchivedInferredLocationRecord> list = new ArrayList<ArchivedInferredLocationRecord>(
				records.length);
		for (ArchivedInferredLocationRecord record : records)
			list.add(record);
		_template.saveOrUpdateAll(list);

		// LastKnownRecord
		LinkedHashMap<Integer, CcAndInferredLocationRecord> lastKnownRecords = new LinkedHashMap<Integer, CcAndInferredLocationRecord>(
				records.length);
		for (ArchivedInferredLocationRecord record : records) {
			CcLocationReportRecord cc = findRealtimeRecord(record);
			if (cc != null) {
				CcAndInferredLocationRecord lastKnown = new CcAndInferredLocationRecord(
						record, cc);
				lastKnownRecords.put(lastKnown.getVehicleId(), lastKnown);
			}
		}
		_template.saveOrUpdateAll(lastKnownRecords.values());
		_template.flush();
		_template.clear();
	}

	private CcLocationReportRecord findRealtimeRecord(
			ArchivedInferredLocationRecord record) {
		// first check cache for realtime record
		CcLocationReportRecord realtime = _ccLocationCache.get(record.getUUID());

		// if not in cache log cache miss
		if (realtime == null) {
			/*
			 * NOTE: db is NOT queried for lost record for
			 * performance reasons.  Assume queue has fallen
			 * behind and incoming update will correct this.
			 */
			_log.info("cache miss for " + record.getVehicleId());
		}
		return realtime;
	}

	@Override
	public List<CcAndInferredLocationRecord> getAllLastKnownRecords(
			Map<CcAndInferredLocationFilter, String> filter) {

		StringBuilder hql = new StringBuilder("from CcAndInferredLocationRecord");
		hql.append(SPACE);
		
		addQueryParam(hql, CcAndInferredLocationFilter.DEPOT_ID, filter.get(CcAndInferredLocationFilter.DEPOT_ID));
		addQueryParam(hql, CcAndInferredLocationFilter.INFERRED_ROUTEID, filter.get(CcAndInferredLocationFilter.INFERRED_ROUTEID));
		addQueryParam(hql, CcAndInferredLocationFilter.INFERRED_PHASE, filter.get(CcAndInferredLocationFilter.INFERRED_PHASE));

		String boundingBox = filter.get(CcAndInferredLocationFilter.BOUNDING_BOX);
		if(StringUtils.isNotBlank(boundingBox)) {
			addBoundingBoxParam(hql, boundingBox);
		}
		
		hql.append("order by vehicleId");

		@SuppressWarnings("unchecked")
		List<CcAndInferredLocationRecord> list = _template.find(hql.toString());
		// our join will return a list of object arrays now, in the order
		// we selected above

		return list;
	}

	@Override
	public CcAndInferredLocationRecord getLastKnownRecordForVehicle(
			Integer vehicleId) throws Exception {

		if (vehicleId == null) {
			return null;
		}

		return _template.get(CcAndInferredLocationRecord.class, vehicleId);
	}
	
	private void addQueryParam(StringBuilder hql, CcAndInferredLocationFilter param, String field) {
		if(StringUtils.isNotBlank(field)) {
			where(hql, param, field);
			hql.append(SPACE);
		}
	}

	private void where(StringBuilder hql, CcAndInferredLocationFilter param, String field) {
		if(hql.toString().contains("where")) {
			hql.append("and " +param.getValue() + "='" +field.toUpperCase() + "'");
		} else {
			hql.append("where " +param.getValue() + "='" +field.toUpperCase() + "'");
		}
	}
	
	private void addBoundingBoxParam(StringBuilder hql, String boundingBox) {
		if(hql.toString().contains("where")) {
			hql.append("and(" + buildCoordinatesQueryString(boundingBox) +")");
		} else {
			hql.append("where(" + buildCoordinatesQueryString(boundingBox) +")");
		}
		hql.append(SPACE);
	}

	private String buildCoordinatesQueryString(String boundingBox) {
		String [] coordinates = boundingBox.split(",");
		Double minLongitude = Double.parseDouble(coordinates[0]);
		Double minLatitude = Double.parseDouble(coordinates[1]);
		Double maxLongitude = Double.parseDouble(coordinates[2]);
		Double maxLatitude = Double.parseDouble(coordinates[3]);
		
		StringBuilder query = new StringBuilder("(latitude between").append(SPACE);
		query.append(minLatitude).append(SPACE);
		query.append("and").append(SPACE);
		query.append(maxLatitude).append(SPACE);
		query.append("and").append(SPACE);
		query.append("longitude between").append(SPACE);
		query.append(minLongitude).append(SPACE);
		query.append("and").append(SPACE);
		query.append(maxLongitude).append(")").append(SPACE);
		query.append("or").append(SPACE);
		query.append("(inferred_latitude between").append(SPACE);
		query.append(minLatitude).append(SPACE);
		query.append("and").append(SPACE);
		query.append(maxLatitude).append(SPACE);
		query.append("and").append(SPACE);
		query.append("inferred_longitude between").append(SPACE);
		query.append(minLongitude).append(SPACE);
		query.append("and").append(SPACE);
		query.append(maxLongitude).append(")");
		
		return query.toString();
	}

}