package org.onebusaway.util.service.psa;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.onebusaway.nyc.util.model.PublicServiceAnnouncement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class PsaDaoImpl implements PsaDao {

  protected static Logger _log = LoggerFactory.getLogger(PublicServiceAnnouncement.class);
  private SessionFactory _sessionFactory;
  
  @Autowired
  public void setSessionFactory(SessionFactory sessionFactory) {
    _sessionFactory = sessionFactory;
  }

  private Session getSession(){
    return _sessionFactory.getCurrentSession();
  }
  
  @Override
  public void saveOrUpdate(PublicServiceAnnouncement psa) {
    getSession().saveOrUpdate(psa);
  }
  
  @Override
  public void saveOrUpdate(Collection<PublicServiceAnnouncement> psas) {
    Session session = getSession();
    for(Iterator<PublicServiceAnnouncement> it = psas.iterator(); it.hasNext();){
      session.saveOrUpdate(it.next());
    }
  }
  
  @Override
  public void delete(PublicServiceAnnouncement psa) {
    getSession().delete(psa);
  }
  
  @Override
  public void deleteAll(Collection<PublicServiceAnnouncement> psas) {
    Session session = getSession();
    for(Iterator<PublicServiceAnnouncement> it = psas.iterator(); it.hasNext();){
      session.delete(it.next());
    }
  }
 
  @Override
  public List<PublicServiceAnnouncement> getAllPsas() {
    return getSession().createCriteria(PublicServiceAnnouncement.class).list();
  }

}
