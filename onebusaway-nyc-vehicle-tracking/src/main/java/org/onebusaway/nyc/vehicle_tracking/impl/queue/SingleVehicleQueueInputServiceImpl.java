package org.onebusaway.nyc.vehicle_tracking.impl.queue;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.queue.model.RealtimeEnvelope;
import org.onebusaway.nyc.vehicle_tracking.services.queue.InputService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import tcip_final_3_0_5_1.CPTVehicleIden;
import tcip_final_3_0_5_1.CcLocationReport;
@Component
@Qualifier("singleVehicleInputService")
public class SingleVehicleQueueInputServiceImpl extends InputServiceImpl
		implements InputService {
	
	private final String _vehicleId = "MTA NYCT_2827";

	@Override
	public boolean acceptMessage(RealtimeEnvelope envelope) {
		if (envelope == null || envelope.getCcLocationReport() == null)
			return false;

		final CcLocationReport message = envelope.getCcLocationReport();
		final CPTVehicleIden vehicleIdent = message.getVehicle();
		final AgencyAndId vehicleId = new AgencyAndId(
				vehicleIdent.getAgencydesignator(), vehicleIdent.getVehicleId()
						+ "");

		return _vehicleId.equals(vehicleId.toString());
	}

	@Override
	public String replaceMessageContents(String contents) {
		return replaceAllStringOccurances(contents);
	}

	private String replaceAllStringOccurances(String contents) {
		final String[] searchList = new String[] { "vehiclepowerstate" };
		final String[] replacementList = new String[] { "vehiclePowerState" };
		return StringUtils.replaceEach(contents, searchList, replacementList);
	}

	@Override
	@PostConstruct
	public void setup() {
		super.setup();
	}

}
