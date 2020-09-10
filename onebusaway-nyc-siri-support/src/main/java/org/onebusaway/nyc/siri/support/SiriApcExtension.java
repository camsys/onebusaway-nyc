package org.onebusaway.nyc.siri.support;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Include some additional passenger capacity information
 * not included in the specification.
 */
@XmlRootElement
public class SiriApcExtension {

    private Integer PassengerCount = null;
    private Integer PassengerCapacity = null;

    @XmlElement(name="EstimatedPassengerCount")
    public Integer getPassengerCount() {
        return PassengerCount;
    }

    public void setPassengerCount(Integer count) {
        PassengerCount = count;
    }

    @XmlElement(name="EstimatedPassengerCapacity")
    public Integer getPassengerCapacity() {
        return PassengerCapacity;
    }

    public void setPassengerCapacity(Integer capacity) {
        PassengerCapacity = capacity;
    }
}
