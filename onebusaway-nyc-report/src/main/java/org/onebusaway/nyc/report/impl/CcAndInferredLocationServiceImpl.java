package org.onebusaway.nyc.report.impl;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.onebusaway.nyc.report.model.ArchivedInferredLocationRecord;
import org.onebusaway.nyc.report.model.CcAndInferredLocationRecord;
import org.onebusaway.nyc.report.model.CcLocationReportRecord;
import org.onebusaway.nyc.report.services.CcAndInferredLocationDao;
import org.onebusaway.nyc.report.services.CcAndInferredLocationService;
import org.onebusaway.nyc.report.services.RecordValidationService;
import org.onebusaway.nyc.report.util.HQLBuilder;
import org.onebusaway.nyc.report.util.OpsApiLibrary;
import org.onebusaway.nyc.util.impl.tdm.TransitDataManagerApiLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CcAndInferredLocationServiceImpl implements CcAndInferredLocationService {

	protected static Logger _log = LoggerFactory.getLogger(CcAndInferredLocationServiceImpl.class);

	
	private static final String SPACE = " ";

	@Autowired
	private CcLocationCache _ccLocationCache;
	
	@Autowired
	private OpsApiLibrary _apiLibrary;

	@Override
	public List<CcAndInferredLocationRecord> getAllLastKnownRecords(
			Map<CcAndInferredLocationFilter, String> filter) throws Exception {
		//_apiLibrary.getItemsForRequest("last-known", "list");
		return null;
		
	}

	@Override
	public CcAndInferredLocationRecord getLastKnownRecordForVehicle(
			Integer vehicleId) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
}