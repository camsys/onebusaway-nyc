var OBA = window.OBA || {};

OBA.State = function(map, routeMap, makeRouteFn) {
    var state = {};        
    var currentHash = null;
    var haveRestored = false;
        
    function setState(k, v) {
        if(state[k] != v) {
            state[k] = v;
        } 
    }
    
    // fetch one route at a time
    function fetchRoutes(routeDirectionId, remainingRouteDirectionIds, displayedRoutes, displayedRoutesList) {
        var routeId = routeDirectionId[0];
        var directionId = routeDirectionId[1];
        var url = OBA.Config.routeShapeUrl + "/" + routeId + ".json";
        jQuery.ajax({
            beforeSend: function(xhr) {
                displayedRoutes.addClass("loading");                
            },
            complete: function(xhr, s) {
                displayedRoutes.removeClass("loading");
            },
            success: function(json, s, xhr) {
                
                var shape;
                try {
                    shape = json.data.entry.polylines;
                } catch (typeError) {
                    OBA.Util.log("invalid route response from server");
                    OBA.Util.log(json);
                    return;
                }

                    routeMap.addRoute(routeId, directionId, shape);
                    
                    var jsonRouteResponse;
                    try {
                        jsonRouteResponse = json.data.references.routes[0];
                    } catch (typeError) {
                        OBA.Util.log("invalid route response from server");
                        OBA.Util.log(json);
                        return;
                    }

                    // adapt the data returned from server to match what the route
                    // generator function expects
                    var routeData = {routeId: routeId,
                                     directionId: directionId,
                                     tripHeadsign: jsonRouteResponse.shortName + " " + jsonRouteResponse.longName,
                                     //serviceNotice: null,
                                     name: jsonRouteResponse.shortName,
                                     description: jsonRouteResponse.longName};                    

                    var clonedDiv = makeRouteFn(routeData);
                    var clonedControlLink = clonedDiv.find(".addToMap");

                    clonedDiv.attr("id", "displayedroute-" + routeId);

                    // update the control link class to alter the event fired 
                    var clonedControlLink = clonedDiv.find(".addToMap");
                    clonedControlLink.removeClass("addToMap");
                    clonedControlLink.addClass("removeFromMap");
                    clonedControlLink.html("Remove from map");

                    displayedRoutesList.append(jQuery("<li></li>").append(clonedDiv));
                    
                    if (remainingRouteDirectionIds.length > 0)
                        fetchRoutes(remainingRouteDirectionIds[0], remainingRouteDirectionIds.slice(1),
                                    displayedRoutes, displayedRoutesList);
            },
            dataType: "json",
            data: {version: 2, key: OBA.Config.apiKey},
            url: url
        });
    }

    function loadState(hash) {
		if(currentHash === hash || hash === null)
			return;

		currentHash = hash;

        unserialize(hash);

		// map state
		if(typeof state.lat !== 'undefined' && typeof state.lng !== 'undefined' &&
			state.lat != map.getCenter().lat() && state.lng != map.getCenter().lng()) { 	   
			        map.setCenter(new google.maps.LatLng(state.lat, state.lng));        
        }

		if(typeof state.z !== 'undefined' && state.z != map.getZoom()) { 
			map.setZoom(parseInt(state.z));
		}

		// displayed route state
		if(typeof state.r !== 'undefined' && state.r != "") {
		    var encodedRouteDirectionIds = state.r.split("^");
		    
		    var routeDirectionIds = [];
		    jQuery.each(encodedRouteDirectionIds, function(_, routeDirectionId) {
		        var routeDirectionPair = routeDirectionId.split("!");
		        routeDirectionIds.push(routeDirectionPair);
		    });

			var displayedRoutes = jQuery("#displayed-routes");
			var displayedRoutesList = jQuery("#displayed-routes-list");

	        if (routeDirectionIds.length > 0) {
                displayedRoutesList.empty();

                routeMap.removeAllRoutes();
//                jQuery.each(routeMap.getRoutes(), function(_, routeId) {
//                    routeMap.removeRoute(routeId);
//                });
                
	            fetchRoutes(routeDirectionIds[0], routeDirectionIds.slice(1), displayedRoutes, displayedRoutesList);
	            
	            jQuery("#no-routes-displayed-message").hide();
	            jQuery("#n-displayed-routes").text(routeDirectionIds.length);
	        }
		} else {	
			jQuery("#n-displayed-routes").text("0");
			jQuery("#displayed-routes-list").empty();
			jQuery("#no-routes-displayed-message").show();

//			var r = routeMap.getRoutes();
//
//			for(var i = 0; i < r.length; i++) {
//				var route = r[i];
//	
//				routeMap.removeRoute(route);
//			}
			routeMap.removeAllRoutes();
		}

		haveRestored = true;
    }
   
    function saveState() {
		if(! haveRestored) {
			return;
		}
		
        setState("z", map.getZoom());
        setState("lng", map.getCenter().lng());
        setState("lat", map.getCenter().lat());
        var routeDirections = routeMap.getRoutes();
        var serializedRouteDirections = jQuery.map(routeDirections, function(routeDirection) {
            return routeDirection[0] + "!" + routeDirection[1];
        });
		setState("r", serializedRouteDirections.join("^"));

        serialize();
    }

    function unserialize(hash) {
		state = {};
		
        var c = hash.split("|");
        
        for(var i = 0; i < c.length; i += 2) {
            if(typeof c[i] !== 'undefined' && typeof c[i + 1] !== 'undefined' && c[i + 1] !== "") {
	            state[c[i]] = c[i + 1];            
	    	}
        }    
    }
    
    function serialize() {
        var s = "";

        jQuery.each(state, function(k) {
            var v = state[k];
            
	    	if(typeof v !== 'undefined' && v !== "") {
		            s += k + "|" + v + "|";            
		    }
        });

		currentHash = s;

        jQuery.history.load(s);
    }    

	// register to be notified when map changes
    google.maps.event.addListener(map, "idle", saveState);

	// setup hash tag change listener
    jQuery.history.init(loadState);

    return {
		saveState: saveState
    }
}

            

