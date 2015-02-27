package org.onebusaway.nyc.transit_data_federation.siri;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DelaysStructure", propOrder = {
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
