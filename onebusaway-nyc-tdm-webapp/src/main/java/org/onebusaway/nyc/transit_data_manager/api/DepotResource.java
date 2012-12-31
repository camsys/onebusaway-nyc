package org.onebusaway.nyc.transit_data_manager.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.onebusaway.nyc.transit_data_manager.adapters.ModelCounterpartConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.data.VehicleDepotData;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.Vehicle;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.message.DepotsMessage;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.message.VehiclesMessage;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;
import org.onebusaway.nyc.transit_data_manager.api.service.DepotDataProviderService;
import org.onebusaway.nyc.transit_data_manager.json.JsonTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import tcip_final_3_0_5_1.CPTVehicleIden;

@Path("/depot")
@Component
@Scope("request")
public class DepotResource {

	private static Logger _log = LoggerFactory.getLogger(DepotResource.class);

	@Autowired
	private JsonTool jsonTool;

	private DepotIdTranslator depotIdTranslator = null;
	private DepotDataProviderService depotDataProviderService;
	ModelCounterpartConverter<CPTVehicleIden, Vehicle> vehicleConverter;

	public DepotResource() throws IOException {
		try {
			depotIdTranslator = new DepotIdTranslator(new File(System.getProperty("tdm.depotIdTranslationFile")));
		} catch (IOException e) {
			// Set depotIdTranslator to null and otherwise do nothing.
			// Everything works fine without the depot id translator.
			depotIdTranslator = null;
		}
	}



	public void setJsonTool(JsonTool jsonTool) {
		this.jsonTool = jsonTool;
	}


	@Path("/list")
	@GET
	@Produces("application/json")
	public String getDepotList() {
		_log.info("Starting getDepotList.");

		VehicleDepotData data = depotDataProviderService.getVehicleDepotData(depotIdTranslator);

		List<String> allDepotNames = data.getAllDepotNames();

		DepotsMessage message = new DepotsMessage();
		message.setDepots(allDepotNames);
		message.setStatus("OK");

		String outputJson;
		try {
			StringWriter stringWriter = new StringWriter();

			jsonTool.writeJson(stringWriter, message);

			outputJson = stringWriter.toString();

			stringWriter.close();
		} catch (IOException e) {
			// This is unlikely.
			_log.info("Exception writing json output at DepotResource.getDepotList.");
			_log.debug(e.getMessage());

			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		_log.info("getDepotList returning JSON output.");
		return outputJson;
	}

	@Path("/{depotName}/vehicles/list")
	@GET
	@Produces("application/json")
	public String getDepotAssignments(@PathParam("depotName") String depotName)
			throws FileNotFoundException {

		_log.info("Starting getDepotAssignments");

		VehicleDepotData data = depotDataProviderService.getVehicleDepotData(depotIdTranslator);

		// Then I need to get the data for the input depot
		List<CPTVehicleIden> depotVehicles = data.getVehiclesByDepotNameStr(depotName);

		List<Vehicle> depotVehiclesJson = new ArrayList<Vehicle>();

		// now iterate through all the vehicles at that depot, converting each
		// vehicle to its json model representation.
		for(CPTVehicleIden depotVehicle : depotVehicles) {
			depotVehiclesJson.add(vehicleConverter.convert(depotVehicle));
		}

		// Now add it to a message object
		VehiclesMessage message = new VehiclesMessage();
		message.setVehicles(depotVehiclesJson);
		message.setStatus("OK");

		StringWriter writer = new StringWriter();
		String output = null;
		try {
			jsonTool.writeJson(writer, message);
			output = writer.toString();
			writer.close();
		} catch (IOException e) {
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		_log.info("getDepotAssignments returning json output.");

		return output;

	}

	@Path("/vehicles/list")
	@GET
	@Produces("application/json")
	public String getDepotsAssignments(@QueryParam("depots") List<String> depots,
				@QueryParam("inverse") boolean inverseSelection)
			throws FileNotFoundException {

		
		_log.info("Starting getDepotsAssignments(" + depots + ", " + inverseSelection + ")");

		VehicleDepotData data = depotDataProviderService.getVehicleDepotData(depotIdTranslator);
		List<CPTVehicleIden> depotVehicles = new ArrayList<CPTVehicleIden>(); 

		if (!inverseSelection) {
			for (String depot : depots) {
				depotVehicles.addAll(data.getVehiclesByDepotNameStr(depot));
			}
		} else {	
			// Then I need to get the data for the input depot
			depotVehicles.addAll(data.getVehiclesExceptForDepotNameStr(depots));
		}
		
		List<Vehicle> depotVehiclesJson = new ArrayList<Vehicle>();

		// now iterate through all the vehicles at that depot, converting each
		// vehicle to its json model representation.
		for(CPTVehicleIden depotVehicle : depotVehicles) {
			depotVehiclesJson.add(vehicleConverter.convert(depotVehicle));
		}

		// Now add it to a message object
		VehiclesMessage message = new VehiclesMessage();
		message.setVehicles(depotVehiclesJson);
		message.setStatus("OK");

		StringWriter writer = new StringWriter();
		String output = null;
		try {
			jsonTool.writeJson(writer, message);
			output = writer.toString();
			writer.close();
		} catch (IOException e) {
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		_log.info("getDepotsAssignments returning json output.");

		return output;

	}

	/**
	 * @param depotDataProviderService the depotDataProviderService to set
	 */
	@Autowired
	public void setDepotDataProviderService(
			DepotDataProviderService depotDataProviderService) {
		this.depotDataProviderService = depotDataProviderService;
	}

	/**
	 * @param vehicleConverter the vehicleConverter to set
	 */
	@Autowired
	@Qualifier("vehicleFromTcip")
	public void setVehicleConverter(
			ModelCounterpartConverter<CPTVehicleIden, Vehicle> vehicleConverter) {
		this.vehicleConverter = vehicleConverter;
	}

}
