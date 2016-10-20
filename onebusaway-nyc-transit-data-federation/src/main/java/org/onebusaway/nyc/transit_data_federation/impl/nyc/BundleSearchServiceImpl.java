package org.onebusaway.nyc.transit_data_federation.impl.nyc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.services.nyc.BundleSearchService;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data_federation.impl.RefreshableResources;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Proposes suggestions to the user based on bundle content--e.g. stop ID and route short names.
 * 
 * @author asutula
 *
 */
@Component
public class BundleSearchServiceImpl implements BundleSearchService {

	@Autowired
	private NycTransitDataService _transitDataService = null;

	private Map<String,List<String>> suggestions = Collections.synchronizedMap(new HashMap<String, List<String>>());

	private boolean _disableInit = false;
	
	@PostConstruct
	@Refreshable(dependsOn = { 
		      RefreshableResources.ROUTE_COLLECTIONS_DATA, 
		      RefreshableResources.TRANSIT_GRAPH })
	public void init() {
		if (_disableInit)
			return;
		Runnable initThread = new Runnable() {
			@Override
			public void run() {
				Map<String,List<String>> tmpSuggestions = Collections.synchronizedMap(new HashMap<String, List<String>>());
				

				Map<String, List<CoordinateBounds>> agencies = _transitDataService.getAgencyIdsWithCoverageArea();
				for (String agency : agencies.keySet()) {
					ListBean<RouteBean> routes = _transitDataService.getRoutesForAgencyId(agency);
					for (RouteBean route : routes.getList()) {
						String shortName = route.getShortName();
						generateInputsForString(tmpSuggestions, shortName, "\\s+");
					}

					ListBean<String> stopIds = _transitDataService.getStopIdsForAgencyId(agency);
					for (String stopId : stopIds.getList()) {
						if (_transitDataService.stopHasRevenueService(agency, stopId)) {
							AgencyAndId agencyAndId = AgencyAndIdLibrary.convertFromString(stopId);
							generateInputsForString(tmpSuggestions, agencyAndId.getId(), null);
						}
					}
				}
				suggestions = tmpSuggestions;
			}
		};

		new Thread(initThread).start();
	}

	private void generateInputsForString(Map<String,List<String>> tmpSuggestions, String string, String splitRegex) {
		String[] parts;
		if (splitRegex != null)
			parts = string.split(splitRegex);
		else
			parts = new String[] {string};
		for (String part : parts) {
			int length = part.length();
			for (int i = 0; i < length; i++) {
				String key = part.substring(0, i+1).toLowerCase();
				List<String> suggestion = tmpSuggestions.get(key);
				if (suggestion == null) {
					suggestion = new ArrayList<String>();
				}
				suggestion.add(string);
				Collections.sort(suggestion);
				tmpSuggestions.put(key, suggestion);
			}
		}
	}

	@Override
	public List<String> getSuggestions(String input) {
		List<String> tmpSuggestions = this.suggestions.get(input);
		if (tmpSuggestions == null)
			tmpSuggestions = new ArrayList<String>();
		if (tmpSuggestions.size() > 10)
			tmpSuggestions = tmpSuggestions.subList(0, 10);
		return tmpSuggestions;
	}
	
	public void setDisableInit(boolean disableInit) {
		_disableInit = disableInit;
	}
}
