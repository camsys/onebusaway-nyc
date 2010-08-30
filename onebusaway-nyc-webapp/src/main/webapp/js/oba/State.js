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
			var routeIds = state.r.split("^");

			var displayedRoutes = jQuery("#displayed-routes");
			var displayedRoutesList = jQuery("#displayed-routes-list");

 	        jQuery("#no-routes-displayed-message").hide();
	        jQuery("#n-displayed-routes").text(routeIds.length);

	        jQuery.ajax({
	            beforeSend: function(xhr) {
					displayedRoutesList.empty();
					
					var r = routeMap.getRoutes();
					
					for(var i = 0; i < r.length; i++) {
						var route = r[i];
						
						routeMap.removeRoute(route);
					}

	                displayedRoutes.addClass("loading");                
	            },
	            complete: function(xhr, s) {
	                displayedRoutes.removeClass("loading");
	            },
	            success: function(json, s, xhr) {
					if(typeof json.routes === 'undefined') {
						return;
					}

					for(var i in json.routes) {
						var route = json.routes[i];

					    routeMap.addRoute(route.routeId, route);

						var clonedDiv = makeRouteFn(route);
						var clonedControlLink = clonedDiv.find(".addToMap");

						clonedDiv.attr("id", "displayedroute-" + route.routeId);

		     			// update the control link class to alter the event fired 
						var clonedControlLink = clonedDiv.find(".addToMap");
						clonedControlLink.removeClass("addToMap");
						clonedControlLink.addClass("removeFromMap");
						clonedControlLink.html("Remove from map");

						displayedRoutesList.append(jQuery("<li></li>").append(clonedDiv));
					}
				},
	            dataType: "json",
	            data: OBA.Util.serializeArray(routeIds, "routeId"),
	            url: OBA.Config.routeShapeUrl
	        });
		} else {	
			jQuery("#n-displayed-routes").text("0");
			jQuery("#displayed-routes-list").empty();
			jQuery("#no-routes-displayed-message").show();

			var r = routeMap.getRoutes();

			for(var i = 0; i < r.length; i++) {
				var route = r[i];
	
				routeMap.removeRoute(route);
			}
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
		setState("r", routeMap.getRoutes().join("^"));

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

            

