package org.onebusaway.nyc.transit_data_federation.services.bundle;

import java.util.List;

public interface BundleSearchService {
	
	public List<String> getSuggestions(String input);

}
