package org.onebusaway.nyc.vehicle_tracking.impl.queue;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.queue.model.RealtimeEnvelope;
import org.onebusaway.nyc.vehicle_tracking.services.queue.InputService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Qualifier("queueInputService")
public class InputQueueServiceImpl extends InputServiceImpl implements
		InputService {

	@Override
	public String replaceMessageContents(String contents) {
		return replaceAllStringOccurances(contents);
	}
	
	private String replaceAllStringOccurances(String contents){
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
