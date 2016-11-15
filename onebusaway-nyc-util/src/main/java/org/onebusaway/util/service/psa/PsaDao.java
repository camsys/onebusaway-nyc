package org.onebusaway.util.service.psa;

import java.util.Collection;
import java.util.List;

import org.onebusaway.nyc.util.model.PublicServiceAnnouncement;

public interface PsaDao {
  void saveOrUpdate(PublicServiceAnnouncement psa);
  
  void saveOrUpdate(Collection<PublicServiceAnnouncement> psas);
 
  void delete(PublicServiceAnnouncement psa);
  
  void deleteAll(Collection<PublicServiceAnnouncement> psas);
  
  List<PublicServiceAnnouncement> getAllPsas();
 
}
