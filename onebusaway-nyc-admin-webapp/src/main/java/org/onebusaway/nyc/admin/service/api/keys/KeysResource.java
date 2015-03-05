package org.onebusaway.nyc.admin.service.api.keys;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.onebusaway.users.model.User;
import org.onebusaway.users.model.UserIndex;
import org.onebusaway.users.model.UserIndexKey;
import org.onebusaway.users.services.UserIndexTypes;
import org.onebusaway.users.services.UserPropertiesService;
import org.onebusaway.users.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/keys")
@Component
@Scope("singleton")
public class KeysResource {

	  private final ObjectMapper mapper = new ObjectMapper();
	  private static Logger log = LoggerFactory.getLogger(KeysResource.class);

	  @Autowired
	  private UserService _userService;

	  @Autowired
	  private UserPropertiesService _userPropertiesService;

	  public KeysResource() {
	  }

	  @Path("/list")
	  @GET
	  @Produces("application/json")
	  public Response listKeys() throws JsonGenerationException,
	      JsonMappingException, IOException {
	    log.info("Starting listKeys");

	    try {
	      List<String> apiKeys = _userService.getUserIndexKeyValuesForKeyType(UserIndexTypes.API_KEY);
	      Response response = constructResponse(apiKeys);
		  log.info("Returning response from listKeys");
		  return response;
	    } catch (Exception e) {
	      log.error(e.getMessage());
	      throw new WebApplicationException(e, Response.serverError().build());
	    }
	  }

	  @Path("/create")
	  @GET
	  @Produces("application/json")
	  public Response createKey() throws JsonGenerationException,
	      JsonMappingException, IOException {
	    log.info("Starting createKey with no parameter");
	    return createKey(UUID.randomUUID().toString());
	  }

	  @Path("/create/{keyValue}")
	  @GET
	  @Produces("application/json")
	  public Response createKey(@PathParam("keyValue")
	  String keyValue) throws JsonGenerationException, JsonMappingException,
	      IOException {
	    log.info("Starting createKey with parameter " + keyValue);

	    String message = "API Key created: " + keyValue;
	    try {
	      saveOrUpdateKey(keyValue, 0L);
	    } catch (Exception e) {
	      log.error(e.getMessage());
	      message = e.getMessage();
	    }
	    Response response = constructResponse(message);
	    log.info("Returning createKey result.");
	    return response;
	  }

	  @Path("/delete/{keyValue}")
	  @GET
	  @Produces("application/json")
	  public Response deleteKey(@PathParam("keyValue")
	  String keyValue) throws JsonGenerationException, JsonMappingException,
	      IOException {
	    log.info("Starting deleteKey with parameter " + keyValue);

	    String message = "API Key deleted: " + keyValue;
	    try {
	      delete(keyValue);
	    } catch (Exception e) {
	      log.error(e.getMessage());
	      message = "Failed to delete key: " + e.getMessage();
	    }
	    Response response = constructResponse(message);
	    log.info("Returning deleteKey result.");
	    return response;
	  }

	  // Private methods

	  private void saveOrUpdateKey(String apiKey, Long minApiRequestInterval)
	      throws Exception {
	    UserIndexKey key = new UserIndexKey(UserIndexTypes.API_KEY, apiKey);
	    UserIndex userIndexForId = _userService.getUserIndexForId(key);
	    if (userIndexForId != null) {
	      throw new Exception("API key " + apiKey + " already exists.");
	    }
	    UserIndex userIndex = _userService.getOrCreateUserForIndexKey(key, "", true);

	    _userPropertiesService.authorizeApi(userIndex.getUser(),
	        minApiRequestInterval);

	    // Clear the cached value here
	    _userService.getMinApiRequestIntervalForKey(apiKey, true);
	  }

	  private void delete(String apiKey) throws Exception {
	    UserIndexKey key = new UserIndexKey(UserIndexTypes.API_KEY, apiKey);
	    UserIndex userIndex = _userService.getUserIndexForId(key);

	    if (userIndex == null)
	      throw new Exception("API key " + apiKey + " not found (userIndex null).");

	    User user = userIndex.getUser();

	    boolean found = false;
	    for (UserIndex index : user.getUserIndices()) {
	      if (index.getId().getValue().equalsIgnoreCase(key.getValue())) {
	        userIndex = index;
	        found = true;
	        break;
	      }
	    }
	    if (!found)
	      throw new Exception("API key " + apiKey + " not found (no exact match).");

	    _userService.removeUserIndexForUser(user, userIndex.getId());

	    if (user.getUserIndices().isEmpty())
	      _userService.deleteUser(user);

	    // Clear the cached value here
	    try {
	    _userService.getMinApiRequestIntervalForKey(apiKey, true);
	    } catch (Exception e) {
	      // Ignore this
	    }
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
}
