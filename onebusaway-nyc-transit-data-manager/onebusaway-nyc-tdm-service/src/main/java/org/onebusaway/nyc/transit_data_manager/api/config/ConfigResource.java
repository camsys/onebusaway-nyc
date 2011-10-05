package org.onebusaway.nyc.transit_data_manager.api.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import org.codehaus.jackson.map.ser.StdSerializerProvider;
import org.joda.time.DateTime;
import org.onebusaway.nyc.transit_data_manager.config.ConfigurationDatastoreInterface;
import org.onebusaway.nyc.transit_data_manager.config.model.jaxb.ConfigItem;
import org.onebusaway.nyc.transit_data_manager.config.model.jaxb.ConfigItemsMessage;
import org.onebusaway.nyc.transit_data_manager.config.model.jaxb.Message;
import org.onebusaway.nyc.transit_data_manager.config.model.jaxb.SingleConfigItem;
import org.onebusaway.nyc.transit_data_manager.config.model.jaxb.WarningMessage;
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
    mapper = getObjectMapper();
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

    ConfigItem item = datastore.getConfigItemByComponentKey(component, key);
    resultStr = mapper.writeValueAsString(item);

    return resultStr;
  }

  @Path("/{component}/{key}/set")
  @POST
  @Consumes("application/json")
  @Produces("application/json")
  public String setConfigVal(String configStr, @PathParam("component")
  String component, @PathParam("key")
  String key) throws JsonParseException, JsonMappingException, IOException {

    boolean giveUnsupportedValueTypeWarning = false;
    boolean giveNoValueError = false;

    // Start by mapping the input, which has an enclosing element, to a
    // SingleConfigItem
    SingleConfigItem configItem = mapper.readValue(configStr,
        SingleConfigItem.class);

    // Now Grab grab the input config item from the input
    ConfigItem inputConfigItem = configItem.getConfig();

    if (!"string".equalsIgnoreCase(inputConfigItem.getValueType())) {
      giveUnsupportedValueTypeWarning = true;
    }
    
    if (inputConfigItem.getValue() == null) {
      giveNoValueError = true;
    }

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
    configToSet.setUnits(inputConfigItem.getUnits());
    configToSet.setValueType("String"); // Right now all we do is store things
                                        // as strings.

    if (!giveNoValueError) datastore.setConfigItemByComponentKey(component, key, configToSet);

    Message statusMessage = null;

    if (giveNoValueError){
      statusMessage = new WarningMessage();
      statusMessage.setStatus("ERROR");
      ((WarningMessage) statusMessage).setWarningMessage("No Value entered. Nothing saved.");
    } else if (giveUnsupportedValueTypeWarning) {
      statusMessage = new WarningMessage();
      statusMessage.setStatus("WARNING");
      ((WarningMessage) statusMessage).setWarningMessage("valueTypes other than string are unsupported at this time. Value will be stored as a string.");
    } else {
      statusMessage = new Message();
      statusMessage.setStatus("OK");
    }

    return mapper.writeValueAsString(statusMessage);

  }

  /**
   * Get the object mapper. This function does any setup necessary, such
   * as adding a null value serializer
   * string.
   * 
   * @return
   */
  private ObjectMapper getObjectMapper() {
    // this code was taken from
    // http://wiki.fasterxml.com/JacksonHowToCustomSerializers
    // Basically it sets up a serializer for null values, so that they are
    // mapped to an empty string.

    StdSerializerProvider sp = new StdSerializerProvider();
    //sp.setNullValueSerializer(new NullSerializer()); // Turns out for now I do not want to serialize null as "".
    
    
    ObjectMapper m = new ObjectMapper();
    m.setSerializerProvider(sp);

    return m;
  }
}
