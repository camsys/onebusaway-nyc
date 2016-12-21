package org.onebusaway.nyc.admin.service.psa;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.onebusaway.nyc.admin.model.PublicServiceAnnouncement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;

public class PsaDaoImpl implements PsaDao {

  protected static Logger _log = LoggerFactory.getLogger(PublicServiceAnnouncement.class);
  private HibernateTemplate _template;
  
  @Autowired
  public void setSessionFactory(SessionFactory sessionFactory) {
    _template = new HibernateTemplate(sessionFactory);
  }
  
  @Override
  public void saveOrUpdate(PublicServiceAnnouncement psa) {
    _template.saveOrUpdate(psa);
  }
  
  @Override
  public void saveOrUpdate(Collection<PublicServiceAnnouncement> psas) {
    _template.saveOrUpdateAll(psas);
  }
  
  @Override
  public void delete(PublicServiceAnnouncement psa) {
    _template.delete(psa);
  }
  
  @Override
  public void deleteAll(Collection<PublicServiceAnnouncement> psas) {
    _template.deleteAll(psas);
  }
 
  @Override 
  public List<PublicServiceAnnouncement> getAllPsas() {
    DetachedCriteria crit = DetachedCriteria.forClass(PublicServiceAnnouncement.class);
    return _template.findByCriteria(crit);
  }

}
