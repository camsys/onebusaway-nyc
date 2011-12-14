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

OBA.Sign = function() {
	
	var refreshInterval = 30;
	var timeout = 30;
	var configurableMessageHtml = null;
	var stopIdsToRequest = null;
	var vehiclesPerStop = null;
	var tisMode = null;
	
	function getParameterByName(name, defaultValue) {
		name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
		var regexS = "[\\?&]"+name+"=([^&#]*)";
		var regex = new RegExp(regexS);
		var results = regex.exec(window.location.href);
		if(results == null) {
			return defaultValue;
		} else {
			return decodeURIComponent(results[1].replace(/\+/g, " "));
		}
	}

	function detectSize() {
		var wrappedWindow = jQuery(window);
		var h = wrappedWindow.height();
		var w = wrappedWindow.width();

		// special mode for MTA TIS display
		if(tisMode === "true") {
			vehiclesPerStop = 3;
			jQuery('body').removeClass().addClass('landscape').addClass('sizeTIS');
			return;
		}
		
		if(w > h) {
			vehiclesPerStop = 3;
			if(h >= 1150) {
				jQuery('body').removeClass().addClass('landscape').addClass('size1200');
			} else if(h >= 1000) {
				jQuery('body').removeClass().addClass('landscape').addClass('size1000');
			} else if(h >= 700) {
				jQuery('body').removeClass().addClass('landscape').addClass('size700');			
			} else {
				jQuery('body').removeClass().addClass('landscape');			
			}			
		} else {
			vehiclesPerStop = 6;
			if(h >= 1900) {
				jQuery('body').removeClass().addClass('portrait').addClass('size1900');
			} else if(h >= 1500) {
				jQuery('body').removeClass().addClass('portrait').addClass('size1600');
			} else if(h >= 1200) {
				jQuery('body').removeClass().addClass('portrait').addClass('size1200');
			} else if(h >= 1000) {
				jQuery('body').removeClass().addClass('portrait').addClass('size1000');
			} else if(h >= 700) {
				jQuery('body').removeClass().addClass('portrait').addClass('size700');			
			} else {
				jQuery('body').removeClass().addClass('portrait');			
			}			
		}
	}
	
	function setupUI() {
		// configure interface with URL params
		tisMode = getParameterByName("tisMode", tisMode);
		
		refreshInterval = getParameterByName("refresh", refreshInterval);

		timeout = refreshInterval;
			
		configurableMessageHtml = getParameterByName("message", configurableMessageHtml);
		if(configurableMessageHtml !== null) {
			var header = jQuery("#branding");

			jQuery("<p></p>")
					.attr("id", "message")
					.text(configurableMessageHtml)
					.appendTo(header);
		}

		var stopIds = getParameterByName("stopIds", null);		
		if(stopIds !== null) {
			stopIdsToRequest = [];
			jQuery.each(stopIds.split(","), function(_, o) {
				stopIdsToRequest.push(o);
			});
		} else {
			showError("No stops are configured for display.");
		}
		
		// add event handlers
		detectSize();
		jQuery.event.add(window, "resize", detectSize);
		
		// setup error handling/timeout
		jQuery.ajaxSetup({
			"error": showError,
			"timeout": timeout * 1000000,
			"cache": false
		});

		jQuery("#arrivals")
				.html("")
				.empty();

		update();
		setInterval(update, refreshInterval * 1000);
	}

	function getNewTableForStop(stopId, stopName) {
		var table = jQuery("<table></table>")
						.addClass("stop" + stopId);

		jQuery('<thead>' + 
					'<tr>' + 
						'<th class="stop">' + 
							'<span class="name">' + stopName + '</span>' + 
							' <span class="stop-id">Stop #' + stopId + '</span>' + 
						'</th>' + 
						'<th class="instructions">' + 
							OBA.Config.infoBubbleFooterFunction("sign", stopId) +
						'</th>' +
						'<th class="qr"></th>' +
					'</tr>' + 
				 '</thead>')
				 .appendTo(table);

		jQuery("<tbody></tbody>")
				 .appendTo(table);
		
		_gaq.push(['_trackEvent', "DIY Sign", "Add Stop", stopId]);
		
		return table;
	}
	
	function updateTableForStop(stopTable, applicableSituations, headsignToDistanceAways) {
		if(stopTable === null) {
			return;
		}
		
		var tableBody = stopTable.find("tbody");
		tableBody
			.html("")
			.empty();

		var tableHeader = stopTable.find("thead tr th");
		tableHeader
			.find("p.alert")
			.html("")
			.remove();

		// situations
		jQuery.each(applicableSituations, function(situationId, situation) {
			var alert = jQuery("<p></p>")
							.addClass("alert")
							.html(situation.Description.replace(/\n/g, "<br/>"));
			
			stopTable.find("thead tr th.stop").append(alert);
		});
		
		// arrivals
		jQuery.each(headsignToDistanceAways, function(headsign, distanceAways) {
			if(distanceAways.length === 0) {
				jQuery('<tr class="last">' + 
						'<td colspan="3">' + 
							'OneBusAway NYC is not tracking any buses en-route to this stop. Please check back shortly for an update.</li>' +
						'</td>' +
					   '</tr>')
					   .appendTo(tableBody);
			} else {			
				jQuery.each(distanceAways, function(_, vehicleInfo) {
					if((_ + 1) > vehiclesPerStop) {
						return;
					}
						
					var row = jQuery("<tr></tr>");

					if((_ + 1) === vehiclesPerStop || (_ + 1) === distanceAways.length) {
						row.addClass("last");
					}
					
					// name cell
					var vehicleIdSpan = jQuery("<span></span>")
											.addClass("bus-id")
											.text(" Bus #" + vehicleInfo.vehicleId);
	
					jQuery('<td></td>')
						.text(headsign)
						.append(vehicleIdSpan)
						.appendTo(row);
				
					// distance cell
					jQuery('<td colspan="2"></td>')
						.addClass("distance")
						.text(vehicleInfo.distanceAway)
						.appendTo(row);
						
					tableBody.append(row);				
				});
			}
		});
		
		// (this is a keep-alive mechanism for a MTA TIS watchdog process that ensures sign apps stay running)
		if(tisMode === "true") {
			window.name = "BusTime";
		}
	}
	
	function updateTimestamp(date) {
		jQuery("#lastupdated")
			.html("")
			.remove();
	
		jQuery("<span></span>")
			.attr("id", "lastupdated")
			.text("Last updated " + date.format("mmm d, yyyy h:MM:ss TT"))
			.appendTo("#footer");
	}
	
	function showError() {
		updateTimestamp(new Date());
		hideError();
		
		jQuery("<p></p>")
				.html("An error occured while updating arrival information&mdash;please check back later.")
				.appendTo("#error");

		jQuery("#arrivals")
			.html("")
			.empty();
	}

	function hideError() {
		jQuery("#error")
			.children()
			.html("")
			.remove();
	}
	
	function update() {
		if(stopIdsToRequest === null) {
			return;
		}

		var arrivalsDiv = jQuery("#arrivals");		
		jQuery.each(stopIdsToRequest, function(_, stopId) {
			if(stopId === null || stopId === "") {
				return;
			}

			var agencyId = "MTA NYCT";
			var stopIdWithoutAgency = stopId;

			var stopIdParts = stopId.split("_");
			if(stopIdParts.length === 2) {
				agencyId = stopIdParts[0];
				stopIdWithoutAgency = stopIdParts[1];
			}

			var stopTable = jQuery("table.stop" + stopIdWithoutAgency);
			if(stopTable.length === 0) {
				stopTable = getNewTableForStop(stopIdWithoutAgency, "Loading...");

				jQuery.getJSON("../" + OBA.Config.searchUrl, { q: stopIdWithoutAgency }, function(json) {
					stopName = json.searchResults[0].name;
					stopTable.find(".name").text(stopName);
				});
				
				arrivalsDiv.append(stopTable.hide());
			}
    		
			var params = { OperatorRef: agencyId, MonitoringRef: stopIdWithoutAgency, StopMonitoringDetailLevel: "normal" };
			jQuery.getJSON("../" + OBA.Config.siriSMUrl, params, function(json) {	
				updateTimestamp(new Date(json.Siri.ServiceDelivery.ResponseTimestamp));
				hideError();
				
				var situationsById = {};
				if(typeof json.Siri.ServiceDelivery.SituationExchangeDelivery !== 'undefined') {
					jQuery.each(json.Siri.ServiceDelivery.SituationExchangeDelivery[0].Situations, function(_, situationElement) {
						situationsById[situationElement[0].SituationNumber] = situationElement[0];
					});
				}
				
				var headsignToDistanceAways = {};
				var applicableSituations = {};
				jQuery.each(json.Siri.ServiceDelivery.StopMonitoringDelivery[0].MonitoredStopVisit, function(_, monitoredStopVisit) {
					var journey = monitoredStopVisit.MonitoredVehicleJourney;

					var headsign = journey.DestinationName;
					if(typeof headsignToDistanceAways[headsign] === 'undefined') {
						headsignToDistanceAways[headsign] = [];
					}
					
					jQuery.each(journey.SituationRef, function(_, situationRef) {
						applicableSituations[situationRef.SituationSimpleRef] = situationsById[situationRef.SituationSimpleRef];
					});
					
					var vehicleInfo = {};
					vehicleInfo.distanceAway = journey.MonitoredCall.Extensions.Distances.PresentableDistance;
					vehicleInfo.vehicleId = journey.VehicleRef.split("_")[1];
					
					headsignToDistanceAways[headsign].push(vehicleInfo);
				});

				// update table for this stop ID
				var stopTable = jQuery("table.stop" + stopIdWithoutAgency).show();

				updateTableForStop(stopTable, applicableSituations, headsignToDistanceAways);
			}); // ajax()
		}); // each()
	}
	
	return {
		initialize: function() {			
			setupUI();			
		}
	};
};

jQuery(document).ready(function() { OBA.Sign().initialize(); });
