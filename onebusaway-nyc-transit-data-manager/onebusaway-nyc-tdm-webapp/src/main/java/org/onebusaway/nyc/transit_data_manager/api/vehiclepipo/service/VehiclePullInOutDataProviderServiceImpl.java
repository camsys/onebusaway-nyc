package org.onebusaway.nyc.transit_data_manager.api.vehiclepipo.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.onebusaway.nyc.transit_data_manager.adapters.ModelCounterpartConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.UtsPulloutsToDataCreator;
import org.onebusaway.nyc.transit_data_manager.adapters.data.ImporterVehiclePulloutData;
import org.onebusaway.nyc.transit_data_manager.adapters.input.VehicleAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.output.json.PullInOutFromTcip;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.PullInOut;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.VehiclePullInOutInfo;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;
import org.onebusaway.nyc.transit_data_manager.api.sourceData.MostRecentFilePicker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Default implementation of {@link VehiclePullInOutDataProviderService}
 * @author abelsare
 *
 */
public class VehiclePullInOutDataProviderServiceImpl implements VehiclePullInOutDataProviderService {

	private MostRecentFilePicker mostRecentFilePicker;
	private VehicleAssignmentsOutputConverter converter;
	private ModelCounterpartConverter<VehiclePullInOutInfo, PullInOut> pulloutDataConverter;
	
	private static Logger log = LoggerFactory.getLogger(VehiclePullInOutDataProviderServiceImpl.class);
	
	@Override
	public ImporterVehiclePulloutData getVehiclePipoData(DepotIdTranslator depotIdTranslator) {
		File inputFile = mostRecentFilePicker.getMostRecentSourceFile();
		
		if (inputFile == null) {
			throw new WebApplicationException(new IOException("No Source file found."), Response.Status.INTERNAL_SERVER_ERROR);
		}
		
		UtsPulloutsToDataCreator creator;
		ImporterVehiclePulloutData pulloutData;
		
		log.debug("Getting PulloutData object in getVehiclePipoData from " + inputFile.getPath());
		
		try {
			creator = new UtsPulloutsToDataCreator(inputFile);
			creator.setConverter(converter);
			
			if (depotIdTranslator == null) {
				log.info("Depot ID translation has not been enabled properly. Depot ids will not be translated.");
			} else {
				log.info("Using depot ID translation.");
			}
			creator.setDepotIdTranslator(depotIdTranslator);
			pulloutData = (ImporterVehiclePulloutData) creator.generateDataObject();
		} catch (FileNotFoundException e) {
			log.info("Could not create data object from " + inputFile.getPath());
		    log.info(e.getMessage());
		    throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
		log.debug("Returning PulloutData object in getVehiclePipoData.");
		return pulloutData;
	}
	
	@Override
	public List<PullInOut> buildResponseData(List<VehiclePullInOutInfo> vehiclePullInOuts) {
		List<PullInOut> outputData = new ArrayList<PullInOut>();
		//Convert to required JSON format
		for(VehiclePullInOutInfo vehiclePullInOut : vehiclePullInOuts) {
			outputData.add(pulloutDataConverter.convert(vehiclePullInOut));
		}
		return outputData;
	}

	/**
	 * Injects most recent file picker
	 * @param mostRecentFilePicker the mostRecentFilePicker to set
	 */
	@Autowired
	@Qualifier("vehiclePipoFilePicker")
	public void setMostRecentFilePicker(MostRecentFilePicker mostRecentFilePicker) {
		this.mostRecentFilePicker = mostRecentFilePicker;
	}
	
	/**
	 * Injects the converter
	 * @param converter the converter to set
	 */
	@Autowired
	public void setConverter(VehicleAssignmentsOutputConverter converter) {
		this.converter = converter;
	}
	
	/**
	 * Injects {@link PullInOutFromTcip}
	 * @param pulloutDataConverter the pulloutDataConverter to set
	 */
	@Autowired
	public void setPulloutDataConverter(
			ModelCounterpartConverter<VehiclePullInOutInfo, PullInOut> pulloutDataConverter) {
		this.pulloutDataConverter = pulloutDataConverter;
	}

}
