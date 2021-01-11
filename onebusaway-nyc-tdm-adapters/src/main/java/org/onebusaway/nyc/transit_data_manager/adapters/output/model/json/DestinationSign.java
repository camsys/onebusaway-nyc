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

package org.onebusaway.nyc.transit_data_manager.adapters.output.model.json;

public class DestinationSign {
	private Long messageId;
	private String routeName;
	private String messageText;
	private String agency;

	public Long getMessgeId() {
		return messageId;
	}
	
	public void setMessageId(Long messageId) {
		this.messageId = messageId;
	}

	public String getRouteName() {
		return routeName;
	}
	
	public void setRouteName(String routeName) {
		this.routeName = routeName;
	}

	public Object getMessageText() {
		return messageText;
	}
	
	public void setMessageText(String messageText) {
		this.messageText = messageText;
	}

	public String getAgency() {
		return agency;
	}
	
	public void setAgency(String agencydesignator) {
		this.agency = agencydesignator;
	}



}