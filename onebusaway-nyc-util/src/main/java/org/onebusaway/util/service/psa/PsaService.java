package org.onebusaway.util.service.psa;

import java.util.List;

import org.onebusaway.nyc.util.model.PublicServiceAnnouncement;

public interface PsaService {

  List<PublicServiceAnnouncement> getAllPsas();
  
  void refreshPsas(List<PublicServiceAnnouncement> psas);
}
