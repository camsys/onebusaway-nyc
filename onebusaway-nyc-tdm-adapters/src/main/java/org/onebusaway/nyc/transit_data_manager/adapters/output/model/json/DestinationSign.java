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