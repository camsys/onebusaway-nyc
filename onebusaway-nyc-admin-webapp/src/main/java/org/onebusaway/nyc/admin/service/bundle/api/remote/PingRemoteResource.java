package org.onebusaway.nyc.admin.service.bundle.api.remote;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/ping/remote")
@Component
@Scope("singleton")
/**
 * Ping endpoint on remote server.  Confirms a host is available.
 *
 */
public class PingRemoteResource {

  @GET
  @Produces("application/json")
  public Response ping() {
    return Response.ok("{1}").build();
  }
}
