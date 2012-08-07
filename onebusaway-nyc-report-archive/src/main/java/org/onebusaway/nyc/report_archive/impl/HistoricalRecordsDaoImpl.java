package org.onebusaway.nyc.report_archive.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.onebusaway.nyc.report_archive.result.HistoricalRecord;
import org.onebusaway.nyc.report_archive.result.HistoricalRecordResultTransformer;
import org.onebusaway.nyc.report_archive.services.HistoricalRecordsDao;
import org.onebusaway.nyc.report_archive.util.HQLBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link HistoricalRecordsDao}
 * @author abelsare
 *
 */
@Component
public class HistoricalRecordsDaoImpl implements HistoricalRecordsDao {

	private static Logger log = LoggerFactory.getLogger(HistoricalRecordsDaoImpl.class);
	
	private HibernateTemplate hibernateTemplate;
	
	private static final String SPACE = " ";
	
	@Override
	public List<HistoricalRecord> getHistoricalRecords(
			Map<CcAndInferredLocationFilter, Object> filter) {
		HQLBuilder queryBuilder = new HQLBuilder();
		
		String ccLocationAlias = "cc";
		String inferredLocationAlias = "inf";
		
		StringBuilder hql = new StringBuilder("select cc.vehicleAgencyDesignator, cc.timeReported, " +
				"cc.timeReceived, cc.operatorIdDesignator, cc.routeIdDesignator, cc.runIdDesignator, " +
				"cc.destSignCode, cc.emergencyCode, cc.latitude, cc.longitude, cc.nmeaSentenceGPRMC, " +
				"cc.nmeaSentenceGPGGA, cc.speed, cc.directionDeg, cc.vehicleId, cc.manufacturerData, " +
				"cc.requestId, inf.depotId, inf.serviceDate, inf.inferredRunId, inf.assignedRunId, " +
				"inf.inferredBlockId, inf.inferredTripId, inf.inferredRouteId, inf.inferredDirectionId, " +
				"inf.inferredDestSignCode, inf.inferredLatitude, inf.inferredLongitude, inf.inferredPhase, " +
				"inf.inferredStatus, inf.inferenceIsFormal, inf.distanceAlongBlock, inf.distanceAlongTrip, " +
				"inf.nextScheduledStopId, inf.nextScheduledStopDistance, inf.scheduleDeviation ");
		
		hql = queryBuilder.from(hql, "CcLocationReportRecord", ccLocationAlias);
		hql = queryBuilder.from(hql, "ArchivedInferredLocationRecord", inferredLocationAlias);
		
		hql = addQueryParams(filter, queryBuilder, ccLocationAlias,
				inferredLocationAlias, hql);
		
		hql = queryBuilder.join(hql, ccLocationAlias, inferredLocationAlias, "uuid", "=");
		
		
		addRecordLimit(filter.get(CcAndInferredLocationFilter.RECORDS));
		
		log.info("Executing query : " + hql.toString());
		
		final StringBuilder hqlQuery = hql;
		
		List<HistoricalRecord> results = hibernateTemplate.execute(new HibernateCallback<List<HistoricalRecord>>() {

			@SuppressWarnings("unchecked")
			@Override
			public List<HistoricalRecord> doInHibernate(Session session) throws HibernateException,
					SQLException {
				Query query = session.createQuery(hqlQuery.toString());
				query.setResultTransformer(new HistoricalRecordResultTransformer());
				return query.list();
			}
		});
		
		return results;
	}

	private StringBuilder addQueryParams(Map<CcAndInferredLocationFilter, Object> filter,
			HQLBuilder queryBuilder, String ccLocationAlias, String inferredLocationAlias, 
			StringBuilder hql) {
		
		hql = addStringQueryParam(queryBuilder, hql, inferredLocationAlias, 
				CcAndInferredLocationFilter.DEPOT_ID, filter.get(CcAndInferredLocationFilter.DEPOT_ID));
		
		hql = addStringQueryParam(queryBuilder, hql, inferredLocationAlias, 
				CcAndInferredLocationFilter.INFERRED_ROUTEID, filter.get(CcAndInferredLocationFilter.INFERRED_ROUTEID));
		
		
		hql = addNumberQueryParam(queryBuilder, hql, ccLocationAlias, 
				CcAndInferredLocationFilter.VEHICLE_ID, filter.get(CcAndInferredLocationFilter.VEHICLE_ID));
		
		hql = addNumberQueryParam(queryBuilder, hql, inferredLocationAlias, 
				CcAndInferredLocationFilter.VEHICLE_AGENCY_ID, filter.get(CcAndInferredLocationFilter.VEHICLE_AGENCY_ID));
		
		String inferredPhases = (String) filter.get(CcAndInferredLocationFilter.INFERRED_PHASE);
		if(StringUtils.isNotBlank(inferredPhases)) {
			hql = addInferredPhase(queryBuilder, hql, inferredLocationAlias, 
					CcAndInferredLocationFilter.INFERRED_PHASE, inferredPhases);
		}
		
		String boundingBox = (String) filter.get(CcAndInferredLocationFilter.BOUNDING_BOX);
		if(StringUtils.isNotBlank(boundingBox)) {
			hql = addBoundingBoxParam(hql, boundingBox,	ccLocationAlias, inferredLocationAlias);
		}
		
		Object startDateObj = filter.get(CcAndInferredLocationFilter.START_DATE);
		Object endDateObj = filter.get(CcAndInferredLocationFilter.END_DATE);
		
		addDateBoundary(queryBuilder, hql, ccLocationAlias, startDateObj, endDateObj);
		
		return hql;
	}
	
	private StringBuilder addStringQueryParam(HQLBuilder queryBuilder, StringBuilder hql, String alias,
			CcAndInferredLocationFilter param, Object value) {
		String stringValue = (String) value;
		if(StringUtils.isNotBlank(stringValue)) {
			hql = queryBuilder.where(hql, alias, param.getValue(), stringValue);
		}
		return hql;
	}
	
	private StringBuilder addNumberQueryParam(HQLBuilder queryBuilder, StringBuilder hql, String alias,
			CcAndInferredLocationFilter param, Object value) {
		Integer numberValue = (Integer) value;
		if(numberValue != null) {
			hql = queryBuilder.where(hql, alias, param.getValue(), numberValue);
		}
		return hql;
	}
	
	private void addRecordLimit(Object maxRecords) {
		if(maxRecords != null) {
			Integer recordLimit = (Integer) maxRecords;
			hibernateTemplate.setMaxResults(recordLimit);
		}
	}
	
	private StringBuilder addInferredPhase(HQLBuilder queryBuilder, StringBuilder hql, String alias,
			CcAndInferredLocationFilter param, String value) {
		String [] inferredPhases = value.split(",");
		//There should be atleast one inferred phase value if we are here
		if(hql.toString().contains("where")) {
			hql.append("and(" +alias + "." +param.getValue() + "='" +inferredPhases[0] +"'");
		} else {
			hql.append("where(" +alias + "." +param.getValue() + "='" +inferredPhases[0] +"'");
		}
		hql.append(SPACE);
		if(inferredPhases.length > 1) {
			for(int i=1;i<inferredPhases.length; i++) {
				hql.append("or ").append(alias + "." +param.getValue() + "='" +inferredPhases[i] +"'");
			}
		}
		hql.append(")");
		hql.append(SPACE);
		return hql;
	}
	
	private StringBuilder addBoundingBoxParam(StringBuilder hql, String boundingBox,
			String ccLocationAlias, String inferredLocationAlias) {
		if(hql.toString().contains("where")) {
			hql.append("and(" + buildCoordinatesQueryString(boundingBox, ccLocationAlias, 
					inferredLocationAlias) +")");
		} else {
			hql.append("where(" + buildCoordinatesQueryString(boundingBox, ccLocationAlias, 
					inferredLocationAlias) +")");
		}
		hql.append(SPACE);
		
		return hql;
	}

	private String buildCoordinatesQueryString(String boundingBox, String ccLocationAlias, 
			String inferredLocationAlias) {
		String [] coordinates = boundingBox.split(",");
		Double minLongitude = Double.parseDouble(coordinates[0]);
		Double minLatitude = Double.parseDouble(coordinates[1]);
		Double maxLongitude = Double.parseDouble(coordinates[2]);
		Double maxLatitude = Double.parseDouble(coordinates[3]);
		
		StringBuilder query = new StringBuilder("(" +ccLocationAlias +".latitude between").append(SPACE);
		query.append(minLatitude).append(SPACE);
		query.append("and").append(SPACE);
		query.append(maxLatitude).append(SPACE);
		query.append("and").append(SPACE);
		query.append(ccLocationAlias +".longitude between").append(SPACE);
		query.append(minLongitude).append(SPACE);
		query.append("and").append(SPACE);
		query.append(maxLongitude).append(")").append(SPACE);
		query.append("or").append(SPACE);
		query.append("(" +inferredLocationAlias + ".inferredLatitude between").append(SPACE);
		query.append(minLatitude).append(SPACE);
		query.append("and").append(SPACE);
		query.append(maxLatitude).append(SPACE);
		query.append("and").append(SPACE);
		query.append(inferredLocationAlias +".inferredLongitude between").append(SPACE);
		query.append(minLongitude).append(SPACE);
		query.append("and").append(SPACE);
		query.append(maxLongitude).append(")");
		
		return query.toString();
	}
	
	private StringBuilder addDateBoundary(HQLBuilder queryBuilder, StringBuilder hql, String alias,
			Object startDateObj, Object endDateObj) {
		//DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		//Check if start date is set. Do not append date boundary if start date is not set
		String startDate = (String) startDateObj;
		String endDate = (String) endDateObj;
		
		if(StringUtils.isNotBlank(startDate)) {
			//try {
				//Date startDate = formatter.parse((String) startDateObj);
				if(StringUtils.isBlank(endDate)) {
					//Default end date to now if end Date is not specified
					//endDate = formatter.parse(new Date().toString());
				} 
				hql = queryBuilder.dateBoundary(hql, alias, "timeReported", startDate, endDate);
			/*} catch (ParseException e) {
				log.error("Error parsing date field");
				e.printStackTrace();
			}*/
		}
		return hql;
	}
	
	@Autowired
	@Qualifier("slaveSessionFactory")
	public void setSessionFactory(SessionFactory sessionFactory) {
		hibernateTemplate = new HibernateTemplate(sessionFactory);
	}

}
