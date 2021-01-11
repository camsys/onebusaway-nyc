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
