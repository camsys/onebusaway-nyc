package org.onebusaway.nyc.transit_data_federation.siri;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SiriUpcomingServiceExtension {

	private Boolean upcomingScheduledService = null;

	@XmlElement(name = "UpcomingScheduledService")
	public Boolean hasUpcomingScheduledService() {
        return upcomingScheduledService;
    }

    public void setUpcomingScheduledService(Boolean value) {
        this.upcomingScheduledService = value;
    }

}
