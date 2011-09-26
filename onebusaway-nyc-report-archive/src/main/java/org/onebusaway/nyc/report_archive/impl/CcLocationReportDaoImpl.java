package org.onebusaway.nyc.report_archive.impl;

import org.onebusaway.nyc.report_archive.model.CcLocationReport;
import org.onebusaway.nyc.report_archive.services.CcLocationReportDao;

import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
class CcLocationReportDaoImpl implements CcLocationReportDao {

  private HibernateTemplate _template;

  @Autowired
  public void setSessionFactory(SessionFactory sessionFactory) {
    _template = new HibernateTemplate(sessionFactory);
  }

  public HibernateTemplate getHibernateTemplate() {
    return _template;
  }

  // TODO Remove transactional?
  @Transactional
  @Override
  public void saveOrUpdateReport(CcLocationReport report) {
    _template.saveOrUpdate(report);
  }

  @Override
  public void saveOrUpdateReports(CcLocationReport... reports) {
    List<CcLocationReport> list = new ArrayList<CcLocationReport>(
        reports.length);
    for (CcLocationReport report : reports)
      list.add(report);
    _template.saveOrUpdateAll(list);
  }

  @SuppressWarnings("unchecked")
  public int getNumberOfReports() {
    @SuppressWarnings("rawtypes")
    Long count = (Long) _template.execute(new HibernateCallback() {
      public Object doInHibernate(Session session) throws HibernateException,
          SQLException {
        // TODO This is bad, has a hard-coded table name here... hmm...
        SQLQuery query = session.createSQLQuery("select count(*) from obanyc_cclocationreport_archive");
        long value = ((Number)query.uniqueResult()).longValue();
        return Long.valueOf(value);      }
    });
    return count.intValue();
  }
}
