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

OBA.SignConfig = function() {
	function hideError() {
		jQuery("#error")
			.empty();
	}
	
	function showError(message) {
		jQuery("<li></li>")
			.text(message)
			.appendTo("#error");
	}
	
	function generateSignUrl() {
		var url = window.location.href + "sign?";
		
		var routeId = jQuery("#route option:selected").val();
		url += "routeId=" + OBA.Util.parseEntityId(routeId) + "&";

		var refreshInterval = jQuery("#refresh option:selected").val();
		url += "refresh=" + refreshInterval + "&";

		var message = jQuery("#message").val();
		url += "message=" + jQuery('<div>' + message + '</div>').text() + "&";

		var stopIdCollection = jQuery("#stop_id_collection");
		var stopIds = "";
		stopIdCollection.find("li").each(function() {
			var stopId = jQuery(this).data("stopId");
			
			if(stopIds.length > 0) {
				stopIds += ",";
			}
			
			if(stopId !== null && stopId !== "") {
				stopIds += OBA.Util.parseEntityId(stopId);
			}
		});
		url += "stopIds=" + stopIds;
		
		if(stopIds === "") {
			showError("You must select at least one stop to display on your sign.");
			return;
		}
		
		window.location.href = url;
	}
	
	function setupUI() {
		jQuery("#route").change(function() {			
			var routeId = jQuery(this).val();
			var url = OBA.Config.vehiclesUrl + "/" + routeId + ".json";			
			var params = {version: 2, key: OBA.Config.apiKey};			
			
			jQuery.getJSON(url, params, function(json) {
				var directionIdSelect = jQuery("#direction_id");				

				directionIdSelect.empty();

				var option = jQuery("<option></option>")
								.val("")
								.text("Select A Route Direction");

				directionIdSelect.append(option);
				
				// generate map of directionIds->headsigns->observation counts
				var directionIdToHeadsignMap = {};
				jQuery.each(json.data.references.trips, function(_, trip) {
					var directionId = trip.directionId;
					var headsign = trip.tripHeadsign;
					
					if(typeof directionIdToHeadsignMap[directionId] === 'undefined') {
						directionIdToHeadsignMap[directionId] = {};
					}
					
					if(typeof directionIdToHeadsignMap[directionId][headsign] === 'undefined') {
						directionIdToHeadsignMap[directionId][headsign] = 1;
					} else {
						directionIdToHeadsignMap[directionId][headsign]++;
					}
				});

				// find most common headsign per direction ID
				var directionIdToMostCommonHeadsign = {};
				jQuery.each(directionIdToHeadsignMap, function(directionId, headsignMap) {
					var maxObs = -1;
					var mostCommonHeadsign = null;
					jQuery.each(headsignMap, function(headsign, count) {
						if(count > maxObs) {
							maxObs = count;
							mostCommonHeadsign = headsign;							
						}
					});
					
					var option = jQuery("<option></option>")
									.val(directionId)
									.text(mostCommonHeadsign);
					
					directionIdSelect.append(option);
				});
			}); // getJSON
		}); // route change
		
		jQuery("#direction_id").change(function() {
			var routeId = jQuery("#route option:selected").val();
			var directionId = jQuery("#direction_id option:selected").val();
			var stopIdSelect = jQuery("#stop_id");

			if(directionId === "" || directionId === null) {
				return;
			}

			var url = OBA.Config.routeShapeUrl + "/" + routeId + ".json";			
			var params = {version: 2, key: OBA.Config.apiKey};			

			jQuery.getJSON(url, params, function(json) {				
				// build a hash map of stop IDs to add for this direction
				var stopIdsToAdd = {};
				jQuery.each(json.data.entry.stopGroupings, function(_, stopGroupRef) {
					if(stopGroupRef.type !== "direction") {
						return;
					}

					for(var i = 0; i < stopGroupRef.stopGroups.length; i++) {
						var stopGroup = stopGroupRef.stopGroups[i];
						if(stopGroup.id !== directionId) {
							continue;
						}
						for(var z = 0; z < stopGroup.stopIds.length; z++) {
							var stopId = stopGroup.stopIds[z];
							stopIdsToAdd[stopId] = stopId;							
						}
					}
				});

				stopIdSelect.empty();

				var option = jQuery("<option></option>")
								.val("")
								.text("Select A Stop");

				stopIdSelect.append(option);
				
				// go through stops and add the ones we need
				jQuery.each(json.data.references.stops, function(_, stop) {
					if(stop.id in stopIdsToAdd) {
						var option = jQuery("<option></option>")
										.val(stop.id)
										.text(stop.name);
	
						stopIdSelect.append(option);
					}
				});
			});
		}); // direction ID change
		
		// add stop Id button
		jQuery("input.addStopId").click(function() {
			hideError();
			
			var stopIdCollection = jQuery("#stop_id_collection");
			var stopIdSelectedItem = jQuery("#stop_id option:selected");
			var stopId = stopIdSelectedItem.val();
			var stopName = stopIdSelectedItem.text();
			
			// already in list?
			stopIdCollection.find("li").each(function() {
				var item = jQuery(this);				
				if(item.data("stopId") === stopId) {
					stopId = null;
					return;
				}
			});
			
			if(stopId === null || stopName === null || stopId === "") {
				return;
			}

			var removeLink = jQuery('<a href="#">Remove</a>')
								.click(function() {
									jQuery(this).parent().remove();
									return false;
								});

			var newStopIdItem = jQuery("<li></li>")
									.text(stopName)
									.data("stopId", stopId)
									.append(removeLink);
			
			stopIdCollection.append(newStopIdItem);			
		});

		jQuery("form").submit(function(e) {
			e.preventDefault();
			
			generateSignUrl();
			
			return false;
		});
	}
	
	return {
		initialize: function() {
			setupUI();
			
			jQuery("#route").trigger("change");
		}
	};
};

jQuery(document).ready(function() { OBA.SignConfig().initialize(); });
