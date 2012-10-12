package org.onebusaway.nyc.transit_data_manager.api.keys;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
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
import org.onebusaway.nyc.transit_data_manager.config.model.jaxb.Message;
import org.onebusaway.nyc.transit_data_manager.config.model.jaxb.keys.Key;
import org.onebusaway.nyc.transit_data_manager.config.model.jaxb.keys.KeyMessage;
import org.onebusaway.nyc.transit_data_manager.config.model.jaxb.keys.KeysListMessage;
import org.onebusaway.nyc.transit_data_manager.util.ObjectMapperProvider;
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

import com.sun.jersey.api.spring.Autowire;

@Path("/keys")
@Component
@Autowire
@Scope("singleton")
public class KeysResource {

  private static Logger _log = LoggerFactory.getLogger(KeysResource.class);

  @Autowired
  private UserService _userService;

  @Autowired
  private UserPropertiesService _userPropertiesService;

  public KeysResource() {
  }

  // @Autowired
  // private ConfigurationDatastoreInterface datastore;
  //
  // public void setDatastore(ConfigurationDatastoreInterface datastore) {
  // this.datastore = datastore;
  // }
  //
  @Path("/list")
  @GET
  @Produces("application/json")
  public Response listKeys() throws JsonGenerationException,
      JsonMappingException, IOException {
    _log.info("Starting listKeys");

    try {
      List<String> apiKeys = _userService.getUserIndexKeyValuesForKeyType(UserIndexTypes.API_KEY);

      List<Key> keys = new ArrayList<Key>();

      for (String apiKey : apiKeys) {
        Key key = new Key();
        key.setKeyvalue(apiKey);
        keys.add(key);
      }
      KeysListMessage message = new KeysListMessage();
      message.setKeys(keys);

      message.setStatus("OK");
      message.setMessageText("OK");

      Response response = generateResponse(message);
      _log.info("Returning listKeys result.");
      return response;
    } catch (Exception e) {
      _log.error(e.getMessage());
      throw new WebApplicationException(e, Response.serverError().build());
    }
  }

  @Path("/create")
  @GET
  @Produces("application/json")
  public Response createKey() throws JsonGenerationException,
      JsonMappingException, IOException {
    _log.info("Starting createKey with no parameter");
    return createKey(UUID.randomUUID().toString());
  }

  @Path("/create/{keyValue}")
  @GET
  @Produces("application/json")
  public Response createKey(@PathParam("keyValue")
  String keyValue) throws JsonGenerationException, JsonMappingException,
      IOException {
    _log.info("Starting createKey with parameter " + keyValue);

    KeyMessage message = new KeyMessage();
    try {
      saveOrUpdateKey(keyValue, 0L);

      Key key = new Key();
      key.setKeyvalue(keyValue);
      message.setKey(key);

      message.setStatus("OK");
      message.setMessageText("Key created");
    } catch (Exception e) {
      _log.error(e.getMessage());
      message.setStatus("ERROR");
      message.setMessageText("Failed to create key: " + e.getMessage());
    }

    Response response = generateResponse(message);
    _log.info("Returning createKey result.");
    return response;
  }

  @Path("/delete/{keyValue}")
  @GET
  @Produces("application/json")
  public Response deleteKey(@PathParam("keyValue")
  String keyValue) throws JsonGenerationException, JsonMappingException,
      IOException {
    _log.info("Starting deleteKey with parameter " + keyValue);

    KeyMessage message = new KeyMessage();
    try {
      delete(keyValue);

      Key key = new Key();
      key.setKeyvalue(keyValue);
      message.setKey(key);

      message.setStatus("OK");
      message.setMessageText("Key deleted");
    } catch (Exception e) {
      _log.error(e.getMessage());
      message.setStatus("ERROR");
      message.setMessageText("Failed to delete key: " + e.getMessage());
    }

    Response response = generateResponse(message);
    _log.info("Returning deleteKey result.");
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
      throw new Exception("API key " + apiKey + " not found.");

    User user = userIndex.getUser();

    boolean found = false;
    for (UserIndex index : user.getUserIndices()) {
      if (index.getId().equals(key)) {
        found = true;
        break;
      }
    }
    if (!found)
      throw new Exception("API key " + apiKey + " not found.");

    _userService.removeUserIndexForUser(user, key);

    if (user.getUserIndices().isEmpty())
      _userService.deleteUser(user);

    // Clear the cached value here
    _userService.getMinApiRequestIntervalForKey(apiKey, true);
  }

  private Response generateResponse(Message message) throws IOException,
      JsonGenerationException, JsonMappingException {
    Response response;
    ObjectMapper mapper = getObjectMapper();
    StringWriter sw = new StringWriter();
    MappingJsonFactory jsonFactory = new MappingJsonFactory();
    JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(sw);
    // write back response
    mapper.writeValue(jsonGenerator, message);
    response = Response.ok(sw.toString()).build();
    return response;
  }

  // @Path("/{component}/list")
  // @GET
  // @Produces("application/json")
  // public String getComponentConfigList(@PathParam("component")
  // String component) throws JsonGenerationException, JsonMappingException,
  // IOException {
  //
  // _log.info("Starting getComponentConfigList for component " + component);
  //
  // List<ConfigItem> configList = null;
  //
  // // Check to see if we have any data saved for this component
  //
  // if (!datastore.getHasComponent(component)) { // We do not have any
  // // configuration for this
  // // component.
  // configList = new ArrayList<ConfigItem>();
  // } else { // We do have configuration for this component.
  // configList = datastore.getConfigItemsForComponent(component);
  // }
  //
  // ConfigItemsMessage message = new ConfigItemsMessage();
  // message.setConfig(configList);
  // message.setStatus("OK");
  //
  // _log.info("Returning component config as JSON.");
  // return mapper.writeValueAsString(message);
  // }
  //
  // @Path("/{component}/{key}/get")
  // @GET
  // @Produces("application/json")
  // public String getConfigVal(@PathParam("component")
  // String component, @PathParam("key")
  // String key) throws JsonGenerationException, JsonMappingException,
  // IOException {
  //
  // _log.info("Starting getConfigVal for component " + component + " and key "
  // + key);
  // String resultStr = null;
  //
  // ConfigItem item = datastore.getConfigItemByComponentKey(component, key);
  // resultStr = mapper.writeValueAsString(item);
  //
  // _log.info("Returning JSON output for config value.");
  // return resultStr;
  // }
  //
  // @Path("/{component}/{key}/set")
  // @POST
  // @Consumes("application/json")
  // @Produces("application/json")
  // public String setConfigVal(String configStr, @PathParam("component")
  // String component, @PathParam("key")
  // String key) throws JsonParseException, JsonMappingException, IOException {
  //
  // _log.info("Starting setConfigVal for component " + component + " and key "
  // + key);
  //
  // boolean giveUnsupportedValueTypeWarning = false;
  // boolean giveNoValueError = false;
  //
  // // Start by mapping the input, which has an enclosing element, to a
  // // SingleConfigItem
  // SingleConfigItem configItem = mapper.readValue(configStr,
  // SingleConfigItem.class);
  //
  // // Now Grab grab the input config item from the input
  // ConfigItem inputConfigItem = configItem.getConfig();
  //
  // if (!"string".equalsIgnoreCase(inputConfigItem.getValueType())) {
  // giveUnsupportedValueTypeWarning = true;
  // }
  //
  // if (inputConfigItem.getValue() == null) {
  // giveNoValueError = true;
  // }
  //
  // // Now selectively take some values from the input and create a new
  // // configitem
  // // We do this to ensure that the key and component names are identical to
  // // the called url.
  // ConfigItem configToSet = new ConfigItem();
  // configToSet.setComponent(component);
  // configToSet.setKey(key);
  // configToSet.setUpdated(new DateTime());
  // configToSet.setDescription(inputConfigItem.getDescription());
  // configToSet.setValue(inputConfigItem.getValue());
  // configToSet.setUnits(inputConfigItem.getUnits());
  // configToSet.setValueType("String"); // Right now all we do is store things
  // // as strings.
  //
  // if (!giveNoValueError) datastore.setConfigItemByComponentKey(component,
  // key, configToSet);
  //
  // Message statusMessage = new Message();
  //
  // if (giveNoValueError){
  // statusMessage.setStatus("ERROR");
  // statusMessage.setMessageText("No Value entered. Nothing saved.");
  // } else if (giveUnsupportedValueTypeWarning) {
  // statusMessage.setStatus("WARNING");
  // statusMessage.setMessageText("valueTypes other than string are unsupported at this time. Value will be stored as a string.");
  // } else {
  // statusMessage = new Message();
  // statusMessage.setStatus("OK");
  // }
  //
  // _log.info("Returning result in setConfigVal.");
  // return mapper.writeValueAsString(statusMessage);
  //
  // }

  /**
   * Get the object mapper. This function does any setup necessary, such as
   * adding a null value serializer string.
   * 
   * @return
   */
  private ObjectMapper getObjectMapper() {
    return ObjectMapperProvider.getObjectMapper();
  }
}
