package org.onebusaway.nyc.siri.support;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Include some additional vehicle information
 * not included in the specification.
 */
@XmlRootElement
public class SiriVehicleFeatures {

    private boolean kneelingVehicle;

    @XmlElement(name="KneelingVehicle")
    public Boolean getKneelingVehicle() {
        return kneelingVehicle;
    }

    public void setKneelingVehicle(boolean kneelingVehicle){
        this.kneelingVehicle = kneelingVehicle;}
}
