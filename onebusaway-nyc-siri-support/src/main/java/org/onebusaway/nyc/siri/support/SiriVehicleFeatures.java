package org.onebusaway.nyc.siri.support;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Include some additional vehicle information
 * not included in the specification.
 */
@XmlRootElement
public class SiriVehicleFeatures {

    private boolean strollerVehicle;

    @XmlElement(name="StrollerVehicle")
    public Boolean getStrollerVehicle() {
        return strollerVehicle;
    }

    public void setStrollerVehicle(boolean strollerVehicle){
        this.strollerVehicle = strollerVehicle;}
}
