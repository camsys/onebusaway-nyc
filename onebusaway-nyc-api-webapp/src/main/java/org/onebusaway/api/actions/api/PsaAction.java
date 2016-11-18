package org.onebusaway.api.actions.api;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.rest.DefaultHttpHeaders;
import org.codehaus.jackson.map.ObjectMapper;
import org.onebusaway.nyc.util.model.PublicServiceAnnouncement;
import org.onebusaway.util.service.psa.PsaService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

@ParentPackage("struts-default")
public class PsaAction {

  private ObjectMapper _mapper = new ObjectMapper();
  private PsaService _service;

  @Autowired
  public void setPsaService(PsaService service) {
    _service = service;
  }

  public DefaultHttpHeaders index() throws Exception {
    List<PublicServiceAnnouncement> psas = _service.getAllPsas();
    String body = _mapper.writeValueAsString(psas);
    HttpServletResponse response = ServletActionContext.getResponse();
    response.setContentType("application/json");
    response.getWriter().write(body);
    return null;
  }


}
