package org.onebusaway.nyc.webapp.actions.admin.model;

import java.util.List;

public class DestinationSignsMessage {

	private List<DestinationSign> signs;
	private String status;
	
	public List<DestinationSign> getSigns() {
		return signs;
	}
	public void setSigns(List<DestinationSign> signs) {
		this.signs = signs;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
}
