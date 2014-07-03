package org.onebusaway.nyc.ops.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.onebusaway.nyc.report.impl.CcAndInferredLocationFilter;
import org.onebusaway.nyc.report.impl.CcLocationCache;
import org.onebusaway.nyc.report.impl.RecordValidationServiceImpl;
import org.onebusaway.nyc.report.model.ArchivedInferredLocationRecord;
import org.onebusaway.nyc.report.model.CcAndInferredLocationRecord;
import org.onebusaway.nyc.report.model.CcLocationReportRecord;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;



public class CcAndInferredLocationServiceImplTest {

	  private SessionFactory _sessionFactory;

	  private CcAndInferredLocationDaoImpl _dao;
	  private CcLocationCache _cache;

	  @Before
	  public void setup() throws IOException {

	    Configuration config = new AnnotationConfiguration();
	    config = config.configure("org/onebusaway/nyc/ops/hibernate-configuration.xml");
	    _sessionFactory = config.buildSessionFactory();

	    _cache = new CcLocationCache(10);
	    
	    _dao = new CcAndInferredLocationDaoImpl();
	    _dao.setValidationService(new RecordValidationServiceImpl());
	    _dao.setSessionFactory(_sessionFactory);
	    _dao.setCcLocationCache(_cache);

	  }

	  @After
	  public void teardown() {
	    if (_sessionFactory != null)
	      _sessionFactory.close();
	  }
	  
	  @Test
	  public void test() throws Exception {
		
	    /*assertEquals(0, getNumberOfRecords());

	    CcLocationReportRecord bhs = getCcRecord();
	    _cache.put(bhs); // this happens via OpsInputQueueListener 

	    ArchivedInferredLocationRecord record = getTestRecord();
	    _dao.saveOrUpdateRecords(record);

	    assertEquals(1, getNumberOfRecords());
	    assertEquals(1, getNumberOfCurrentRecords());
	    
	    record = getTestRecord();

	    _dao.saveOrUpdateRecord(record);
	    assertEquals(2, getNumberOfRecords());
	    assertEquals(1, getNumberOfCurrentRecords());

	    Map<CcAndInferredLocationFilter, String> filter = new HashMap<CcAndInferredLocationFilter, String>();
	    filter.put(CcAndInferredLocationFilter.DEPOT_ID, "");
	    filter.put(CcAndInferredLocationFilter.INFERRED_ROUTEID, "");
	    filter.put(CcAndInferredLocationFilter.INFERRED_PHASE, "");
	    
	    List<CcAndInferredLocationRecord> lastKnownRecords = _dao.getAllLastKnownRecords(filter);
	    // this should be one as both saveOrUpdateRecords happen for the same bus. Also this will prob end up being similar to getNumberOfCurrentRecords 

	    assertEquals(1, lastKnownRecords.size()); 
	    assertEquals(record.getVehicleId(), lastKnownRecords.get(0).getVehicleId());
	    
	    Integer aVehicleId = new Integer(120);
	    
	    CcAndInferredLocationRecord latestRec = _dao.getLastKnownRecordForVehicle(aVehicleId);

	    // check that we received a non null value
	    assertNotNull(latestRec);
	    
	    // Now check to see that the single vehicle result matches the result in the getAllLastKnownRecords result for the same vehicle.
	    boolean foundMatch = false;
	    for (CcAndInferredLocationRecord rec : lastKnownRecords) {
	    	if (rec.getVehicleId().intValue() == aVehicleId.intValue()) {
	    		foundMatch = true;
	    		assertTrue(rec.getUUID() == latestRec.getUUID());
	    	}
	    }
	    
	    assertTrue(foundMatch);*/
	  }
	  
	  private CcLocationReportRecord getCcRecord() {
	      ArchivedInferredLocationRecord record = getTestRecord();
	      CcLocationReportRecord cc = new CcLocationReportRecord();
				cc.setUUID(record.getUUID());
	      cc.setTimeReported(record.getTimeReported());
				cc.setArchiveTimeReceived(new Date(122345l));
	      cc.setVehicleId(record.getVehicleId());
	      cc.setDestSignCode(123);
	      cc.setDirectionDeg(new BigDecimal("10.1"));
	      cc.setLatitude(new BigDecimal("70.25"));
	      cc.setLongitude(new BigDecimal("-101.10"));
	      cc.setManufacturerData("manufacturerData");
	      cc.setOperatorIdDesignator("opIdDesignator");
	      cc.setRequestId(456);
	      cc.setRouteIdDesignator("rteIdDesignator");
	      cc.setRunIdDesignator("runIdDesignator");
	      cc.setSpeed(new BigDecimal("5.6"));
	      cc.setTimeReceived(new Date(System.currentTimeMillis()));
	      cc.setVehicleAgencyDesignator("vehicleAgencyDesignator");
	      cc.setVehicleAgencyId(789);
	      cc.setNmeaSentenceGPGGA("$GPRMC,105850.00,A,4038.445646,N,07401.094043,W,002.642,128.77,220611,,,A*7C");
	      cc.setNmeaSentenceGPRMC("$GPGGA,105850.000,4038.44565,N,07401.09404,W,1,09,01.7,+00042.0,M,,M,,*49");
	      cc.setRawMessage("This is a standin for the raw message");
	      return cc;
	  }

	  private ArchivedInferredLocationRecord getTestRecord() {

	    ArchivedInferredLocationRecord record = new ArchivedInferredLocationRecord();
			record.setUUID("foo");
	    record.setTimeReported(new Date(123456789L));
	    record.setVehicleId(120);
	    record.setAgencyId("NYC");
	    record.setArchiveTimeReceived(new Date(System.currentTimeMillis()));
	    record.setServiceDate(new Date(20111010L));
	    record.setScheduleDeviation(1);
	    record.setInferredBlockId("1");
	    record.setInferredTripId("2");
	    record.setDistanceAlongBlock(1000.0);
	    record.setDistanceAlongTrip(5000.0);
	    record.setInferredLatitude(new BigDecimal("70.35"));
	    record.setInferredLongitude(new BigDecimal("-101.20"));
	    record.setInferredPhase("phase");
	    record.setInferredStatus("status");

	    record.setLastUpdateTime(20111010L);
	    record.setLastLocationUpdateTime(20111010L);
	    record.setInferredDestSignCode(123);
	    record.setInferenceIsFormal(false);
	    record.setDepotId("123");
	    record.setEmergencyFlag(true);
	    record.setInferredOperatorId("6");
	    record.setInferredRunId("1");

	    record.setNextScheduledStopId("1");
	    record.setNextScheduledStopDistance(1.0);

	    return record;
	  }    
	 
	 @SuppressWarnings("unchecked")
	private int getNumberOfRecords() {
	    @SuppressWarnings("rawtypes")
	    Long count = (Long) _dao.getHibernateTemplate().execute(new HibernateCallback() {
		public Object doInHibernate(Session session) throws HibernateException {
	        Query query = session.createQuery("select count(*) from ArchivedInferredLocationRecord");
		return (Long)query.uniqueResult();
		}
	    });
	    return count.intValue();
	  }

	  @SuppressWarnings("unchecked")
	private int getNumberOfCurrentRecords() {
	    @SuppressWarnings("rawtypes")
	    Long count = (Long) _dao.getHibernateTemplate().execute(new HibernateCallback() {
		public Object doInHibernate(Session session) throws HibernateException {
	        Query query = session.createQuery("select count(*) from CcAndInferredLocationRecord");
		return (Long)query.uniqueResult();
		}
	    });
	    return count.intValue();
	 }

}


