package org.onebusaway.nyc.report_archive.impl;

import org.onebusaway.nyc.report_archive.model.CcLocationReportRecord;
import org.onebusaway.nyc.report_archive.model.InvalidLocationRecord;
import org.onebusaway.nyc.report_archive.services.CcLocationReportDao;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class CcLocationReportDaoImpl implements CcLocationReportDao {

	protected static Logger _log = LoggerFactory
			.getLogger(CcLocationReportDaoImpl.class);
	public static final int BATCH_SIZE = 1000;
	private HibernateTemplate _template;
	private int _batchCount = 0;

	@Autowired
	public void setSessionFactory(SessionFactory sessionFactory) {
		_template = new HibernateTemplate(sessionFactory);
	}

	public HibernateTemplate getHibernateTemplate() {
		return _template;
	}

	@Transactional(rollbackFor = Throwable.class)
	@Override
	public void saveOrUpdateReport(CcLocationReportRecord report) {
		_batchCount++;
		_template.saveOrUpdate(report);
		if (_batchCount == BATCH_SIZE) {
			// clear from level one cache
			_log.warn("cc flush");
			_template.flush();
			_template.clear();
			_batchCount = 0;
		}
	}

	@Override
	public void saveOrUpdateReports(CcLocationReportRecord... reports) {
		List<CcLocationReportRecord> list = new ArrayList<CcLocationReportRecord>(
				reports.length);
		for (CcLocationReportRecord report : reports)
			list.add(report);
		_template.saveOrUpdateAll(list);
	}

	@SuppressWarnings("unchecked")
	public int getNumberOfReports() {
		@SuppressWarnings("rawtypes")
		Long count = (Long) _template.execute(new HibernateCallback() {
			public Object doInHibernate(Session session)
					throws HibernateException {
				Query query = session
						.createQuery("select count(*) from CcLocationReportRecord");
				return (Long) query.uniqueResult();
			}
		});
		return count.intValue();
	}

	@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW)
	public void handleException(String content, Throwable error,
			Date timeReceived) {
		InvalidLocationRecord ilr = new InvalidLocationRecord(content, error,
				timeReceived);
		_template.saveOrUpdate(ilr);
		// clear from level one cache
		_template.flush();
		_template.evict(ilr);
	}

}
