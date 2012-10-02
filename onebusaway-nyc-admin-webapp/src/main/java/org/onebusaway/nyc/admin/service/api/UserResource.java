package org.onebusaway.nyc.admin.service.api;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.onebusaway.nyc.admin.model.ui.UserDetail;
import org.onebusaway.nyc.admin.service.UserManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Provides API methods for user operations
 * @author abelsare
 *
 */
@Path("/users")
@Component
public class UserResource {

	private UserManagementService userManagementService;
	private final ObjectMapper mapper = new ObjectMapper();
	private static Logger log = LoggerFactory.getLogger(UserResource.class);
	
	@GET
	@Produces("application/json")
	public Response getUserNames(@QueryParam("search") String searchString) {
		
		log.info("Getting matching user names");
		
		List<String> matchingUserNames = userManagementService.getUserNames(searchString);
		
		Response response = constructResponse(matchingUserNames);
		
		log.info("Returning response from getUserNames");
		
		return response;
	}
	
	@Path("/getUserDetails")
	@GET
	@Produces("application/json")
	public Response getDetail(@QueryParam("user") String userName) {
		log.info("Getting user detail for user : {}", userName);
		
		UserDetail userDetail = userManagementService.getUserDetail(userName);
		
		Response response = constructResponse(userDetail);
		
		log.info("Returning response from getUserDetail");
		
		return response;
	}
	
	private Response constructResponse(Object result) {
		final StringWriter sw = new StringWriter();
		final MappingJsonFactory jsonFactory = new MappingJsonFactory();
		Response response = null;
		try {
			final JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(sw);
			mapper.writeValue(jsonGenerator, result);
			response = Response.ok(sw.toString()).build();
		} catch (JsonGenerationException e) {
			log.error("Error generating response JSON");
			response = Response.serverError().build();
			e.printStackTrace();
		} catch (JsonMappingException e) {
			log.error("Error mapping response to JSON");
			response = Response.serverError().build();
			e.printStackTrace();
		} catch (IOException e) {
			log.error("I/O error while creating response JSON");
			response = Response.serverError().build();
			e.printStackTrace();
		}
		
		return response; 
	}

	/**
	 * @param userManagementService the userManagementService to set
	 */
	@Autowired
	public void setUserManagementService(UserManagementService userManagementService) {
		this.userManagementService = userManagementService;
	}

	
	
}
