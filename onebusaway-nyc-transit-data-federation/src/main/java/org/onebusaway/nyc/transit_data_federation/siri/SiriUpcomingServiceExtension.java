package org.onebusaway.nyc.transit_data_federation.siri;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlType(name = "ScheduledService", propOrder = {
	    "upcomingScheduledService"
})
public class SiriUpcomingServiceExtension {
	
	@XmlElement(name = "UpcomingScheduledService")
	protected Boolean upcomingScheduledService = null;

	public Boolean hasUpcomingScheduledService() {
        return upcomingScheduledService;
    }

    public void setUpcomingScheduledService(Boolean value) {
        this.upcomingScheduledService = value;
    }

}
