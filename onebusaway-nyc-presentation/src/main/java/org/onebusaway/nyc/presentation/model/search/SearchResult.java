package org.onebusaway.nyc.presentation.model.search;

import java.util.List;

import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;

/**
 * marker interface to allow for route or stop search results
 */
public interface SearchResult {
	 public String getName();
	 public String getType();
	 public List<NaturalLanguageStringBean> getServiceAlerts();
}
