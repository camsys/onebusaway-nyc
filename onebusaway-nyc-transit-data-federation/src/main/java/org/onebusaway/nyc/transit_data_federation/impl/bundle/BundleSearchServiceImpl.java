package org.onebusaway.nyc.transit_data_federation.impl.bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.services.bundle.BundleSearchService;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BundleSearchServiceImpl implements BundleSearchService {

	@Autowired
	private NycTransitDataService _transitDataService;
	
	private Map<String,List<String>> suggestions = new HashMap<String, List<String>>();
	
	@PostConstruct
	public void init() {
		Map<String, List<CoordinateBounds>> agencies = this._transitDataService.getAgencyIdsWithCoverageArea();
		for (String agency : agencies.keySet()) {
			ListBean<RouteBean> routes = this._transitDataService.getRoutesForAgencyId(agency);
			for (RouteBean route : routes.getList()) {
				String shortName = route.getShortName();
				generateInputsForString(shortName, "\\s+");
			}
			
			ListBean<String> stopIds = this._transitDataService.getStopIdsForAgencyId(agency);
			for (String stopId : stopIds.getList()) {
				stopId = stopId.replace("MTA NYCT_", "");
				generateInputsForString(stopId, null);
			}
		}
	}
	
	private void generateInputsForString(String string, String splitRegex) {
		String[] parts;
		if (splitRegex != null)
			parts = string.split(splitRegex);
		else
			parts = new String[] {string};
		for (String part : parts) {
			int length = part.length();
			for (int i = 0; i < length; i++) {
				String key = part.substring(0, i+1).toLowerCase();
				if (suggestions.get(key) == null) {
					suggestions.put(key, new ArrayList<String>());
				}
				suggestions.get(key).add(string);
				Collections.sort(suggestions.get(key));
			}
		}
	}

	@Override
	public List<String> getSuggestions(String input) {
		List<String> suggestions = this.suggestions.get(input);
		if (suggestions == null)
			suggestions = new ArrayList<String>();
		if (suggestions.size() > 10)
			suggestions = suggestions.subList(0, 10);
		return suggestions;
	}
}
