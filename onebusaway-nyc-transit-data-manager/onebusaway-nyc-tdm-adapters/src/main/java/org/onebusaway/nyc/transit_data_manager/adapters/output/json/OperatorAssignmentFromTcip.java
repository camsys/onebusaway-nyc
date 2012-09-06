package org.onebusaway.nyc.transit_data_manager.adapters.output.json;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.onebusaway.nyc.transit_data_manager.adapters.ModelCounterpartConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.UTSUtil;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.OperatorAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.TcipMappingTool;
import org.springframework.beans.factory.annotation.Autowired;

import tcip_final_3_0_5_1.SCHOperatorAssignment;

/**
 * Contains convert function to convert TCIP Operator assignments
 * (SCHOperatorAssignment) to Json model operator assignments
 * (OperatorAssignment).
 * 
 * @author sclark
 * 
 */
public class OperatorAssignmentFromTcip implements ModelCounterpartConverter<SCHOperatorAssignment, OperatorAssignment> {

	public OperatorAssignment convert(SCHOperatorAssignment input) {
		OperatorAssignment opAssign = new OperatorAssignment();
		UTSUtil util = new UTSUtil();
		TcipMappingTool mappingTool = new TcipMappingTool();

		opAssign.setAgencyId(mappingTool.getJsonModelAgencyIdByTcipId(input.getOperator().getAgencyId()));
		opAssign.setPassId(util.stripLeadingCharacters(mappingTool.cutPassIdFromOperatorDesignator(input.getOperator().getDesignator())));

		opAssign.setRunRoute(input.getLocalSCHOperatorAssignment().getRunRoute());
		opAssign.setRunNumber(mappingTool.cutRunNumberFromTcipRunDesignator(input.getRun().getDesignator()));

		opAssign.setDepot(input.getOperatorBase().getFacilityName());

		DateTimeFormatter xmlDTF = TcipMappingTool.TCIP_DATETIME_FORMATTER;
		DateTime serviceDate = xmlDTF.parseDateTime(input.getMetadata().getEffective());

		DateTimeFormatter shortDateDTF = TcipMappingTool.TCIP_DATEONLY_FORMATTER;
		opAssign.setServiceDate(shortDateDTF.print(serviceDate));

		DateTimeFormatter jsonUpdateDTF = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ");
		opAssign.setUpdated(jsonUpdateDTF.print(xmlDTF.parseDateTime(input.getMetadata().getUpdated())));

		return opAssign;
	}

}
