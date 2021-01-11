/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onebusaway.nyc.transit_data_manager.adapters.output.json;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.onebusaway.nyc.transit_data_manager.adapters.ModelCounterpartConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.OperatorAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.TcipMappingTool;

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
		TcipMappingTool mappingTool = new TcipMappingTool();

		opAssign.setAgencyId(mappingTool.getJsonModelAgencyIdByTcipId(input.getOperator().getAgencyId()));
		opAssign.setPassId(new Long(input.getOperator().getOperatorId()).toString());

		opAssign.setRunRoute(input.getLocalSCHOperatorAssignment().getRunRoute());
		opAssign.setRunNumber(mappingTool.cutRunNumberFromTcipRunDesignator(input.getRun().getDesignator()));
		// obanyc-1278
		if ("MISC".equals(opAssign.getRunRoute())) {
			opAssign.setRunId(input.getLocalSCHOperatorAssignment().getRunRoute() 
					+ "-" + input.getOperatorBase().getFacilityName()
					+ "-" + mappingTool.cutRunNumberFromTcipRunDesignator(input.getRun().getDesignator()));
		} else {
			opAssign.setRunId(input.getLocalSCHOperatorAssignment().getRunRoute() 
					+ "-" + mappingTool.cutRunNumberFromTcipRunDesignator(input.getRun().getDesignator()));
		}
		
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
