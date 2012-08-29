package org.onebusaway.nyc.report_archive.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.report_archive.event.handlers.SNSApplicationEventPublisher;
import org.onebusaway.nyc.report_archive.model.CcLocationReportRecord;
import org.onebusaway.nyc.report_archive.services.EmergencyStatusNotificationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Default implementation of {@link EmergencyStatusNotificationService}
 * @author abelsare
 *
 */
public class EmergencyStatusNotificationServiceImpl implements EmergencyStatusNotificationService {

	private SNSApplicationEventPublisher snsEventPublisher;
	private Map<Integer, Boolean> emergencyState = Collections.synchronizedMap(new HashMap<Integer, Boolean>());
	
	@Override
	public void process(CcLocationReportRecord record) {
		Boolean emergencyStatusChange = Boolean.FALSE;
		Boolean isEmergency = Boolean.FALSE;
		Integer vehicleId = record.getVehicleId();
		String emergencyCode = record.getEmergencyCode();
		
		if(StringUtils.isNotBlank(emergencyCode) && StringUtils.equals(emergencyCode, "1")) {
			isEmergency = Boolean.TRUE;
		}
		
		if(emergencyState.containsKey(vehicleId)) {
			//Check if emergency status has changed 
			Boolean existingEmergency = emergencyState.get(vehicleId);
			if(!(existingEmergency.equals(isEmergency))) {
				emergencyStatusChange = Boolean.TRUE;
				//Add the record with current emergency code
				emergencyState.put(vehicleId, isEmergency);
			}
		} else {
			emergencyState.put(vehicleId, isEmergency);
			if(isEmergency) {
				emergencyStatusChange = Boolean.TRUE;
			}
		}
		
		//Publish sns application event if emergency state has changed
		if(emergencyStatusChange) {
			snsEventPublisher.setData(record);
			snsEventPublisher.run();
		}
	}

	/**
	 * @param snsEventPublisher the snsEventPublisher to set
	 */
	@Autowired
	public void setSnsEventPublisher(SNSApplicationEventPublisher snsEventPublisher) {
		this.snsEventPublisher = snsEventPublisher;
	}

	//Use only for unit test
	protected void setEmergencyState(Map<Integer, Boolean> emergencyState) {
		this.emergencyState = emergencyState;
	}

}
