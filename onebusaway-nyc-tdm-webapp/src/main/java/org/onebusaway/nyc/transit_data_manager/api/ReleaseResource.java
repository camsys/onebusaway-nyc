package org.onebusaway.nyc.transit_data_manager.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.map.ObjectMapper;
import org.onebusaway.nyc.util.git.GitRepositoryHelper;
import org.onebusaway.nyc.util.model.GitRepositoryState;
import org.springframework.stereotype.Component;

import com.sun.jersey.api.spring.Autowire;

/**
 * Webservice to show git status.
 *
 */
@Path("/release")
@Component
@Autowire
public class ReleaseResource {
	private ObjectMapper _mapper = new ObjectMapper();
	private GitRepositoryState gitState = null;
	
	public ReleaseResource() {
		
	}
	
	@GET
	@Produces("application/json")
	public Response getDetails() throws Exception {
		if (gitState == null) {
			gitState = new GitRepositoryHelper().getGitRepositoryState();
		}
		return Response.ok(_mapper.writeValueAsString(gitState)).build();
	}
}
