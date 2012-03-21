/*
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

var OBA = window.OBA || {};

// do not add constructor params here!
OBA.Popups = (function() {	

	var infoWindow = null;

	function closeInfoWindow() {
		if(infoWindow !== null) {
			infoWindow.close();
		}
		infoWindow = null;
	}
	
	// PUBLIC METHODS
	function showPopupWithContent(map, marker, content) {
		closeInfoWindow();
		
		infoWindow = new google.maps.InfoWindow({
		    	pixelOffset: new google.maps.Size(0, (marker.getIcon().size.height / 2)),
		    	disableAutoPan: false
		});
		
		google.maps.event.addListener(infoWindow, "closeclick", closeInfoWindow);

		infoWindow.setContent(content);
		infoWindow.open(map, marker);
	}
	
	function showPopupWithContentFromRequest(map, marker, url, params, contentFn) {
		closeInfoWindow();
		
		infoWindow = new google.maps.InfoWindow({
	    	pixelOffset: new google.maps.Size(0, (marker.getIcon().size.height / 2)),
	    	disableAutoPan: false,
	    	stopId: marker.stopId // to lock an icon on the map when a popup is open for it
		});

		google.maps.event.addListener(infoWindow, "closeclick", closeInfoWindow);

		var popupContainerId = "container" + Math.floor(Math.random() * 1000000);
		var refreshFn = function(openBubble) {
			// pass a new "now" time for debugging if we're given one
			if(OBA.Config.time !== null) {
				params.time = OBA.Config.time;
			}
			
			jQuery.getJSON(url, params, function(json) {
				if(infoWindow === null) {
					return;
				}
				
				infoWindow.setContent(contentFn(json, popupContainerId, marker));
				
				if(openBubble === true) {
					infoWindow.open(map, marker);
				}
			});
		};
		refreshFn(true);		
		infoWindow.refreshFn = refreshFn;	

		var updateTimestamp = function() {
			var timestampContainer = jQuery("#" + popupContainerId).find(".updated");
			
			if(timestampContainer.length === 0) {
				return;
			}
			
			var age = parseInt(timestampContainer.attr("age"), 10);
			var referenceEpoch = parseInt(timestampContainer.attr("referenceEpoch"), 10);
			var newAge = age + ((new Date().getTime() - referenceEpoch) / 1000);
			timestampContainer.text("Data updated " + OBA.Util.displayTime(newAge));
		};
		updateTimestamp();		
		infoWindow.updateTimestamp = updateTimestamp;
	}
	
	// CONTENT GENERATION
	function getServiceAlerts(r, situationRefs) {
	    var html = '';

	    var situationIds = {};
        var situationRefsCount = 0; 
        if (situationRefs != null) {
            jQuery.each(situationRefs, function(_, situation) {
                situationIds[situation.SituationSimpleRef] = true;
                situationRefsCount += 1;
            });
        }
        
        if (situationRefs == null || situationRefsCount > 0) {
            if (r.Siri.ServiceDelivery.SituationExchangeDelivery != null) {
                jQuery.each(r.Siri.ServiceDelivery.SituationExchangeDelivery[0].Situations.PtSituationElement, function(_, ptSituationElement) {
                    var situationId = ptSituationElement.SituationNumber;
                    if (situationRefs == null || situationIds[situationId] === true) {
                        html += '<li>' + ptSituationElement.Description.replace(/\n/g, "<br/>") + '</li>';
                    }
                });
            }
        }
        
        if (html !== '') {
            html = '<p class="service-alert title">Service Change:</p><ul class="service-alert">' + html + '</ul>';
        }
        
        return html;
	}
	
	function getVehicleContentForResponse(r, popupContainerId, marker) {
		var activity = r.Siri.ServiceDelivery.VehicleMonitoringDelivery[0].VehicleActivity[0];
		if(activity === null || activity.MonitoredVehicleJourney === null) {
			return null;
		}

		var vehicleId = activity.MonitoredVehicleJourney.VehicleRef;
		var vehicleIdParts = vehicleId.split("_");
		var vehicleIdWithoutAgency = vehicleIdParts[1];

		var routeId = activity.MonitoredVehicleJourney.LineRef;
		var routeIdParts = routeId.split("_");
		var routeIdWithoutAgency = routeIdParts[1];

		var html = '<div id="' + popupContainerId + '" class="popup">';
		
		// header
		html += '<div class="header vehicle">';
		html += '<p class="title">' + activity.MonitoredVehicleJourney.PublishedLineName + " " + activity.MonitoredVehicleJourney.DestinationName + '</p><p>';
		html += '<span class="type">Vehicle #' + vehicleIdWithoutAgency + '</span>';

		var updateTimestamp = OBA.Util.ISO8601StringToDate(activity.RecordedAtTime).getTime();
		var updateTimestampReference = OBA.Util.ISO8601StringToDate(r.Siri.ServiceDelivery.ResponseTimestamp).getTime();

		var age = (parseInt(updateTimestampReference, 10) - parseInt(updateTimestamp, 10)) / 1000;
		var staleClass = ((age > OBA.Config.staleTimeout) ? " stale" : "");			

		html += '<span class="updated' + staleClass + '"' + 
				' age="' + age + '"' + 
				' referenceEpoch="' + new Date().getTime() + '"' + 
				'>Data updated ' 
				+ OBA.Util.displayTime(age) 
				+ '</span>'; 
		
		// (end header)
		html += '</p>';
		html += '</div>';
		
		// service alerts
		html += getServiceAlerts(r, activity.MonitoredVehicleJourney.SituationRef);		
		
		// service available at stop
		if(typeof activity.MonitoredVehicleJourney.MonitoredCall === 'undefined' 
			&& (typeof activity.MonitoredVehicleJourney.OnwardCalls === 'undefined'
			|| typeof activity.MonitoredVehicleJourney.OnwardCalls.OnwardCall === 'undefined')) {

			html += '<p class="service">Next stops are not known for this vehicle.</p>';
		} else {
			var nextStops = [];
			nextStops.push(activity.MonitoredVehicleJourney.MonitoredCall);
			
			if(typeof activity.MonitoredVehicleJourney.OnwardCalls !== 'undefined'
				&& typeof activity.MonitoredVehicleJourney.OnwardCalls.OnwardCall !== 'undefined') {

				jQuery.each(activity.MonitoredVehicleJourney.OnwardCalls.OnwardCall, function(_, onwardCall) {
					if(nextStops.length >= 3) {
						return false;
					}
					nextStops.push(onwardCall);
				});
			}
		
			html += '<p class="service">Next stops:</p>';
			html += '<ul>';			
			jQuery.each(nextStops, function(_, call) {
				var stopIdParts = call.StopPointRef.split("_");
				var stopIdWithoutAgencyId = stopIdParts[1];
				
				html += '<li class="nextStop">';				
				html += '<a href="#' + stopIdWithoutAgencyId + '">' + call.StopPointName + '</a>';
				html += '<span>';
				html +=   call.Extensions.Distances.PresentableDistance;
				html += '</span></li>';
			});
			html += '</ul>';
		}
		
		html += OBA.Config.infoBubbleFooterFunction('route', activity.MonitoredVehicleJourney.PublishedLineName);			
		
		// (end popup)
		html += '</div>';
		
		return html;
	}

	function getStopContentForResponse(r, popupContainerId, marker) {
		var siri = r.siri;
		var stopResult = r.stop;
		
		var stopId = stopResult.id;
		var stopIdParts = stopId.split("_");
		var stopIdWithoutAgency = stopIdParts[1];

		var html = '<div id="' + popupContainerId + '" class="popup">';
		
		// header
		html += '<div class="header stop">';
		html += '<p class="title">' + stopResult.name + '</p><p>';
		html += '<span class="type">Stop #' + stopIdWithoutAgency + '</span>';
		
		// update time across all arrivals
		var updateTimestampReference = OBA.Util.ISO8601StringToDate(siri.Siri.ServiceDelivery.ResponseTimestamp).getTime();
		var maxUpdateTimestamp = null;
		jQuery.each(siri.Siri.ServiceDelivery.StopMonitoringDelivery[0].MonitoredStopVisit, function(_, monitoredJourney) {
			var updateTimestamp = OBA.Util.ISO8601StringToDate(monitoredJourney.RecordedAtTime).getTime();
			if(updateTimestamp > maxUpdateTimestamp) {
				maxUpdateTimestamp = updateTimestamp;
			}
		});		
		
		if(maxUpdateTimestamp !== null) {
			var age = (parseInt(updateTimestampReference, 10) - parseInt(maxUpdateTimestamp, 10)) / 1000;
			var staleClass = ((age > OBA.Config.staleTimeout) ? " stale" : "");

			html += '<span class="updated' + staleClass + '"' + 
					' age="' + age + '"' + 
					' referenceEpoch="' + new Date().getTime() + '"' + 
					'>Data updated ' 
					+ OBA.Util.displayTime(age) 
					+ '</span>'; 
		}
		
		// (end header)
		html += '  </p>';
		html += ' </div>';
		
		// service alerts
	    html += getServiceAlerts(siri, null);

	    var routeAndDirectionWithArrivals = {};
	    var routeAndDirectionWithArrivalsCount = 0;
	    var routeAndDirectionWithoutArrivals = {};
	    var routeAndDirectionWithoutArrivalsCount = 0;
	    var routeAndDirectionWithoutSerivce = {};
	    var routeAndDirectionWithoutSerivceCount = 0;
	    
	    // break up routes here between those with and without service
	    jQuery.each(stopResult.routesAvailable, function(_, route) {
	    	jQuery.each(route.directions, function(__, direction) {
	    		if(direction.hasUpcomingScheduledService === false) {
	    			routeAndDirectionWithoutSerivce[route.id + "_" + direction.directionId] = { "id":route.id, "shortName":route.shortName, "destination":direction.destination };
	    			routeAndDirectionWithoutSerivceCount++;
	    		} else {
	    			routeAndDirectionWithoutArrivals[route.id + "_" + direction.directionId] = { "id":route.id, "shortName":route.shortName, "destination":direction.destination };
	    			routeAndDirectionWithoutArrivalsCount++;
	    		}
	    	});
	    });
	    
	    // ...now those with and without arrivals
	    var visits = siri.Siri.ServiceDelivery.StopMonitoringDelivery[0].MonitoredStopVisit;
	    jQuery.each(visits, function(_, monitoredJourney) {
			var routeId = monitoredJourney.MonitoredVehicleJourney.LineRef;
			var directionId = monitoredJourney.MonitoredVehicleJourney.DirectionRef;

			if(typeof routeAndDirectionWithArrivals[routeId + "_" + directionId] === 'undefined') {
				routeAndDirectionWithArrivals[routeId + "_" + directionId] = [];
				delete routeAndDirectionWithoutArrivals[routeId + "_" + directionId];
				routeAndDirectionWithoutArrivalsCount--;
			}

			routeAndDirectionWithArrivals[routeId + "_" + directionId].push(monitoredJourney.MonitoredVehicleJourney);
			routeAndDirectionWithArrivalsCount++;
		});	    
		
	    // service available
		html += '<p class="service">Service available at this stop:</p>';

		if(routeAndDirectionWithArrivalsCount > 0) {
			jQuery.each(routeAndDirectionWithArrivals, function(_, mvjs) {
				var mvj = mvjs[0];

				html += '<ul>';

				html += '<li class="route">';
				html += '<a href="#' + mvj.PublishedLineName + '">' + mvj.PublishedLineName + " " + mvj.DestinationName + '</a>';
				html += '</li>';

				jQuery.each(mvjs, function(_, monitoredVehicleJourney) {
					if(_ >= 3) {
						return false;
					}

					if(typeof monitoredVehicleJourney.MonitoredCall !== 'undefined') {
						var distance = monitoredVehicleJourney.MonitoredCall.Extensions.Distances.PresentableDistance;

						if(typeof monitoredVehicleJourney.ProgressStatus !== 'undefined' && 
								monitoredVehicleJourney.ProgressStatus === "layover") {
							distance += " (at terminal)";
						}

						var lastClass = ((_ === 2 || _ === mvjs.length - 1) ? " last" : "");
						html += '<li class="arrival' + lastClass + '">' + distance + '</li>';
					}
				});
			});
		}
		
		if(routeAndDirectionWithoutArrivalsCount > 0) {
			html += '<ul>';
			jQuery.each(routeAndDirectionWithoutArrivals, function(_, d) {
				html += '<li class="route">';
				html += '<a href="#' + d.shortName + '">' + d.shortName + " " + d.destination + '</a>';
				html += '</li>';
			});
			html += '</ul>';
			html += '<p class="service">No buses en-route. Check back shortly for an update.</p>';
		}

		if(routeAndDirectionWithoutSerivceCount > 0) {
			html += '<ul>';
			jQuery.each(routeAndDirectionWithoutSerivce, function(_, d) {
				html += '<li class="route">';
				html += '<a href="#' + d.shortName + '">' + d.shortName + " " + d.destination + '</a>';
				html += '</li>';
			});
			html += '</ul>';
			html += '<p class="service">No scheduled service at this time.</p>';
		}

		html += OBA.Config.infoBubbleFooterFunction("stop", stopIdWithoutAgency);	        

		html += "<ul class='links'>";
		html += "<a href='#' id='zoomHere'>Center and Zoom Here</a>";
		html += "</ul>";
		
		// (end popup)
		html += '</div>';

		var content = jQuery(html);
		var zoomHereLink = content.find("#zoomHere");

		zoomHereLink.click(function(e) {
			e.preventDefault();
			
			var map = marker.map;
			map.setCenter(marker.getPosition());
			map.setZoom(16);
		});
		
		return content.get(0);
	}

	//////////////////// CONSTRUCTOR /////////////////////

	// timer to update data periodically 
	setInterval(function() {
		if(infoWindow !== null && typeof infoWindow.refreshFn === 'function') {
			infoWindow.refreshFn();
		}
	}, OBA.Config.refreshInterval);

	// updates timestamp in popup bubble every second
	setInterval(function() {
		if(infoWindow !== null && typeof infoWindow.updateTimestamp === 'function') {
			infoWindow.updateTimestamp();
		}
	}, 1000);
	
	return {
		reset: function() {
			closeInfoWindow();
		},
		
		getPopupStopId: function() {
			if(infoWindow !== null) {
				return infoWindow.stopId;
			} else {
				return null;
			}
		},
		
		// WAYS TO CREATE/DISPLAY A POPUP
		showPopupWithContent: showPopupWithContent, 
		
		showPopupWithContentFromRequest: showPopupWithContentFromRequest,
		
		// CONTENT METHODS
		getVehicleContentForResponse: getVehicleContentForResponse,
		
		getStopContentForResponse: getStopContentForResponse
	};
})();
