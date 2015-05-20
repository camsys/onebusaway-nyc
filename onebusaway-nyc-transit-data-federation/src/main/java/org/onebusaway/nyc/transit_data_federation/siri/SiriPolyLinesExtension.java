package org.onebusaway.nyc.transit_data_federation.siri;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlType(name = "PolyLines", propOrder = {
	    "polylines"
})
public class SiriPolyLinesExtension {
	
	
	protected List<String> polylines = new ArrayList<String>();
	
	@XmlElement(name = "Polylines")
	public List<String> getPolylines() {
        return polylines;
    }

    public void setPolylines(List<String> value) {
        this.polylines = value;
    }

}
