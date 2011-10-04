package org.onebusaway.nyc.transit_data_manager.api.config;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.onebusaway.nyc.transit_data_manager.config.ConfigurationDatastoreInterface;
import org.onebusaway.nyc.transit_data_manager.config.model.jaxb.ConfigItem;
import org.onebusaway.nyc.transit_data_manager.config.model.jaxb.ConfigItemsMessage;
import org.onebusaway.nyc.transit_data_manager.config.model.jaxb.Message;
import org.onebusaway.nyc.transit_data_manager.config.model.jaxb.SingleConfigItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sun.jersey.api.spring.Autowire;

@Path("/config")
@Component
@Autowire
@Scope("singleton")
public class ConfigResource {

  private ObjectMapper mapper;

  public ConfigResource() {
    mapper = new ObjectMapper();
  }

  @Autowired
  private ConfigurationDatastoreInterface datastore;

  // @Autowired
  public void setDatastore(ConfigurationDatastoreInterface datastore) {
    this.datastore = datastore;
  }

  @Path("/list")
  @GET
  @Produces("application/json")
  public String getCompleteConfigList() throws JsonGenerationException,
      JsonMappingException, IOException {
    List<ConfigItem> configList = datastore.getCompleteSetConfigItems();

    ConfigItemsMessage message = new ConfigItemsMessage();
    message.setConfig(configList);
    message.setStatus("OK");

    return mapper.writeValueAsString(message);

  }

  @Path("/{component}/list")
  @GET
  @Produces("application/json")
  public String getComponentConfigList(@PathParam("component")
  String component) throws JsonGenerationException, JsonMappingException,
      IOException {

    List<ConfigItem> configList = null;

    // Check to see if we have any data saved for this component

    if (!datastore.getHasComponent(component)) { // We do not have any
                                                 // configuration for this
                                                 // component.
      configList = new ArrayList<ConfigItem>();
    } else { // We do have configuration for this component.
      configList = datastore.getConfigItemsForComponent(component);
    }

    ConfigItemsMessage message = new ConfigItemsMessage();
    message.setConfig(configList);
    message.setStatus("OK");

    return mapper.writeValueAsString(message);
  }

  @Path("/{component}/{key}/get")
  @GET
  @Produces("application/json")
  public String getConfigVal(@PathParam("component")
  String component, @PathParam("key")
  String key) throws JsonGenerationException, JsonMappingException, IOException {

    String resultStr = null;

    // Check to see if we have any data saved for this component,key

    if (datastore.getComponentHasKey(component, key)) { // we do have a value
                                                        // for this key.
      ConfigItem item = datastore.getConfigItemByComponentKey(component, key);
      resultStr = mapper.writeValueAsString(item);
    } else { // we do not have a value for this key.
      resultStr = "";
    }

    return resultStr;
  }

  @Path("/{component}/{key}/set")
  @POST
  @Consumes("application/json")
  @Produces("application/json")
  public String setConfigVal(String configStr, @PathParam("component")
  String component, @PathParam("key")
  String key) throws JsonParseException, JsonMappingException, IOException {

    // Start by mapping the input, which has an enclosing element, to a
    // SingleConfigItem
    SingleConfigItem configItem = mapper.readValue(configStr,
        SingleConfigItem.class);

    // Now Grab grab the input config item from the input
    ConfigItem inputConfigItem = configItem.getConfig();

    // Now selectively take some values from the input and create a new
    // configitem
    // We do this to ensure that the key and component names are identical to
    // the called url.
    ConfigItem configToSet = new ConfigItem();
    configToSet.setComponent(component);
    configToSet.setKey(key);
    configToSet.setUpdated(new DateTime());
    configToSet.setDescription(inputConfigItem.getDescription());
    configToSet.setValue(inputConfigItem.getValue());
    configToSet.setValueType(inputConfigItem.getValueType());

    datastore.setConfigItemByComponentKey(component, key, configToSet);

    Message statusMessage = new Message();
    statusMessage.setStatus("OK");
    
    return mapper.writeValueAsString(statusMessage);

  }
}
