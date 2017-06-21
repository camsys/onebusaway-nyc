package org.onebusaway.nyc.report_archive.impl;

import org.onebusaway.nyc.report_archive.services.CcLocationReportDao;
import org.onebusaway.nyc.report.model.CcLocationReportRecord;
import org.onebusaway.nyc.report.model.InvalidLocationRecord;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class CcLocationReportDaoImpl implements CcLocationReportDao {

	protected static Logger _log = LoggerFactory
			.getLogger(CcLocationReportDaoImpl.class);
	private SessionFactory _sessionFactory;
	
	@Autowired
	@Qualifier("sessionFactory")
	public void setSessionFactory(SessionFactory sessionFactory) {
		_sessionFactory = sessionFactory;
	}

	public Session getSession() {
		return _sessionFactory.getCurrentSession();
	}
	
	@Transactional(rollbackFor = Throwable.class)
	@Override
	public void saveOrUpdateReport(CcLocationReportRecord report) {
		getSession().saveOrUpdate(report);
		getSession().flush();
		getSession().clear();
	}

	@Transactional(rollbackFor = Throwable.class)
	@Override
	public void saveOrUpdateReports(CcLocationReportRecord... reports) {
		List<CcLocationReportRecord> list = new ArrayList<CcLocationReportRecord>(
				reports.length);
		for (CcLocationReportRecord report : reports) {
			getSession().saveOrUpdate(report);
		}
		getSession().flush();
		getSession().clear();
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true)
	public int getNumberOfReports() {
		@SuppressWarnings("rawtypes")
		Query query = getSession().createQuery("select count(*) from CcLocationReportRecord");
		Long count = (Long) query.uniqueResult();
		return count.intValue();
	}

	@Override
	@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW)
	public void handleException(String content, Throwable error,
			Date timeReceived) {
		InvalidLocationRecord ilr = new InvalidLocationRecord(content, error,
				timeReceived);
		getSession().saveOrUpdate(ilr);
		// clear from level one cache
		getSession().flush();
		getSession().evict(ilr);
	}

}
