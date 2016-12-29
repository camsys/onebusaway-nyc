package org.onebusaway.util.service.psa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.onebusaway.nyc.util.model.PublicServiceAnnouncement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class PsaServiceImpl implements PsaService {
  
  private PsaDao _dao;
  
  @Autowired
  public void setPsaDao(PsaDao dao) {
    _dao = dao;
  }
  
  @Override
  @Transactional(readOnly = true)
  public List<PublicServiceAnnouncement> getAllPsas() {
    return _dao.getAllPsas();
  }

  @Override
  @Transactional(rollbackFor = Throwable.class)
  public void refreshPsas(List<PublicServiceAnnouncement> psas) {
    List<PublicServiceAnnouncement> oldPsas = _dao.getAllPsas();
    List<PublicServiceAnnouncement> toDelete = new ArrayList<PublicServiceAnnouncement>();
    List<PublicServiceAnnouncement> toSave = new ArrayList<PublicServiceAnnouncement>();
    Set<Long> keepIds = new HashSet<Long>();
    for (PublicServiceAnnouncement psa : psas) {
      keepIds.add(psa.getId());
      toSave.add(psa);
    }
    for (PublicServiceAnnouncement psa : oldPsas) {
      if (!keepIds.contains(psa.getId())) {
        toDelete.add(psa);
      }
    }
    
    _dao.deleteAll(toDelete);
    _dao.saveOrUpdate(toSave);
  }

}
