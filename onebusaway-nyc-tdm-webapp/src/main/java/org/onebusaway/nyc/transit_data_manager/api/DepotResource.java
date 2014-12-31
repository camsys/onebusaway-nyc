package org.onebusaway.nyc.transit_data_manager.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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

	private static final String TEST_DEPOT = "TEST";
	private static final String TEST_DEPOT_FILE = "testDepot.csv";
	private static final String NEW_DEPOTS_STRING = "^";  // For OBANYC-2282.  This will map to all depot codes longer 
	                                                      // than two characters to pick up new depots that are not
	                                                      // yet mapped to two character codes in depot_ids.csv.

	private static Logger _log = LoggerFactory.getLogger(DepotResource.class);

	@Autowired
	private JsonTool jsonTool;
	
	private DepotIdTranslator depotIdTranslator = null;
	private DepotDataProviderService depotDataProviderService;
	ModelCounterpartConverter<CPTVehicleIden, Vehicle> vehicleConverter;
	private String depotDirKey;

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


	@Path("/updated")
	@GET
	@Produces("application/json")
	/**
	 * Return the date when the underlying data feed was last updated.
	 * @return long as a string, the milliseconds since epoch
	 */
	public String getLastUpdated() {
	  VehicleDepotData data = depotDataProviderService.getVehicleDepotData(depotIdTranslator);
	  String outputJson;
	  try {
	    StringWriter stringWriter = new StringWriter();
	    if (data == null || data.getLastUpdatedDate() == null) {
	      // return the epoch if there are any issues
	      jsonTool.writeJson(stringWriter, new Date(0l));
	    } else {
	      jsonTool.writeJson(stringWriter, data.getLastUpdatedDate().getTime());
	    }
	    outputJson = stringWriter.toString();
	    stringWriter.close();
	  } catch (IOException e) {
	     // This is unlikely.
      _log.info("Exception writing json output at DepotResource.getDepotList.");
      _log.debug(e.getMessage());

      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
	  }
	  
	  return outputJson;
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
		List<CPTVehicleIden> depotVehicles = null;
		if (depotName.equals(NEW_DEPOTS_STRING)) {  // Added for OBANYC-2282 to pick up new depots.
		  // Get list of all depots and check for any longer than two characters
		  List<String> allDepotNames = data.getAllDepotNames();
		  depotVehicles = new ArrayList<CPTVehicleIden>();
		  for (String depotCode : allDepotNames) {
		    if (depotCode.length() > 2) {
		      depotVehicles.addAll(data.getVehiclesByDepotNameStr(depotCode));
		    }
		  }		    
		} else {
		  depotVehicles = data.getVehiclesByDepotNameStr(depotName);
		}

		// add support for testing/monitoring depot and vehicles
		if (TEST_DEPOT.equalsIgnoreCase(depotName)) {
			_log.info("looking for test depots in file=" + getDepotDir() + File.separator
					+ TEST_DEPOT_FILE);
			depotVehicles.addAll(getTestVehicles());
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

		_log.info("getDepotAssignments returning json output.");

		return output;

	}

	private Collection<CPTVehicleIden> getTestVehicles() {
		List<CPTVehicleIden> testVehicles = new ArrayList<CPTVehicleIden>();
		BufferedReader br = null;
		try {
			// look for test file
			
			File testFile = new File(getDepotDir() + File.separator
					+ TEST_DEPOT_FILE);
			// if present, load file
			if (testFile.exists() && testFile.canRead()) {
				br = new BufferedReader(new FileReader(testFile));
				String line;
				while ((line = br.readLine()) != null) {
					testVehicles.add(parseCPTVehicleIden(line));
				}
				// translate vile to CPTVehicleIden
			}
		} catch (Exception any) {
			_log.error("exception loading test vehicles:", any);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (Exception bury) {
					// bury it
				}
			}
		}
		// return
		return testVehicles;
	}



	private CPTVehicleIden parseCPTVehicleIden(String line) {
		if (line == null || line.indexOf(',') == -1) return null;
		String[] elements = line.split(",");
		CPTVehicleIden vehicle = new CPTVehicleIden();
		try {
			vehicle.setAgencyId(Long.parseLong(elements[0]));
			vehicle.setVehicleId(Integer.parseInt(elements[1]));
		} catch (NumberFormatException nfe) {
			_log.error("invalid test vehicle=" + line, nfe);
		}
		return vehicle;
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

	@Autowired
	@Qualifier("depotFileDir")
	public void setDepotDirKey(String depotDirKey) {
		this.depotDirKey = depotDirKey;
	}
	
	private String getDepotDir() {
		String depotDir = System.getProperty(depotDirKey);
		return depotDir;
	}
}
