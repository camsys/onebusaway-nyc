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

package org.onebusaway.nyc.report_archive.event;

import org.springframework.context.ApplicationEvent;

/**
 * Application event dispatched when amazon sns service needs to be notified.
 * @author abelsare
 *
 */
public class SNSApplicationEvent extends ApplicationEvent{

	private static final long serialVersionUID = 1L;
	
	private SNSApplicationEventData data;

	public SNSApplicationEvent(Object source) {
		super(source);
	}

	/**
	 * Returns the data that the listener should act on
	 * @return the data
	 */
	public SNSApplicationEventData getData() {
		return data;
	}

	/**
	 * Sets the data on this event
	 * @param data the data to set
	 */
	public void setData(SNSApplicationEventData data) {
		this.data = data;
	}

}
