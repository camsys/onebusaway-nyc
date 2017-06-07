package org.onebusaway.nyc.transit_data_manager.persistence.service.impl;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.onebusaway.nyc.transit_data_manager.logging.SystemLogRecord;
import org.onebusaway.nyc.transit_data_manager.persistence.service.SystemLogPersistenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

public class SystemLogPersistenceServiceImpl implements SystemLogPersistenceService{
	private SessionFactory _sessionFactory;

	
	@Transactional("transactionManagerArchive")
	public void saveLogRecord(SystemLogRecord logRecord) {
		getSession().saveOrUpdate(logRecord);
	}
	
	@Autowired
	@Qualifier("archiveSessionFactory")
	public void setSessionFactory(SessionFactory sessionFactory) {
		_sessionFactory = sessionFactory;
	}

	private Session getSession(){
	    return _sessionFactory.getCurrentSession();
    }
}
