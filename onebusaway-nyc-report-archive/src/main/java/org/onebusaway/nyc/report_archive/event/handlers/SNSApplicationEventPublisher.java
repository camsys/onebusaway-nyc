package org.onebusaway.nyc.report_archive.event.handlers;

import org.onebusaway.nyc.report_archive.event.SNSApplicationEvent;
import org.onebusaway.nyc.report_archive.event.SNSApplicationEventData;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

/**
 * Publishes SNS application event for amazon SNS to be notified.
 * @author abelsare
 *
 */
public class SNSApplicationEventPublisher implements ApplicationEventPublisherAware{

	private ApplicationEventPublisher applicationEventPublisher;
	private SNSApplicationEventData data;
	
	
	public void run() {
		SNSApplicationEvent applicationEvent = new SNSApplicationEvent(this);
		applicationEvent.setData(data);
		applicationEventPublisher.publishEvent(applicationEvent);
	}
	
	@Override
	public void setApplicationEventPublisher(
			ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	/**
	 * @param data the data to set
	 */
	public void setData(SNSApplicationEventData data) {
		this.data = data;
	}

}
