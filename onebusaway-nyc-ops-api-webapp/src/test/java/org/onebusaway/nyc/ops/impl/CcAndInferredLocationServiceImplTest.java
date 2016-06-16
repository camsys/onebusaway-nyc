package org.onebusaway.nyc.ops.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onebusaway.nyc.ops.util.OpsApiLibrary;
import org.onebusaway.nyc.report.api.json.JsonTool;
import org.onebusaway.nyc.report.api.json.LowerCaseWDashesGsonJsonTool;
import org.onebusaway.nyc.report.impl.CcLocationCache;
import org.onebusaway.nyc.report.impl.RecordValidationServiceImpl;
import org.onebusaway.nyc.report.model.CcAndInferredLocationRecord;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;



public class CcAndInferredLocationServiceImplTest {

	  private SessionFactory _sessionFactory;
	  private CcAndInferredLocationDaoImpl _dao;
	  private CcAndInferredLocationServiceImpl _service;
	  private CcLocationCache _cache;
	  private JsonTool _jsonTool;
	  
	  @Mock
	  private OpsApiLibrary _apiLibrary;
	  
	  @Before
	  public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);
	    Configuration config = new AnnotationConfiguration();
	    config = config.configure("org/onebusaway/nyc/ops/hibernate-configuration.xml");
	    _sessionFactory = config.buildSessionFactory();
	    _jsonTool = new LowerCaseWDashesGsonJsonTool(false);
	    _service = new CcAndInferredLocationServiceImpl();

	    _dao = new CcAndInferredLocationDaoImpl();
	    _dao.setValidationService(new RecordValidationServiceImpl());
	    _dao.setSessionFactory(_sessionFactory);
	    _dao.setCcLocationCache(_cache);
	    
	    String last_results_json = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("sample-message.json"))).readLine();
		when(_apiLibrary.getItemsForRequestAsString("last-known", "list")).thenReturn(last_results_json);
		
		_service.set_apiLibrary(_apiLibrary);
		_service.set_jsonTool(_jsonTool);
	    
	  }

	  @After
	  public void teardown() {
	    if (_sessionFactory != null)
	      _sessionFactory.close();
	  }
	  
	  @Test
	  public void serviceTest() throws Exception {
		ArrayList<CcAndInferredLocationRecord> records = (ArrayList<CcAndInferredLocationRecord>) _service.getAllLastKnownRecords();
	    CcAndInferredLocationRecord record = records.get(0);
	    
	    // Check for Exactly One Result
	    assertEquals(1, records.size());
	    
	    // Check Deserialized Values
	    assertEquals("77a5b211-086e-11e4-9b1a-123139312bff", record.getUUID());
	    assertEquals(new Integer(2008), record.getVehicleAgencyId());
	    assertEquals("2014-07-10T20:12:06.000Z", record.getTimeReported());
	    assertEquals("2014-07-10T20:12:07.473Z", record.getTimeReceived());
	    assertEquals("2014-07-10T20:12:07.566Z", record.getArchiveTimeReceived());
	    assertEquals("648703", record.getOperatorIdDesignator());
	    assertEquals("44", record.getRouteIdDesignator());
	    assertEquals("24", record.getRunIdDesignator());
	    assertEquals(new Integer(8443), record.getDestSignCode());
	    assertEquals(new BigDecimal(40.675807).setScale(6, RoundingMode.HALF_UP), record.getLatitude());
	    assertEquals(new BigDecimal(-73.950039).setScale(6, RoundingMode.HALF_UP), record.getLongitude());
	    assertEquals(new BigDecimal(11.5), record.getSpeed());
	    assertEquals(new BigDecimal(189.10).setScale(2, RoundingMode.HALF_UP), record.getDirectionDeg());
	    assertEquals("MTANYCT", record.getAgencyId());
	    assertEquals(new Integer(9695), record.getVehicleId());
	    assertEquals("FB", record.getDepotId());
	    assertEquals("2014-07-10", record.getServiceDate());
	    assertEquals("B44-24", record.getInferredRunId());
	    assertEquals(new Integer(8443), record.getInferredDestSignCode());
	    assertEquals(new BigDecimal(40.675811).setScale(6, RoundingMode.HALF_UP), record.getInferredLatitude());
	    assertEquals(new BigDecimal(-73.949941).setScale(6, RoundingMode.HALF_UP), record.getInferredLongitude());
	    assertEquals("IN_PROGRESS", record.getInferredPhase());
	    assertEquals("blockInf", record.getInferredStatus());
	    assertEquals(true, record.isInferenceIsFormal());
	    assertEquals(new Double(17316.347663427005), record.getDistanceAlongBlock());
	    assertEquals(new Double(2664.283320458455), record.getDistanceAlongTrip());
	    assertEquals(new Integer(66), record.getScheduleDeviation());
	    assertEquals("B44-24", record.getAssignedRunId());
	  }

}


