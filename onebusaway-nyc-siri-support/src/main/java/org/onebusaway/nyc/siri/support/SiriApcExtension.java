package org.onebusaway.nyc.siri.support;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Include some additional passenger capacity information
 * not included in the specification.
 */
@XmlRootElement
public class SiriApcExtension {

    private Integer passengerCount = null;
    private Integer passengerCapacity = null;
    private String passengerLoadFactor = null;

    @XmlElement(name="EstimatedPassengerCount")
    public Integer getPassengerCount() {
        return passengerCount;
    }

    public void setPassengerCount(Integer count) {
        passengerCount = count;
    }

    @XmlElement(name="EstimatedPassengerCapacity")
    public Integer getPassengerCapacity() {
        return passengerCapacity;
    }

    public void setPassengerCapacity(Integer capacity) {
        passengerCapacity = capacity;
    }

    @XmlElement(name="EstimatedPassengerLoadFactor")
    public String getOccupancyLoadFactor() {
        return passengerLoadFactor;
    }
    public void setOccupancyLoadFactor(String loadFactorString) {
        passengerLoadFactor = loadFactorString;
    }
}
