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

		haveRestored = true;
    }
   
    function saveState() {
		if(! haveRestored) {
			return;
		}
		
        setState("z", map.getZoom());
        setState("lng", map.getCenter().lng());
        setState("lat", map.getCenter().lat());

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

            

