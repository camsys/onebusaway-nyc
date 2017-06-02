package org.onebusaway.nyc.admin.service.api;


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.map.ObjectMapper;
import org.onebusaway.nyc.admin.model.PublicServiceAnnouncement;
import org.onebusaway.nyc.admin.service.psa.PsaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Path("/psa")
@Produces("application/json")
public class PsaResource {

  private ObjectMapper _mapper = new ObjectMapper();
  private PsaService _service;
  
  @Autowired
  public void setPsaService(PsaService service) {
    _service = service;
  }
  
  @GET
  public Response getAllPsas() throws Exception {
    return Response.ok(_mapper.writeValueAsString(_service.getAllPsas())).build();
  }
  
  @GET
  @Path("/random")
  @Produces("text/plain")
  public Response random() throws Exception {
    PublicServiceAnnouncement psa = _service.getRandomPsa();
    return Response.ok(psa.getText()).build();
  }
  
}
