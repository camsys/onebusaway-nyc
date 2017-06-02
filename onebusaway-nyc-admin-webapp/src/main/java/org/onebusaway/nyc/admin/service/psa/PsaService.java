package org.onebusaway.nyc.admin.service.psa;

import java.util.List;

import org.onebusaway.nyc.admin.model.PublicServiceAnnouncement;

public interface PsaService {

  List<PublicServiceAnnouncement> getAllPsas();
  
  void refreshPsas(List<PublicServiceAnnouncement> psas);
  
  PublicServiceAnnouncement getRandomPsa();
}
