/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
