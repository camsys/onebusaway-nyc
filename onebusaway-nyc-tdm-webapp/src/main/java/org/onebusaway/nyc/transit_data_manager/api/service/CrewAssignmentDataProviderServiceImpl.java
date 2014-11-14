package org.onebusaway.nyc.transit_data_manager.api.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.onebusaway.nyc.transit_data_manager.adapters.ModelCounterpartConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.UTSUtil;
import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.UtsCrewAssignsToDataCreator;
import org.onebusaway.nyc.transit_data_manager.adapters.data.OperatorAssignmentData;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.OperatorAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;
import org.onebusaway.nyc.transit_data_manager.api.sourceData.MostRecentFilePicker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import tcip_final_4_0_0_0.SCHOperatorAssignment;

/**
 * Default implementation of {@link CrewAssignmentDataProviderService}
 * @author abelsare
 *
 */
public class CrewAssignmentDataProviderServiceImpl implements CrewAssignmentDataProviderService{

	private MostRecentFilePicker mostRecentFilePicker;
	private ModelCounterpartConverter<SCHOperatorAssignment, OperatorAssignment> crewDataConverter;
	
	private static Logger log = LoggerFactory.getLogger(CrewAssignmentDataProviderServiceImpl.class);
	
	@Override
	public OperatorAssignmentData getCrewAssignmentData(DepotIdTranslator depotIdTranslator) {
		File inputFile = mostRecentFilePicker.getMostRecentSourceFile();

		if (inputFile == null) {
			throw new WebApplicationException(new IOException("No Source file found."), Response.Status.INTERNAL_SERVER_ERROR);
		}

		log.debug("Generating OperatorAssignmentData object from file " + inputFile.getPath());
		// First create a OperatorAssignmentData object

		OperatorAssignmentData data;

		try {
			data = getOpAssignDataObjectForFile(inputFile, depotIdTranslator);
		} catch (IOException e1) {
			log.info("Exception loading data. Verify input file " + inputFile.toString());
			log.debug(e1.getMessage());
			throw new WebApplicationException(e1,
					Response.Status.INTERNAL_SERVER_ERROR);
		}
		
		return data;
	}
	
	@Override
	public List<OperatorAssignment> buildResponseData(List<SCHOperatorAssignment> crewAssignments) {
		List<OperatorAssignment> operatorAssignments = new UTSUtil().
	    		listConvertOpAssignTcipToJson(crewDataConverter, crewAssignments);
		return operatorAssignments;
	}

	private OperatorAssignmentData getOpAssignDataObjectForFile(File inputFile, DepotIdTranslator depotIdTranslator) 
				throws FileNotFoundException {
		
		UtsCrewAssignsToDataCreator process = new UtsCrewAssignsToDataCreator(
				inputFile);

		// If depotIdTranslator is null, print a message saying the depot id translation tool is not set up.
		if (depotIdTranslator == null) {
			log.info("DepotIdTranslator has not been set up. Depot IDs will not be translated.");
		} else {
			log.info("Using depot ID translation.");
		}
		process.setDepotIdTranslator(depotIdTranslator);

		return process.generateDataObject();
	}
	
	/**
	 * Injects most recent file picker
	 * @param mostRecentFilePicker the mostRecentFilePicker to set
	 */
	@Autowired
	@Qualifier("crewFilePicker")
	public void setMostRecentFilePicker(MostRecentFilePicker mostRecentFilePicker) {
		this.mostRecentFilePicker = mostRecentFilePicker;
	}

	/**
	 * @param crewDataConverter the crewDataConverter to set
	 */
	@Autowired
	@Qualifier("crewDataConverter")
	public void setCrewDataConverter(
			ModelCounterpartConverter<SCHOperatorAssignment, OperatorAssignment> crewDataConverter) {
		this.crewDataConverter = crewDataConverter;
	}


}
