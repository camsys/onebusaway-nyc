package org.onebusaway.nyc.transit_data_manager.siri;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sun.jersey.api.spring.Autowire;

@Path("/situation-exchange-incremental")
@Component
@Scope("request")
@Autowire
public class SituationExchangeIncrementalResource extends
    SituationExchangeResource {

  public SituationExchangeIncrementalResource() throws JAXBException {
    super();
  }

  @POST
  @Produces("application/xml")
  @Consumes("application/xml")
  public Response handlePost(String body) throws Exception {
    return handleRequest(body, INCREMENTAL);
  }


}
