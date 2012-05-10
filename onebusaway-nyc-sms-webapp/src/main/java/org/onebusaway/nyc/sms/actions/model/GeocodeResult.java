package org.onebusaway.nyc.sms.actions.model;

import java.io.Serializable;

import org.onebusaway.nyc.geocoder.service.NycGeocoderResult;
import org.onebusaway.nyc.presentation.model.SearchResult;

/**
 * Ambiguous address top-level search result.
 * 
 * @author jmaki
 * 
 */
public class GeocodeResult implements SearchResult, Serializable {

	private static final long serialVersionUID = 1L;

	private NycGeocoderResult result;

	public GeocodeResult(NycGeocoderResult result) {
		this.result = result;
	}

	public String getFormattedAddress() {
		return result.getFormattedAddress();
	}

	public Double getLatitude() {
		return result.getLatitude();
	}

	public Double getLongitude() {
		return result.getLongitude();
	}

	public String getNeighborhood() {
		return result.getNeighborhood();
	}

	public Boolean isRegion() {
		return result.isRegion();
	}

	@Override
	public void setDistanceToQueryLocation(Double distance) {
		// TODO Auto-generated method stub

	}

	@Override
	public Double getDistanceToQueryLocation() {
		// TODO Auto-generated method stub
		return null;
	}

}
