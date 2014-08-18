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
import org.onebusaway.nyc.report.impl.CcAndInferredLocationFilter;
import org.onebusaway.nyc.report_archive.result.HistoricalRecord;
import org.onebusaway.nyc.report_archive.result.HistoricalRecordResultTransformer;
import org.onebusaway.nyc.report_archive.services.HistoricalRecordsDao;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
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
	private ConfigurationService configurationService;
	
	private static final String SPACE = " ";
	private static final int MAX_RECORD_LIMIT = 3000;
	private static final boolean IS_MYSQL = true;

  protected static final int DEFAULT_QUERY_TIMEOUT = 10 * 60; // 10 minutes
	
	@Override
	public List<HistoricalRecord> getHistoricalRecords(
			final Map<CcAndInferredLocationFilter, Object> filter) {
		
		String ccLocationAlias = "cc";
		String inferredLocationAlias = "inf";
		
		StringBuilder sql = new StringBuilder("select cc.vehicle_agency_designator, cc.time_reported, cc.time_received, " +
				"cc.operator_id_designator, cc.route_id_designator, cc.run_id_designator, cc.dest_sign_code, " +
				"cc.emergency_code, cc.latitude, cc.longitude, cc.nmea_sentence_gprmc, cc.nmea_sentence_gpgga, " +
				"cc.speed, cc.direction_deg, cc.vehicle_id, cc.manufacturer_data, cc.request_id, inf.depot_id, inf.service_date, " +
				"inf.inferred_run_id, inf.assigned_run_id, inf.inferred_block_id, inf.inferred_trip_id, " +
				"inf.inferred_route_id, inf.inferred_direction_id, inf.inferred_dest_sign_code, inf.inferred_latitude, " +
				"inf.inferred_longitude, inf.inferred_phase, inf.inferred_status, inf.inference_is_formal, " +
				"inf.distance_along_block, inf.distance_along_trip, inf.next_scheduled_stop_id, inf.next_scheduled_stop_distance, " +
				"inf.previous_scheduled_stop_id, inf.previous_scheduled_stop_distance, inf.schedule_deviation from obanyc_cclocationreport cc ");
		
  	/* 
  	 * here we expect vehicle_id_2 index to be present. On mysql, create it via
  	 * alter table `obanyc_cclocationreport` add index `vehicle_id_2` (`vehicle_id`,`time_received`);
  	 */
		
		if (filter.containsKey(CcAndInferredLocationFilter.VEHICLE_ID) && IS_MYSQL) {
			sql.append("force index (vehicle_id_2) ");  
		} 
		
		/*
		 * for the historical query, the vehicle and time join out performs the equivalent of the uuid join
		 */
    sql.append("LEFT OUTER JOIN obanyc_inferredlocation inf " +
        "ON cc.vehicle_id = inf.vehicle_id AND cc.time_reported = inf.time_reported ");
		
		//add parameters to the query
		sql = addDateBoundary(sql, "cc.time_received", filter.get(CcAndInferredLocationFilter.START_DATE));
		
		sql = addQueryParams(filter, ccLocationAlias, inferredLocationAlias, sql);

		sql = order(sql, "cc.time_received", "desc");
		
		sql = addRecordLimit(sql, filter.get(CcAndInferredLocationFilter.RECORDS));

		final String sqlQuery = sql.toString();
		
		final Integer timeout = (Integer) (filter.get(CcAndInferredLocationFilter.TIMEOUT) == null? DEFAULT_QUERY_TIMEOUT: filter.get(CcAndInferredLocationFilter.TIMEOUT));
		
		List<HistoricalRecord> results = hibernateTemplate.execute(
				new HibernateCallback<List<HistoricalRecord>>() {

			@SuppressWarnings("unchecked")
			@Override
			public List<HistoricalRecord> doInHibernate(Session session) throws HibernateException,
					SQLException {
				
				Query query = buildQuery(filter, sqlQuery, session);

				log.debug("Executing query(" + timeout + ") : " + sqlQuery);
				query.setTimeout(timeout); // in seconds
				return query.list();
			}

		});
		
		return results;
	}
	
	private Query buildQuery(final Map<CcAndInferredLocationFilter, Object> filter,
			final String sqlQuery, Session session) {
		Query query = session.createSQLQuery(sqlQuery);

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

	private StringBuilder addQueryParams(Map<CcAndInferredLocationFilter, Object> filter, 	String ccLocationAlias, 
			String inferredLocationAlias, StringBuilder sql) {
		
		sql = addQueryParam(sql, inferredLocationAlias +".depot_id", CcAndInferredLocationFilter.DEPOT_ID, 
				filter.get(CcAndInferredLocationFilter.DEPOT_ID));
		
		sql = addQueryParam(sql, inferredLocationAlias +".inferred_route_id", CcAndInferredLocationFilter.INFERRED_ROUTEID,
				filter.get(CcAndInferredLocationFilter.INFERRED_ROUTEID));
		
		
		sql = addQueryParam(sql, ccLocationAlias +".vehicle_id", CcAndInferredLocationFilter.VEHICLE_ID, 
				filter.get(CcAndInferredLocationFilter.VEHICLE_ID));
    
		sql = addQueryParam(sql, ccLocationAlias +".vehicle_agency_designator", CcAndInferredLocationFilter.VEHICLE_AGENCY_ID,
				filter.get(CcAndInferredLocationFilter.VEHICLE_AGENCY_ID));
		
		String inferredPhases = (String) filter.get(CcAndInferredLocationFilter.INFERRED_PHASE);
		
		sql = addInferredPhase(sql, inferredLocationAlias + ".inferred_phase", CcAndInferredLocationFilter.INFERRED_PHASE, 
					inferredPhases);
		
		
		String boundingBox = (String) filter.get(CcAndInferredLocationFilter.BOUNDING_BOX);
		
		sql = addBoundingBoxParam(sql, ccLocationAlias, inferredLocationAlias, boundingBox);
		
		return sql;
	}
	
	private StringBuilder addQueryParam(StringBuilder sql, String field, CcAndInferredLocationFilter param,
			Object value) {
		if(value != null) {
			sql = where(sql, field, ":" +param.getValue());
		}
		return sql;
	}
	
	private StringBuilder where(StringBuilder sql, String field, String value) {
		if(sql.toString().contains("where")) {
			sql.append("and " +field + " = " +value);
		} else {
			sql.append("where " +field + " = " +value);
		}

		sql.append(SPACE);

		return sql;
	}
	
	private StringBuilder addRecordLimit(StringBuilder sql, Object maxRecords) {
		Integer recordLimit = configurationService.getConfigurationValueAsInteger(
				"operational-api.historicalRecordLimit", MAX_RECORD_LIMIT);
		Integer limitValue = recordLimit;
		
		if(maxRecords != null) {
			Integer recordLimitFromParameter = (Integer) maxRecords;
			// enforce record limit to protect database
			if(recordLimitFromParameter.intValue() < recordLimit) {
				limitValue = recordLimitFromParameter;
			}
		}
		
		// MYSQL prefers the SQL syntax to the hibernateTemplate
		if (IS_MYSQL) {
		  sql.append(" limit ").append(limitValue).append(" ");
		} else {
		  hibernateTemplate.setMaxResults(limitValue);
		}
		
		return sql;
	}
	
	private StringBuilder addInferredPhase(StringBuilder sql, String field,
			CcAndInferredLocationFilter param, String value) {
		
		if(StringUtils.isNotBlank(value)) {
			String [] inferredPhases = value.split(",");
			//There should be atleast one inferred phase value if we are here
			if(sql.toString().contains("where")) {
				sql.append("and(" + field + "= " +":" + param.getValue() + "0");
			} else {
				sql.append("where(" + field + "= " +":" + param.getValue() + "0");
			}
			sql.append(SPACE);
			if(inferredPhases.length > 1) {
				for(int i=1;i<inferredPhases.length; i++) {
					sql.append("or ").append(field + "= " +":" + param.getValue() +i);
				}
			}
			sql.append(")");
			sql.append(SPACE);
		}
		
		return sql;
	}
	
	private StringBuilder addBoundingBoxParam(StringBuilder sql, String ccLocationAlias, 
			String inferredLocationAlias, String value) {
		
		if(StringUtils.isNotBlank(value)) {
			if(sql.toString().contains("where")) {
				sql.append("and(" + buildCoordinatesQueryString(ccLocationAlias, inferredLocationAlias) +")");
			} else {
				sql.append("where(" + buildCoordinatesQueryString(ccLocationAlias, inferredLocationAlias) +")");
			}
			sql.append(SPACE);
		}
		
		return sql;
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
		query.append("(" +inferredLocationAlias + ".inferred_latitude >=").append(SPACE);
		query.append(":minLatitude").append(SPACE);
		query.append("and").append(SPACE);
		query.append(inferredLocationAlias + ".inferred_latitude <").append(SPACE);
		query.append(":maxLatitude").append(SPACE);
		query.append("and").append(SPACE);
		query.append(inferredLocationAlias +".inferred_longitude >=").append(SPACE);
		query.append(":minLongitude").append(SPACE);
		query.append("and").append(SPACE);
		query.append(inferredLocationAlias +".inferred_longitude <").append(SPACE);
		query.append(":maxLongitude").append(")");
		
		return query.toString();
	}
	
	private StringBuilder addDateBoundary(StringBuilder sql, String field, Object startDateObj) {

		//Check if start date is set. Do not append date boundary if start date is not set
		if(startDateObj != null) {
			if(sql.toString().contains("where")) {
				sql.append("and(");
			} else {
				sql.append("where(");
			}
			sql.append(field).append(SPACE);
			sql.append(">=").append(SPACE);
			sql.append(":startDate").append(SPACE);
			sql.append("and").append(SPACE);
			sql.append(field).append(SPACE);
			sql.append("<").append(SPACE);
			sql.append(":endDate").append(")").append(SPACE);
			
		} 
		
		return sql;
	}
	
	private StringBuilder order(StringBuilder sql, String field, String order) {
		sql.append("order by " +field);
		sql.append(SPACE);
		if(StringUtils.isNotBlank(order)) {
			sql.append(order);
			sql.append(SPACE);
		}
		
		return sql;
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

	/**
	 * @param configurationService the configurationService to set
	 */
	@Autowired
	public void setConfigurationService(ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}

}
