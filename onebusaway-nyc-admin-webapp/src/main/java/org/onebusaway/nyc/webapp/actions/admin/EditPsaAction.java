package org.onebusaway.nyc.webapp.actions.admin;

import java.util.Collections;
import java.util.List;

import org.apache.struts2.convention.annotation.Action;
import org.onebusaway.nyc.util.model.PublicServiceAnnouncement;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCAdminActionSupport;
import org.onebusaway.util.service.psa.PsaService;
import org.springframework.beans.factory.annotation.Autowired;

public class EditPsaAction extends OneBusAwayNYCAdminActionSupport {
  private static final long serialVersionUID = 1L;
  
  private List<PublicServiceAnnouncement> _psas;
  private PsaService _service;
  
  @Autowired
  public void setPsaService(PsaService service) {
    _service = service;
  }
  
  @Override
  public String execute() {
    if (_psas == null) {
      _psas = _service.getAllPsas();
      if (_psas.isEmpty()) {
        _psas.add(new PublicServiceAnnouncement());
      }
      return SUCCESS;
    }
    else {
      _psas.removeAll(Collections.singletonList(null));
      _service.refreshPsas(_psas);
      return SUCCESS;
    }
  }
  
  @Action("add")
  public String add() {
    _psas.add(new PublicServiceAnnouncement());
    return SUCCESS;
  }
 
  public List<PublicServiceAnnouncement> getPsas() {
    return _psas;
  }
  
  public void setPsas(List<PublicServiceAnnouncement> psas) {
    _psas = psas;
  }
  
}
