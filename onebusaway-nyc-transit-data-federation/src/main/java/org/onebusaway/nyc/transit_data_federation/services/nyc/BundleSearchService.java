package org.onebusaway.nyc.transit_data_federation.services.nyc;

import java.util.List;

public interface BundleSearchService {
	
	public List<String> getSuggestions(String input);

}
