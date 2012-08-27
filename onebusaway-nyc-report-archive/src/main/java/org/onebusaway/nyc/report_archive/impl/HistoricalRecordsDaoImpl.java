package org.onebusaway.nyc.report_archive.impl;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
	private static final int MAX_RECORD_LIMIT = 3000;
	
	@Override
	public List<HistoricalRecord> getHistoricalRecords(
			final Map<CcAndInferredLocationFilter, Object> filter) {
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
		
		Object startDateObj = filter.get(CcAndInferredLocationFilter.START_DATE);
		hql = addDateBoundary(queryBuilder, hql, ccLocationAlias, inferredLocationAlias, startDateObj);
		
		hql = queryBuilder.join(hql, ccLocationAlias, inferredLocationAlias, "uuid", "=");

		hql = addQueryParams(filter, queryBuilder, ccLocationAlias,	inferredLocationAlias, hql);
		
		hql = queryBuilder.order(hql, inferredLocationAlias, "timeReported", "desc");
		
		addRecordLimit(filter.get(CcAndInferredLocationFilter.RECORDS));
		
		
		final StringBuilder hqlQuery = hql;
		
		List<HistoricalRecord> results = hibernateTemplate.execute(
				new HibernateCallback<List<HistoricalRecord>>() {

			@SuppressWarnings("unchecked")
			@Override
			public List<HistoricalRecord> doInHibernate(Session session) throws HibernateException,
					SQLException {
				
				Query query = buildQuery(filter, hqlQuery, session);

				log.debug("Executing query : " + hqlQuery.toString());
				
				return query.list();
			}

		});
		
		return results;
	}
	
	private Query buildQuery(final Map<CcAndInferredLocationFilter, Object> filter,
			final StringBuilder hqlQuery, Session session) {
		Query query = session.createQuery(hqlQuery.toString());

		setDateParameters(query, filter);
		
		setNamedParamter(query, CcAndInferredLocationFilter.DEPOT_ID, 
				filter.get(CcAndInferredLocationFilter.DEPOT_ID));
		
		setNamedParamter(query, CcAndInferredLocationFilter.INFERRED_ROUTEID, 
				filter.get(CcAndInferredLocationFilter.INFERRED_ROUTEID));
		
		setVehicleIdParamter(query, CcAndInferredLocationFilter.VEHICLE_ID, 
				filter.get(CcAndInferredLocationFilter.VEHICLE_ID));
		
		setNamedParamter(query, CcAndInferredLocationFilter.VEHICLE_AGENCY_ID, 
				filter.get(CcAndInferredLocationFilter.VEHICLE_AGENCY_ID));
		
		setBoundingBoxNamedParameters(query, filter.get(CcAndInferredLocationFilter.BOUNDING_BOX));
		
		setInferredPhaseNamedParams(query, filter.get(CcAndInferredLocationFilter.INFERRED_PHASE));

		query.setResultTransformer(new HistoricalRecordResultTransformer());
		
		return query;
	}

	private StringBuilder addQueryParams(Map<CcAndInferredLocationFilter, Object> filter,
			HQLBuilder queryBuilder, String ccLocationAlias, String inferredLocationAlias, 
			StringBuilder hql) {
		
		hql = addQueryParam(queryBuilder, hql, inferredLocationAlias, 
				CcAndInferredLocationFilter.DEPOT_ID, filter.get(CcAndInferredLocationFilter.DEPOT_ID));
		
		hql = addQueryParam(queryBuilder, hql, inferredLocationAlias, 
				CcAndInferredLocationFilter.INFERRED_ROUTEID, filter.get(CcAndInferredLocationFilter.INFERRED_ROUTEID));
		
		
		hql = addQueryParam(queryBuilder, hql, ccLocationAlias, 
				CcAndInferredLocationFilter.VEHICLE_ID, filter.get(CcAndInferredLocationFilter.VEHICLE_ID));
		
		hql = addQueryParam(queryBuilder, hql, ccLocationAlias, 
				CcAndInferredLocationFilter.VEHICLE_AGENCY_ID, filter.get(CcAndInferredLocationFilter.VEHICLE_AGENCY_ID));
		
		String inferredPhases = (String) filter.get(CcAndInferredLocationFilter.INFERRED_PHASE);
		if(StringUtils.isNotBlank(inferredPhases)) {
			hql = addInferredPhase(queryBuilder, hql, inferredLocationAlias, 
					CcAndInferredLocationFilter.INFERRED_PHASE, inferredPhases);
		}
		
		String boundingBox = (String) filter.get(CcAndInferredLocationFilter.BOUNDING_BOX);
		if(StringUtils.isNotBlank(boundingBox)) {
			hql = addBoundingBoxParam(hql, ccLocationAlias, inferredLocationAlias);
		}
		
		return hql;
	}
	
	private StringBuilder addQueryParam(HQLBuilder queryBuilder, StringBuilder hql, String alias,
			CcAndInferredLocationFilter param, Object value) {
		if(value != null) {
			hql = queryBuilder.where(hql, alias, param.getValue(), ":" +param.getValue());
		}
		return hql;
	}
	
	private void addRecordLimit(Object maxRecords) {
		if(maxRecords != null) {
			Integer recordLimit = (Integer) maxRecords;
			if(recordLimit.intValue() > MAX_RECORD_LIMIT) {
				hibernateTemplate.setMaxResults(MAX_RECORD_LIMIT);
			} else {
				hibernateTemplate.setMaxResults(recordLimit);
			}
		} else {
			//set the number to max record limit if record limit is not specified
			hibernateTemplate.setMaxResults(MAX_RECORD_LIMIT);
		}
	}
	
	private StringBuilder addInferredPhase(HQLBuilder queryBuilder, StringBuilder hql, String alias,
			CcAndInferredLocationFilter param, String value) {
		String [] inferredPhases = value.split(",");
		//There should be atleast one inferred phase value if we are here
		if(hql.toString().contains("where")) {
			hql.append("and(" +alias + "." +param.getValue() + "= " +":inferredPhase0");
		} else {
			hql.append("where(" +alias + "." +param.getValue() + "= " +":inferredPhase0");
		}
		hql.append(SPACE);
		if(inferredPhases.length > 1) {
			for(int i=1;i<inferredPhases.length; i++) {
				hql.append("or ").append(alias + "." +param.getValue() + "= " +":inferredPhase" +i);
			}
		}
		hql.append(")");
		hql.append(SPACE);
		return hql;
	}
	
	private StringBuilder addBoundingBoxParam(StringBuilder hql, String ccLocationAlias, 
			String inferredLocationAlias) {
		if(hql.toString().contains("where")) {
			hql.append("and(" + buildCoordinatesQueryString(ccLocationAlias, inferredLocationAlias) +")");
		} else {
			hql.append("where(" + buildCoordinatesQueryString(ccLocationAlias, inferredLocationAlias) +")");
		}
		hql.append(SPACE);
		
		return hql;
	}

	private String buildCoordinatesQueryString(String ccLocationAlias, 	String inferredLocationAlias) {
		
		StringBuilder query = new StringBuilder("(" +ccLocationAlias +".latitude >=").append(SPACE);
		query.append(":minLatitude").append(SPACE);
		query.append("and").append(SPACE);
		query.append(ccLocationAlias +".latitude <").append(SPACE);
		query.append(":maxLatitude").append(SPACE);
		query.append("and").append(SPACE);
		query.append(ccLocationAlias +".longitude >=").append(SPACE);
		query.append(":minLongitude").append(SPACE);
		query.append("and").append(SPACE);
		query.append(ccLocationAlias +".longitude <").append(SPACE);
		query.append(":maxLongitude").append(")").append(SPACE);
		query.append("or").append(SPACE);
		query.append("(" +inferredLocationAlias + ".inferredLatitude >=").append(SPACE);
		query.append(":minLatitude").append(SPACE);
		query.append("and").append(SPACE);
		query.append(inferredLocationAlias + ".inferredLatitude <").append(SPACE);
		query.append(":maxLatitude").append(SPACE);
		query.append("and").append(SPACE);
		query.append(inferredLocationAlias +".inferredLongitude >=").append(SPACE);
		query.append(":minLongitude").append(SPACE);
		query.append("and").append(SPACE);
		query.append(inferredLocationAlias +".inferredLongitude <").append(SPACE);
		query.append(":maxLongitude").append(")");
		
		return query.toString();
	}
	
	private StringBuilder addDateBoundary(HQLBuilder queryBuilder, StringBuilder hql, String ccLocationAlias,
			String inferredLocationAlias, Object startDateObj) {

		//Check if start date is set. Do not append date boundary if start date is not set
		if(startDateObj != null) {
			hql = queryBuilder.dateBoundary(hql, inferredLocationAlias, "timeReported", 
					":startDate", ":endDate");
		}
		return hql;
	}
	
	private void setDateParameters(Query query, Map<CcAndInferredLocationFilter, Object> filter) {
		Object startDateObj = filter.get(CcAndInferredLocationFilter.START_DATE);
		Object endDateObj = filter.get(CcAndInferredLocationFilter.END_DATE);
		
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		//formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		if(startDateObj != null) {
			Date startDate;
			try {
				startDate = formatter.parse((String)startDateObj);
				Date endDate = null;
				if(endDateObj == null) {
					//Default end date to now if end Date is not specified
					Date now = new Date();
					String endDateString = formatter.format(now);
					endDate = formatter.parse(endDateString);
				} else {
					endDate =  formatter.parse((String)endDateObj);
				}
				query.setParameter("startDate", startDate);
				query.setParameter("endDate", endDate);
			} catch (ParseException e) {
				log.error("Error parsing date field");
				e.printStackTrace();
			}
		}
	}
	
	private void setNamedParamter(Query query, CcAndInferredLocationFilter param, Object value) {
		if(value != null) {
			query.setParameter(param.getValue(), (String) value);
		}
	}
	
	private void setVehicleIdParamter(Query query, CcAndInferredLocationFilter param, Object value) {
		if(value != null) {
			query.setParameter(param.getValue(), (Integer) value);
		}
	}
	
	private void setBoundingBoxNamedParameters(Query query, Object boundingBox) {
		if(boundingBox != null) {
			String [] coordinates = ((String)boundingBox).split(",");
			BigDecimal minLongitude = new BigDecimal(coordinates[0]);
			BigDecimal minLatitude = new BigDecimal(coordinates[1]);
			BigDecimal maxLongitude = new BigDecimal(coordinates[2]);
			BigDecimal maxLatitude = new BigDecimal(coordinates[3]);
			
			query.setParameter("minLongitude", minLongitude);
			query.setParameter("minLatitude", minLatitude);
			query.setParameter("maxLongitude", maxLongitude);
			query.setParameter("maxLatitude", maxLatitude);
		}
	}
	
	private void setInferredPhaseNamedParams(Query query, Object inferredPhases) {
		if(inferredPhases != null) {
			String [] phases = ((String)inferredPhases).split(",");
			for(int i=0; i<phases.length; i++) {
				query.setParameter("inferredPhase" + i, phases[i]);
			}
		}
	}
	
	@Autowired
	@Qualifier("slaveSessionFactory")
	public void setSessionFactory(SessionFactory sessionFactory) {
		hibernateTemplate = new HibernateTemplate(sessionFactory);
	}

}
