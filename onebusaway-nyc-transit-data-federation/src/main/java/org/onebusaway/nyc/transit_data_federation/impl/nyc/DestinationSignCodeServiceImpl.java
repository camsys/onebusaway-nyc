/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.transit_data_federation.impl.nyc;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.bundle.model.NycFederatedTransitDataBundle;
import org.onebusaway.nyc.transit_data_federation.impl.bundle.NycRefreshableResources;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.transit_data_federation.services.transit_graph.RouteCollectionEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.onebusaway.utility.ObjectSerializationLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class DestinationSignCodeServiceImpl implements DestinationSignCodeService {

  private Logger _log = LoggerFactory.getLogger(DestinationSignCodeServiceImpl.class);

  private Map<String, List<AgencyAndId>> _dscToTripMap;

  private Map<AgencyAndId, String> _tripToDscMap;
  
  private Set<String> _notInServiceDscs;
  
  private TransitGraphDao _transitGraphDao;

  @Autowired
  public void setTransitGraphDao(TransitGraphDao transitGraphDao) {
    _transitGraphDao = transitGraphDao;
  }

  @Autowired
  private NycFederatedTransitDataBundle _bundle;
  
  @PostConstruct
  @Refreshable(dependsOn = NycRefreshableResources.DESTINATION_SIGN_CODE_DATA)
  public void setup() throws IOException, ClassNotFoundException {
  	File dscToTripPath = _bundle.getDSCForTripIndex();
  	if (dscToTripPath.exists()) {
  		_dscToTripMap = ObjectSerializationLibrary.readObject(dscToTripPath);
  	} else {
  		_dscToTripMap = null;
  	}
  
  	File tripToDscPath = _bundle.getTripsForDSCIndex();
  	if (tripToDscPath.exists()) {
  		_tripToDscMap = ObjectSerializationLibrary.readObject(tripToDscPath);
  	} else {
  		_tripToDscMap = null;
  	}
  
  	File notInServiceDSCPath = _bundle.getNotInServiceDSCs();
  	if (notInServiceDSCPath.exists()) {
  		_notInServiceDscs = ObjectSerializationLibrary.readObject(notInServiceDSCPath);
  	} else {
  		_notInServiceDscs = null;
  	}
  }	
	
  @Override
  public List<AgencyAndId> getTripIdsForDestinationSignCode(String destinationSignCode) {
	  return _dscToTripMap.get(destinationSignCode);
  }
  
  @Override
  public Set<AgencyAndId> getRouteCollectionIdsForDestinationSignCode(String destinationSignCode) {
    /*
     * For now we just assume that the mapping is consistent in
     * that a dsc maps to one route-collection-id. 
     */
    Set<AgencyAndId> routeIds = new HashSet<AgencyAndId>();
    if (StringUtils.isNotBlank(destinationSignCode)) {
      List<AgencyAndId> dscTripIds = getTripIdsForDestinationSignCode(destinationSignCode);
      
      if (dscTripIds != null && !dscTripIds.isEmpty()) {
          TripEntry trip = _transitGraphDao.getTripEntryForId(dscTripIds.get(0));
          if(trip == null) {
            _log.warn("No route collection found for trip ID " + dscTripIds.get(0));
            return routeIds;
          }
          
          RouteCollectionEntry route = trip.getRouteCollection();
          routeIds.add(route.getId());
      }
    } 
    
    return routeIds;
  }

  @Override
  public String getDestinationSignCodeForTripId(AgencyAndId tripId) {
	  return _tripToDscMap.get(tripId);
  }

  @Override
  public boolean isOutOfServiceDestinationSignCode(String destinationSignCode) {
	  return _notInServiceDscs.contains(destinationSignCode);
  }

  @Override
  public boolean isMissingDestinationSignCode(String destinationSignCode) {
    return "0000".equals(destinationSignCode)
        || "9999".equals(destinationSignCode)
        || "0".equals(destinationSignCode);
  }

  @Override
  public boolean isUnknownDestinationSignCode(String destinationSignCode) {
	  if(_notInServiceDscs.contains(destinationSignCode) 
			  || _dscToTripMap.containsKey(destinationSignCode))
		  return false;
	  else
		  return true;
  }
}
