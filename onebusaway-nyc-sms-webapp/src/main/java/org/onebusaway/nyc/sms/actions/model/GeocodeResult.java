package org.onebusaway.nyc.sms.actions.model;

import java.io.Serializable;

import org.onebusaway.geocoder.enterprise.services.EnterpriseGeocoderResult;
import org.onebusaway.nyc.presentation.model.SearchResult;

/**
 * Ambiguous address top-level search result.
 * 
 * @author jmaki
 * 
 */
public class GeocodeResult implements SearchResult, Serializable {

	private static final long serialVersionUID = 1L;

	private EnterpriseGeocoderResult result;

	public GeocodeResult(EnterpriseGeocoderResult result) {
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
}
