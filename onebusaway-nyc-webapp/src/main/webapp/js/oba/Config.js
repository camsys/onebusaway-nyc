// Copyright 2010, OpenPlans
// Licensed under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

var OBA = window.OBA || {};

OBA.Config = {
    debug: true,

    // urls to fetch various data
    searchUrl: "search.action",
    routeShapeUrl: "/onebusaway-api-webapp/api/where/stops-for-route",
    stopsUrl: "/onebusaway-api-webapp/api/where/stops-for-location.json",
    stopUrl: "/onebusaway-api-webapp/api/where/arrivals-and-departures-for-stop",
    vehiclesUrl:"/onebusaway-api-webapp/api/where/trips-for-route",
    vehicleUrl: "/onebusaway-api-webapp/api/where/trip-for-vehicle",
    
    // milliseconds to wait in between polls for bus locations
    pollingInterval: 5000,

    // marker images used
    vehicleIcon: new google.maps.MarkerImage("img/vehicle/vehicle-unknown.png",
            new google.maps.Size(20, 20),
            new google.maps.Point(0,0),
            new google.maps.Point(10, 10)),

    stopIconFilePrefix: "img/stop/stop",
    stopIconFileType: "png",
    stopIconSize: new google.maps.Size(24,24),
    stopIconCenter: new google.maps.Point(12,12),
    
    // api key used for webapp
    apiKey: "TEST",
    
    vehicleFilterFunction: function(tripStatus) {
    	if(tripStatus === null)
    		return false;
    	
    	// don't show non-realtime trips (row 8)
    	if(tripStatus.predicted === false || tripStatus.distanceAlongTrip === 0)
    		return false;
    	
    	var status = ((typeof tripStatus.status !== 'undefined' && tripStatus.status !== '') ? tripStatus.status : null);
    	var phase = ((typeof tripStatus.phase !== 'undefined' && tripStatus.phase !== '') ? tripStatus.phase : null);
    	
    	// hide disabled vehicles (row 7)
    	if(status !== null && status.toLowerCase() === 'disabled')
    		return false;
    	
    	// hide deadheading vehicles (row 3)
    	// hide vehicles at the depot (row 1)
    	if(phase !== null && phase.toLowerCase() !== 'in_progress')
    		return false;

  	  	// hide data >= 5m old (row 5)
    	if(typeof tripStatus.lastUpdateTime !== 'undefined' && new Date().getTime() - tripStatus.lastUpdateTime >= 1000 * 60 * 5)
    		return false;
  	  
    	return true;
    }
};
