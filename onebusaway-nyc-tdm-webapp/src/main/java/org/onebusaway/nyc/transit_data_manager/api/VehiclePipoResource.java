package org.onebusaway.nyc.transit_data_manager.api;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.transit_data_manager.adapters.data.ImporterVehiclePulloutData;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.VehiclePullInOutInfo;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.message.VehiclePipoMessage;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;
import org.onebusaway.nyc.transit_data_manager.api.service.RealtimeVehiclePipoService;
import org.onebusaway.nyc.transit_data_manager.api.sourceData.VehiclePipoUploadsFilePicker;
import org.onebusaway.nyc.transit_data_manager.api.vehiclepipo.service.VehiclePullInOutDataProviderService;
import org.onebusaway.nyc.transit_data_manager.api.vehiclepipo.service.VehiclePullInOutService;
import org.onebusaway.nyc.transit_data_manager.json.JsonTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import tcip_final_4_0_0.SCHPullInOutInfo;
import tcip_final_4_0_0.ObaSchPullOutList;
import tcip_final_4_0_0.SchPullOutList.PullOuts;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import com.google.common.collect.ImmutableMap;

interface PulloutListFilter {

public abstract boolean include(boolean includeAll,
    SCHPullInOutInfo pullOut, Map<String, Object> parameters);

}

/**
 * Web service resource to return vehicle pull in pull out data from the server.
 * The data is parsed from a CSV file in the configured directory and converted
 * into TCIP format which is then sent back to the caller as JSON object.
 * 
 * @author abelsare
 *
 */
@Path("/pullouts")
@Component
@Scope("request")
public class VehiclePipoResource {

  private DepotIdTranslator depotIdTranslator;
  private JsonTool jsonTool;
  private VehiclePullInOutService vehiclePullInOutService;
  private VehiclePullInOutDataProviderService vehiclePullInOutDataProviderService;
 
  

  private static Logger log = LoggerFactory
      .getLogger(VehiclePipoResource.class);

  public VehiclePipoResource() throws IOException {

    try {
      depotIdTranslator = new DepotIdTranslator(new File(
          System.getProperty("tdm.depotIdTranslationFile")));
    } catch (IOException e) {
      // Set depotIdTranslator to null and otherwise do nothing.
      // Everything works fine without the depot id translator.
      depotIdTranslator = null;
    }
  }
  
  /**
   * Injects {@link JsonTool}
   * 
   * @param jsonTool
   *          the jsonTool to set
   */
  @Autowired
  public void setJsonTool(JsonTool jsonTool) {
    this.jsonTool = jsonTool;
  }

  /**
   * Injects {@link VehiclePullInOutService}
   * 
   * @param vehiclePullInOutService
   *          the vehiclePullInOutService to set
   */
  @Autowired
  public void setVehiclePullInOutService(
      VehiclePullInOutService vehiclePullInOutService) {
    this.vehiclePullInOutService = vehiclePullInOutService;
  }

  /**
   * @param vehiclePullInOutDataProviderService
   *          the vehiclePullInOutDataProviderService to set
   */
  @Autowired
  public void setVehiclePullInOutDataProviderService(
      VehiclePullInOutDataProviderService vehiclePullInOutDataProviderService) {
    this.vehiclePullInOutDataProviderService = vehiclePullInOutDataProviderService;
  }

  @Path("realtime")
  public RealtimeVehiclePipoResource getRealtimeVehiclePipoResource() throws IOException {
    return new RealtimeVehiclePipoResource();
  }

  @Path("/list")
  @GET
  @Produces("application/json")
  public String getAllActivePullouts(
      @QueryParam(value = "includeAll") final String includeAllPullouts) {
    String method = "getAllActivePullouts";
    log.info("Starting " + method);

    boolean includeAll = isIncludeAllSet(includeAllPullouts);

    ImporterVehiclePulloutData data = vehiclePullInOutDataProviderService
        .getVehiclePipoData(depotIdTranslator);

    List<VehiclePullInOutInfo> activePullouts = vehiclePullInOutService
        .getActivePullOuts(data.getAllPullouts(), includeAll);

    VehiclePipoMessage message = new VehiclePipoMessage();
    message.setPullouts(vehiclePullInOutDataProviderService
        .buildResponseData(activePullouts));
    message.setStatus("OK");

    String outputJson = serializeOutput(message, method);

    log.info(method + " returning json output.");

    return outputJson;
  }

  @Path("/{busNumber}/list")
  @GET
  @Produces("application/json")
  public String getActivePulloutsForBus(
      @PathParam("busNumber") String busNumber,
      @QueryParam(value = "includeAll") final String includeAllPullouts) {
    String method = "getActivePulloutsForBus";
    String output = null;

    log.info("Starting " + method);

    boolean includeAll = isIncludeAllSet(includeAllPullouts);

    ImporterVehiclePulloutData data = vehiclePullInOutDataProviderService
        .getVehiclePipoData(depotIdTranslator);

    Long busId = new Long(busNumber);

    List<VehiclePullInOutInfo> pulloutsByBus = data.getPulloutsByBus(busId);

    if (pulloutsByBus.isEmpty()) {
      output = "No pullouts found for bus : " + busId;
    } else {
      List<VehiclePullInOutInfo> currentActivePulloutByBus = vehiclePullInOutService
          .getActivePullOuts(pulloutsByBus, includeAll);

      VehiclePipoMessage message = new VehiclePipoMessage();
      message.setPullouts(vehiclePullInOutDataProviderService
          .buildResponseData(currentActivePulloutByBus));
      message.setStatus("OK");

      output = serializeOutput(message, method);

    }

    log.info(method + " returning json output.");
    return output;
  }

  @Path("/depot/{depotName}/list")
  @GET
  @Produces("application/json")
  public String getActivePulloutsForDepot(
      @PathParam("depotName") String depotName,
      @QueryParam(value = "includeAll") final String includeAllPullouts) {
    String method = "getActivePulloutsForDepot";
    String output = null;

    log.info("Starting " + method);

    boolean includeAll = isIncludeAllSet(includeAllPullouts);

    ImporterVehiclePulloutData data = vehiclePullInOutDataProviderService
        .getVehiclePipoData(depotIdTranslator);

    List<VehiclePullInOutInfo> pulloutsByDepot = data
        .getPulloutsByDepot(depotName);

    if (pulloutsByDepot.isEmpty()) {
      output = "No pullouts found for depot : " + depotName;
    } else {
      // Get active pullouts once we have all pullouts for a depot
      List<VehiclePullInOutInfo> activePulloutsByDepot = vehiclePullInOutService
          .getActivePullOuts(pulloutsByDepot, includeAll);

      VehiclePipoMessage message = new VehiclePipoMessage();
      message.setPullouts(vehiclePullInOutDataProviderService
          .buildResponseData(activePulloutsByDepot));
      message.setStatus("OK");

      output = serializeOutput(message, method);

    }

    log.info(method + " returning json output.");
    return output;
  }

  @Path("/agency/{agencyId}/list")
  @GET
  @Produces("application/json")
  public String getActivePulloutsForAgency(
      @PathParam("agencyId") String agencyId,
      @QueryParam(value = "includeAll") final String includeAllPullouts) {
    String method = "getActivePulloutsForAgency";
    String output = null;

    log.info("Starting " + method);

    boolean includeAll = isIncludeAllSet(includeAllPullouts);

    ImporterVehiclePulloutData data = vehiclePullInOutDataProviderService
        .getVehiclePipoData(depotIdTranslator);

    List<VehiclePullInOutInfo> pulloutsByAgency = data
        .getPulloutsByAgency(agencyId);

    if (pulloutsByAgency.isEmpty()) {
      output = "No pullouts found for agency : " + agencyId;
    } else {
      // Get active pullouts once we have all pullouts for a depot
      List<VehiclePullInOutInfo> activePulloutsByAgency = vehiclePullInOutService
          .getActivePullOuts(pulloutsByAgency, includeAll);

      VehiclePipoMessage message = new VehiclePipoMessage();
      message.setPullouts(vehiclePullInOutDataProviderService
          .buildResponseData(activePulloutsByAgency));
      message.setStatus("OK");

      output = serializeOutput(message, method);

    }

    log.info(method + " returning json output.");
    return output;
  }

  private boolean isIncludeAllSet(final String includeAllPullouts) {
    boolean includeAll = false;

    // Set include all flag as per the query parameter value
    if (StringUtils.isNotBlank(includeAllPullouts)
        && StringUtils.equalsIgnoreCase(includeAllPullouts, "true")) {
      includeAll = true;
    }
    return includeAll;
  }

  private String serializeOutput(VehiclePipoMessage message, String method) {
    String outputJson;
    StringWriter writer = null;
    try {
      writer = new StringWriter();
      jsonTool.writeJson(writer, message);
      outputJson = writer.toString();

    } catch (IOException e) {
      log.info("Exception writing json output at VehiclePipoResource." + method);
      log.debug(e.getMessage());
      throw new WebApplicationException(e,
          Response.Status.INTERNAL_SERVER_ERROR);
    } finally {
      try {
        writer.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return outputJson;
  }

  

  
  /**
   * Realtime API
   * 
   */
  public class RealtimeVehiclePipoResource {
	  
	private DepotIdTranslator depotIdTranslator;
	private RealtimeVehiclePipoService realtimeVehiclePipoService;
	
	/*@PostConstruct
	public void configureDepotTranslator() throws IOException {
	    try {
	      depotIdTranslator = new DepotIdTranslator(new File(
	          System.getProperty("tdm.depotIdTranslationFile")));
	    } catch (IOException e) {
	      // Set depotIdTranslator to null and otherwise do nothing.
	      // Everything works fine without the depot id translator.
	      depotIdTranslator = null;
	    }
	}*/
	
	public RealtimeVehiclePipoResource() throws IOException {

	    try {
	      depotIdTranslator = new DepotIdTranslator(new File(
	          System.getProperty("tdm.depotIdTranslationFile")));
	    } catch (IOException e) {
	      // Set depotIdTranslator to null and otherwise do nothing.
	      // Everything works fine without the depot id translator.
	      depotIdTranslator = null;
	    }
	  }
	
	@Autowired
	public void setRealtimeVehiclePipoService(
		RealtimeVehiclePipoService realtimeVehiclePipoService) {
		this.realtimeVehiclePipoService = realtimeVehiclePipoService;
	}

    @Path("/list")
    @GET
    @Produces("application/json")
    public String getAllActivePullouts(
        @QueryParam(value = "includeAll") final String includeAllPullouts)
        throws JAXBException, JsonParseException, JsonMappingException,
        IOException {

      PulloutListFilter filter = new PulloutListFilterNoFilter();
      Map<String, Object> parameters = ImmutableMap.<String, Object>of();

      return acquireAndFilterAndRespond(includeAllPullouts, filter, parameters);
    }

    @Path("/{busNumber}/list")
    @GET
    @Produces("application/json")
    public String getActivePulloutsForBus(
        @PathParam("busNumber") String busNumber,
        @QueryParam(value = "includeAll") final String includeAllPullouts)
        throws JAXBException, JsonParseException, JsonMappingException,
        IOException {

      PulloutListFilter filter = new PulloutListFilterByBus();
      Map<String, Object> parameters = ImmutableMap.<String, Object>of("busNumber", busNumber);

      return acquireAndFilterAndRespond(includeAllPullouts, filter, parameters);
    }

    @Path("/depot/{depotName}/list")
    @GET
    @Produces("application/json")
    public String getActivePulloutsForDepot(
        @PathParam("depotName") String depotName,
        @QueryParam(value = "includeAll") final String includeAllPullouts)
        throws JAXBException, JsonParseException, JsonMappingException,
        IOException {

      PulloutListFilter filter = new PulloutListFilterByDepot();
      Map<String, Object> parameters = ImmutableMap.<String, Object>of("depotName", depotName);

      return acquireAndFilterAndRespond(includeAllPullouts, filter, parameters);

    }

    @Path("/agency/{agencyId}/list")
    @GET
    @Produces("application/json")
    public String getActivePulloutsForAgency(
        @PathParam("agencyId") String agencyId,
        @QueryParam(value = "includeAll") final String includeAllPullouts)
        throws JAXBException, JsonParseException, JsonMappingException,
        IOException {

      PulloutListFilter filter = new PulloutListFilterByAgency();
      Map<String, Object> parameters = ImmutableMap.<String, Object>of("agencyId", agencyId);

      return acquireAndFilterAndRespond(includeAllPullouts, filter, parameters);

    }

    
    /**
     * Filter classes.
     */
    
    class PulloutListFilterNoFilter implements PulloutListFilter {
      @Override
      public boolean include(boolean includeAll,
          SCHPullInOutInfo pullOut, Map<String, Object> parameters) {
          if (includeAll || isActive(pullOut)) {
            return true;
          }
        return false;
      }
    }

    class PulloutListFilterByBus implements PulloutListFilter {
      @Override
      public boolean include(boolean includeAll,
          SCHPullInOutInfo pullOut, Map<String, Object> parameters) {
        if (containsUsefulPullOutData(pullOut) && 
        		pullOut.getVehicle().getId().equals(parameters.get("busNumber"))) {
          if (includeAll || isActive(pullOut)) {
            return true;
          }
        }
        return false;
      }
    }

    class PulloutListFilterByDepot implements PulloutListFilter {
      @Override
      public boolean include(boolean includeAll,
          SCHPullInOutInfo pullOut, Map<String, Object> parameters) {
        if (containsUsefulPullOutData(pullOut) && 
        		pullOut.getGarage().getId() != null && 
        		pullOut.getGarage().getId().equals(parameters.get("depotName"))) {
          if (includeAll || isActive(pullOut)) {
            return true;
          }
        }
        return false;
      }
    }

    class PulloutListFilterByAgency implements PulloutListFilter {
      @Override
      public boolean include(boolean includeAll,
          SCHPullInOutInfo pullOut, Map<String, Object> parameters) {
        if (containsUsefulPullOutData(pullOut) &&
        		pullOut.getGarage().getAgdesig() != null && 
        		pullOut.getGarage().getAgdesig().equals(parameters.get("agencyId"))) {
          if (includeAll || isActive(pullOut)) {
            return true;
          }
        }
        return false;
      }
    }

    /*
     * Private support methods.
     */
    
    private String acquireAndFilterAndRespond(final String includeAllPullouts,
        PulloutListFilter filter, Map<String, Object> parameters)
        throws IOException, JsonParseException, JsonMappingException,
        JsonProcessingException {
      
      // Setup
      ObjectMapper m = jaxbSetup();
      boolean includeAll = isIncludeAllSet(includeAllPullouts);

      // Acquire
      ObaSchPullOutList pulloutList = realtimeVehiclePipoService.readRealtimePulloutList(m, depotIdTranslator);

      if (pulloutList.getPullOuts() == null) {
        return m.writeValueAsString(pulloutList);
      }
      
      // Filter
      ObaSchPullOutList filteredList = new ObaSchPullOutList();
      PullOuts pullOuts = new PullOuts();
      filteredList.setPullOuts(pullOuts);
      for (SCHPullInOutInfo pullOut : pulloutList.getPullOuts().getPullOut()) {
        if (filter.include(includeAll, pullOut, parameters)) {
          filteredList.getPullOuts().getPullOut().add(pullOut);
        }
      }
      
      if (filteredList.getPullOuts().getPullOut().isEmpty()) {
        filteredList.setErrorCode("2"); // intentionalBlank
        filteredList.setErrorDescription("intentionalBlank: No pullouts found for query.");
      }

      // Respond
      return m.writeValueAsString(filteredList);
    }

    
    private ObjectMapper jaxbSetup() {
      JaxbAnnotationModule module = new JaxbAnnotationModule();
      ObjectMapper m = new ObjectMapper();
      m.registerModule(module);
      m.registerModule(new AfterburnerModule());
      return m;
    }

    
    private boolean isActive(SCHPullInOutInfo pullOut) {
      return pullOut.getTime().isBeforeNow();
    }

  }
  
  private boolean containsUsefulPullOutData (SCHPullInOutInfo vehiclePipoInfo) {
    boolean result = true;
    
    if (StringUtils.isBlank(vehiclePipoInfo.getVehicle().getId())) // no bus number means not useful.
        result = false;
    if ("open".equalsIgnoreCase(vehiclePipoInfo.getVehicle().getId())) 
      result = false;
    
    if ("no op".equalsIgnoreCase(vehiclePipoInfo.getVehicle().getId()))
      result = false;
    
    return result;
  }

}
