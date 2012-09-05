package org.onebusaway.nyc.transit_data_manager.api;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.onebusaway.nyc.transit_data_manager.adapters.data.ImporterVehiclePulloutData;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.VehiclePullInOutInfo;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.message.VehiclePipoMessage;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;
import org.onebusaway.nyc.transit_data_manager.api.vehiclepipo.service.VehiclePullInOutDataProviderService;
import org.onebusaway.nyc.transit_data_manager.api.vehiclepipo.service.VehiclePullInOutService;
import org.onebusaway.nyc.transit_data_manager.json.JsonTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Web service resource to return vehicle pull in pull out data from the server. The data is parsed from a CSV
 * file in the configured directory and converted into TCIP format which is then sent back to the caller as 
 * JSON object.
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
	
	
	
	private static Logger log = LoggerFactory.getLogger(VehiclePipoResource.class);
	
	public VehiclePipoResource() throws IOException {
	    
		try {
			depotIdTranslator = new DepotIdTranslator(new File(System.getProperty("tdm.depotIdTranslationFile")));
		} catch (IOException e) {
			// Set depotIdTranslator to null and otherwise do nothing.
			// Everything works fine without the depot id translator.
			depotIdTranslator = null;
		}
	}
	
	@Path("/list")
	@GET
	@Produces("application/json")
	public String getAllActivePullouts() {
		String method = "getAllActivePullouts";
		log.info("Starting " +method);

		ImporterVehiclePulloutData data = vehiclePullInOutDataProviderService.getVehiclePipoData(depotIdTranslator);
		
		List<VehiclePullInOutInfo> activePullouts = vehiclePullInOutService.getActivePullOuts(data.getAllPullouts());

		VehiclePipoMessage message  = new VehiclePipoMessage();
		message.setPullouts(vehiclePullInOutDataProviderService.buildResponseData(activePullouts));
		message.setStatus("OK");
		
		String outputJson = serializeOutput(message, method);
		
		log.info(method + " returning json output.");
		
		return outputJson;
	}
	
	@Path("/{busNumber}/list")
	@GET
	@Produces("application/json")
	public String getActivePulloutsForBus(@PathParam("busNumber") String busNumber) {
		String method = "getActivePulloutsForBus";
		String output = null;
		
		log.info("Starting " +method);

		ImporterVehiclePulloutData data = vehiclePullInOutDataProviderService.getVehiclePipoData(depotIdTranslator);
		
		Long busId = new Long(busNumber);
		
		List<VehiclePullInOutInfo> pulloutsByBus = data.getPulloutsByBus(busId);
		
		if(pulloutsByBus.isEmpty()) {
			output = "No pullouts found for bus : " +busId;
		} else {
			List<VehiclePullInOutInfo> currentActivePulloutByBus = vehiclePullInOutService.getActivePullOuts(pulloutsByBus);
			
			VehiclePipoMessage message  = new VehiclePipoMessage();
			message.setPullouts(vehiclePullInOutDataProviderService.buildResponseData(currentActivePulloutByBus));
			message.setStatus("OK");
			
			output = serializeOutput(message, method);
			
		}
		
		log.info(method + " returning json output.");
		return output;
	}
	
	@Path("/depot/{depotName}/list")
	@GET
	@Produces("application/json")
	public String getActivePulloutsForDepot(@PathParam("depotName") String depotName) {
		String method = "getActivePulloutsForDepot";
		String output = null;
		
		log.info("Starting " +method);

		ImporterVehiclePulloutData	data = vehiclePullInOutDataProviderService.getVehiclePipoData(depotIdTranslator);
		
		List<VehiclePullInOutInfo> pulloutsByDepot = data.getPulloutsByDepot(depotName);
		
		if(pulloutsByDepot.isEmpty()) {
			output = "No pullouts found for depot : " +depotName;
		} else {
			//Get active pullouts once we have all pullouts for a depot
			List<VehiclePullInOutInfo> activePulloutsByDepot = vehiclePullInOutService.getActivePullOuts(pulloutsByDepot);
			
			VehiclePipoMessage message  = new VehiclePipoMessage();
			message.setPullouts(vehiclePullInOutDataProviderService.buildResponseData(activePulloutsByDepot));
			message.setStatus("OK");
			
			output = serializeOutput(message, method);
			
		}
		
		log.info(method + " returning json output.");
		return output;
	}
	
	@Path("/agency/{agencyId}/list")
	@GET
	@Produces("application/json")
	public String getActivePulloutsForAgency(@PathParam("agencyId") String agencyId) {
		String method = "getActivePulloutsForAgency";
		String output = null;
		
		log.info("Starting " +method);

		ImporterVehiclePulloutData	data = vehiclePullInOutDataProviderService.getVehiclePipoData(depotIdTranslator);
		
		List<VehiclePullInOutInfo> pulloutsByAgency = data.getPulloutsByAgency(agencyId);
		
		if(pulloutsByAgency.isEmpty()) {
			output = "No pullouts found for agency : " +agencyId;
		} else {
			//Get active pullouts once we have all pullouts for a depot
			List<VehiclePullInOutInfo> activePulloutsByAgency = vehiclePullInOutService.getActivePullOuts(pulloutsByAgency);
			
			VehiclePipoMessage message  = new VehiclePipoMessage();
			message.setPullouts(vehiclePullInOutDataProviderService.buildResponseData(activePulloutsByAgency));
			message.setStatus("OK");
			
			output = serializeOutput(message, method);
			
		}
		
		log.info(method + " returning json output.");
		return output;
	}
	
	

	private String serializeOutput(VehiclePipoMessage message, String method) {
		String outputJson;
		StringWriter writer = null;
		try {
			writer = new StringWriter();
			jsonTool.writeJson(writer, message);
			outputJson = writer.toString();
			
		} catch(IOException e) {
		      log.info("Exception writing json output at VehiclePipoResource." +method);
		      log.debug(e.getMessage());
		      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
		finally {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return outputJson;
	}
	
	

	/**
	 * Injects {@link JsonTool}
	 * @param jsonTool the jsonTool to set
	 */
	@Autowired
	public void setJsonTool(JsonTool jsonTool) {
		this.jsonTool = jsonTool;
	}

	/**
	 * Injects {@link VehiclePullInOutService}
	 * @param vehiclePullInOutService the vehiclePullInOutService to set
	 */
	@Autowired
	public void setVehiclePullInOutService(
			VehiclePullInOutService vehiclePullInOutService) {
		this.vehiclePullInOutService = vehiclePullInOutService;
	}

	/**
	 * @param vehiclePullInOutDataProviderService the vehiclePullInOutDataProviderService to set
	 */
	@Autowired
	public void setVehiclePullInOutDataProviderService(
			VehiclePullInOutDataProviderService vehiclePullInOutDataProviderService) {
		this.vehiclePullInOutDataProviderService = vehiclePullInOutDataProviderService;
	}
}
