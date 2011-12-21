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

OBA.Wizard = function(routeMap) {	
	
	// change to false to disable wizard
	 var wizard_enabled = true;
			
	var wizard = jQuery("#wizard"),
		wizard_start = jQuery("#wizard_start"),
		wizard_startLink = jQuery("#wizard-col1"),
		wizardClose = jQuery("#wizard .close"),
		wizard_inuse = jQuery("#wizard_inuse"),
		wizard_inuseClose = jQuery('#wizard_inuse .close'),
		wizard_didyoumean = jQuery('#wizard_didyoumean'),
		wizard_didyoumeanClose = jQuery('#wizard_didyoumean .close'),
		wizard_finaltip = jQuery("#wizard_finaltip"),
		wizard_finaltipClose = jQuery("#wizard_finaltip .close"),
		searchBar = jQuery("#searchbar form"),
		mapHeader = jQuery("#map_header"),
		formElement = jQuery("input[name=q]"),
		legend = jQuery("#legend"),
		theWindow = jQuery(window);	
	
	var search_title = 'Search',
		search_text = '<p>Type a bus route, <a rel="popover" id="stop_code_popup">stop code</a> or nearby intersection in the search box & press enter.</p><br /><p>Or keep zooming the map in by double clicking on a location.</p>',
		
		direction_title = 'Find Your Stop',
		direction_text = '<p>Click on a direction (next to the <span class="ui-icon ui-icon-triangle-1-e"></span><br /> symbol) to open a bus stop list. Click again to close it.</p><br /><p>Scroll down to your stop & click on it to see it on the map.</p>',

		dirOrStops_text = '<span class="text_span">Click on a direction name (next to the <img src="css/map/img/wizard/arrow.png" /> symbol) to find your stop &nbsp; OR &nbsp; zoom the map until you see clickable <img src="css/map/img/wizard/stop-unknown.png" style="vertical-align:-6px;" /> stop icons.</span>',	
		routeOrZoom_text = '<span class="text_span">Click on a route on the left or zoom the map in until you see a clickable <img src="css/map/img/wizard/stop-unknown.png" style="vertical-align:-6px;" /> stop icon.</span>',
		zoomMap_text = '<span class="text_span">Keep zooming the map in until you see clicakble stop icons: <img src="css/map/img/wizard/stop-unknown.png" style="vertical-align:-6px;" /> or try a search in the left search bar.</span>',
		
		mobile_title = 'Bus Time for Mobile Web',
		mobile_text = 'Visit <a class="mobile_link" href="http://bustime.mta.info/m">mta.info/bustime</a> on your mobile phone browser.',
		
		sms_title = 'Bus Time for SMS / Text',
		sms_text = 'Text your 6-digit bus stop code (also add bus route for best results) to <span style="font-weight:bold">511123</span>.',
		
		share_title = 'Copy this link',
		share_text_prefix = '<form><input id="url" type="text" size="30" style="font-weight:bold;height:20px;width=250px;" value="http://mta.info/bustime/#',
		share_text_postfix = '"></input></form>',
		
		stop_code_title = "What's my bus stop code?",
		stop_pole_diagram = '<div class="pole"><img class="pole_img" src="css/map/img/wizard/bus_stop_pole.png" /><br /><div class="pole_caption">Find stop code here<br />(or enter a search at left).</div></div>',
		
		disambiguation_title = "Find a location",
		disambiguation_text = "Hover to see these on the map, click to zoom in. If none are what you were looking for, try making your search more specific.";
	
	var stop_code_content = "<p>Option 1. Type a location at left or zoom the map in as much as you can. Click on a bus stop name or stop icon <img src='css/map/img/wizard/stop-unknown.png' style='vertical-align:-6px;' /> to see the stop code &amp; bus info.</p>"
						  + "<p>Option 2. Locate your stop code on a bus stop pole box:</p>" 
						  + stop_pole_diagram;
	
	var popover_left = 0,
		wizard_activated = false,
		showFindStopOnMapFooter_launched = false,
		current_height = 0,
		map_listener = null;
		
	/** 
	 * Wizard Stages: 1. Search Start 2a. Search Help 2. Direction/Stop  3. Tips
	 * 
	 * Listen to following search result/map events:
	 * 
	 * stop_result  >> 3. Tips 
	 * intersection_result >> 2. Direction/Stop or Stop Click >> 3. Tips 
	 * location_result >> Hide 'Follow' >> ( ...for now waits until Direction/Stop)
	 * disambiguation_result >> 2a. More search hints
	 * no_results >> just close other popups for now 
	 * route_result >> 2. Direction/Stop > Stop Click >> 3. Tips
	 * map click >> 2. Direction/Stop in footer only
	 * 
	 * 
	 **/
		
	// Set wizard at footer
	function reviseHeight(wizard_height) {	
		wizard.css("height", wizard_height);
		wizard.css("margin-top", -1 * wizard_height - 1);
		current_height = wizard_height;
		popover_left = searchBar.offset().left + searchBar.width();
	}
	reviseHeight(135);
	
	// When window is resized
	function addResizeBehavior() {
		function resize() {
			reviseHeight(current_height);
		}
		theWindow.resize(resize);
	}
	
	// 0. Launch wizard on click
	
	wizard_startLink.click(function(e) { 
		 e.preventDefault(); 
		 reviseHeight(22);
		 wizard_start.hide();
		 wizard_inuse.show();
		 wizard_inuse.popover('hide');  // in case of previous search
		 wizard_start.popover('show');
		 wizard_activated = true;
		 
		 // Stop code inner popup
		 var stop_code_popup = jQuery("#stop_code_popup");
		 stop_code_popup.popover({
				animate: true,
				delayIn: 0,
				delayOut: 200,
				fallback: 'Stop Codes can be found using the map or on bus stop pole boxes.',
				html: true,
				live: false,
				offset: 0,
				placement: 'below',
				title: function() { return stop_code_title; },
				content: function() { return stop_code_content; },
				trigger: 'hover',
				close_btn: false,
				extraClass: true   // info popup within popover
		});
	});
	
	wizardClose.click(function(e) {
		e.preventDefault();
		if (wizard_activated) {
			wizard_start.hide();
			hideSearchPopover();
			hideDidyoumeanPopover();
			unbindLegend();
			wizard.hide();
			wizard_activated = false;
		} else {
			unbindLegend();
			wizard.hide();
		}
	});
	
	wizard_start.popover({
		animate: true,
		delayIn: 0,
		delayOut: 200,
		fallback: 'Enter a search term here',
		html: true,
		live: false,
		placement: 'right',
		title: function() { return search_title; },
		content: function() { return search_text; },
		trigger: 'manual',
		left: popover_left,
		offset: mapHeader.offset().top + formElement.offset().top - 7
	});
	
	function hideSearchPopover() {
		wizard_start.popover('hide');
	}
	
	wizard_inuseClose.click(function(e) {
		e.preventDefault();
		wizard_inuse.hide();
		wizard_start.popover('hide');
		if (wizard_finaltip) {
			wizard_finaltip.popover('hide');
		}
	});
	
	
	// 1. Point out search bar
	// On loading event or map click close pop up
	// Otherwise auto-close wizard if wizard not activated
	searchBar.submit(function() {
		if (wizard_activated) {
			hideSearchPopover();
			hideDidyoumeanPopover();
		} else {
			closeWizard();
		}
	});
	bindLegend();
	map_listener = routeMap.registerMapListener('click', function() { 
			if (wizard_activated) {
				hideSearchPopover(); 
				hideDidyoumeanPopover();
				if (! showFindStopOnMapFooter_launched) {
					showZoomMapFooter();
				}
			} else {
				closeWizard();
			}
		});	
			
	// 2a. Hints for disambiguation	
	var getTopOffset, getPlacement, getPopupLeft;
	
	function prepareDisambiguationPopup() {
		wizard_didyoumean.popover({
			animate: true,
			delayIn: 0,
			delayOut: 200,
			fallback: 'Hover over an address to see it on the map.',
			html: true,
			live: false,
			offset: getTopOffset,
			placement: getPlacement,
			title: function() { return disambiguation_title; },
			content: function() { return disambiguation_text; },
			trigger: 'manual',
			left: getPopupLeft
		});
		
		wizard_didyoumeanClose.click(function(e) {
			e.preventDefault();
			hideSearchPopover();
			hideDidyoumeanPopover();
			wizard_inuse.hide();
			wizard_start.popover('hide');
			if (wizard_finaltip) {
				wizard_finaltip.popover('hide');
			}
		});
	}
	
	var disambiguationPopup = false;
	var didyoumeanHidden = false;
	
	function hideDidyoumeanPopover() {
		wizard_didyoumean.popover('hide');
		wizard_didyoumean.hide();
		if (disambiguationPopup) {
			didyoumeanHidden = true;
		}
	}
	
	function showDisambiguationHelp() {
		if (! wizard_activated) {
			closeWizard();
			unbindLegend();
			return;
		}
		hideSearchPopover(); 		// check this is closed
		hideDirectionPopover();
		jQuery("#wizard_inuse .text_span").html("Find your location...");
				
		// pin to bottom of did you mean results, but if too low pin right
		var popupOffset = 10, popupApproxHeight = 120;
		var lowest_top_offset = theWindow.height() - popupOffset - popupApproxHeight;

		getTopOffset = function() { 
			didyoumean = jQuery("#results");
			top_offset = didyoumean.offset().top + didyoumean.height() + popupOffset;
			if (top_offset > lowest_top_offset) {
				return didyoumean.offset().top + popupOffset;
			} else {
				return top_offset;
			}
		};

		getPopupLeft = function() { 
			didyoumean = jQuery("#results");
			top_offset = didyoumean.offset().top + didyoumean.height() + popupOffset;
			if (top_offset > lowest_top_offset) {
				return didyoumean.offset().left + didyoumean.width() + popupOffset;  
			} else {
				return didyoumean.offset().left;
			} 
		};
			
		getPlacement = function() { 
			didyoumean = jQuery("#results");
			top_offset = didyoumean.offset().top + didyoumean.height() + popupOffset;
			if (top_offset > lowest_top_offset) {
				return 'right'; 
			} else {
				return 'below'; 
			}
		};
		
		if (!disambiguationPopup) {
			prepareDisambiguationPopup();
			disambiguationPopup = true;
		}
		if (!didyoumeanHidden) {		// once closed, don't show again
			wizard_didyoumean.popover('show');	
		}
	}
	
	// 2. Click on direction headings & find stop
	// Show when legend loads
	
	function bindLegend() {
		legend.bind('disambiguation_result', showDisambiguationHelp);
		legend.bind('stop_result', hideDirectionPopoverAndShowFinalTips);
		legend.bind('intersection_result', showFindStopOnMapFooter);
		legend.bind('route_result', showDirectionPopup);
		legend.bind('location_result', showClickOnRouteOrZoomFooter);
		legend.bind('no_result', noResultResponse);
	}
	
	function unbindLegend() {
		legend.unbind('disambiguation_result', showDisambiguationHelp);
		legend.unbind('stop_result', hideDirectionPopoverAndShowFinalTips);
		legend.unbind('intersection_result', showFindStopOnMapFooter);
		legend.unbind('route_result', showDirectionPopup);
		legend.unbind('location_result', showClickOnRouteOrZoomFooter);
		legend.unbind('no_result', noResultResponse);
		routeMap.unregisterInfoWindowListeners();
		if (map_listener) {
			routeMap.unregisterMapListener(map_listener);
		}
	}
	
	function showDirectionPopup() {
		if (! wizard_activated) {
			closeWizard();
			unbindLegend();
			return;
		}
		hideSearchPopover(); 		// check these are closed
		hideDidyoumeanPopover();
		
		wizard_inuse.popover({
			animate: true,
			delayIn: 100,
			delayOut: 200,
			fallback: 'Click on a route direction to find your stop',
			html: true,
			live: false,
			placement: 'right',
			title: function() { return direction_title; },
			content: function() { return direction_text; },
			trigger: 'manual',
			left: popover_left,
			offset: (legend.offset().top + 30)
		});
		wizard_inuse.popover('show');
		
		// Change footer text
		showFindStopOnMapFooter();
				
		// On map interaction hide this popup and show final tips
		if (map_listener) {
			routeMap.unregisterMapListener(map_listener);
		}
		map_listener = routeMap.registerMapListener('click', hideDirectionPopover);	
	}
	
	function hideDirectionPopover() {
		if (wizard_activated) {
			wizard_inuse.popover('hide');
			wizard_inuse.hide();
			showFindStopOnMapFooter();
		} else {
			closeWizard();
		}
	}

	function hideDirectionPopoverAndShowFinalTips() {
		if (! wizard_activated) {
			closeWizard();
			unbindLegend();
			return;
		}
		hideSearchPopover();
		hideDidyoumeanPopover();
		wizard_inuse.popover('hide');
		wizard_inuse.hide();
		if (wizard_activated) {
			showFinalTips();
		}
	}
	
	function noResultResponse() {
		hideSearchPopover();
		hideDidyoumeanPopover();
		jQuery("#wizard_inuse .text_span").html("Find your location...");
	}
	
	function showZoomMapFooter() {
		jQuery("#wizard_inuse .text_span").html(zoomMap_text);
		
		// On stop bubble click show final Tips
		routeMap.registerInfoWindowListener(hideDirectionPopoverAndShowFinalTips);
	}
	
	function showClickOnRouteOrZoomFooter() {
		if (! wizard_activated) {
			closeWizard();
			unbindLegend();
			return;
		}
		hideSearchPopover();
		hideDidyoumeanPopover();
		wizard_inuse.show();
		jQuery("#wizard_inuse .text_span").html(routeOrZoom_text);		
	}
	
	function showFindStopOnMapFooter() {
		if (! wizard_activated) {
			closeWizard();
			unbindLegend();
			return;
		}
		hideSearchPopover();
		hideDidyoumeanPopover();
		jQuery("#wizard_inuse .text_span").html(dirOrStops_text);
		wizard_inuse.show();
		
		// On stop bubble click show final Tips
		routeMap.registerInfoWindowListener(hideDirectionPopoverAndShowFinalTips);
		showFindStopOnMapFooter_launched = true;
	}
	
	// 3. Final tips & social web links
	function showFinalTips() {
		wizard_inuse.hide();
		reviseHeight(90);
		wizard_finaltip.show();
		unbindLegend();
	}
	
	wizard_finaltipClose.click(function(e) {
		e.preventDefault();
		wizard_finaltip.hide();
		wizard_start.popover('hide');
		if (wizard_inuse) {
			wizard_inuse.popover('hide');
		}
		if (wizard_share) {
			wizard_share.popover('hide');
		}
		wizard.hide();
	});
	
	
	// Mobile Web tip popups
	var wizard_mobile_splash = jQuery("#wizard-col3");
	wizard_mobile_splash.popover({
		animate: true,
		delayIn: 100,
		delayOut: 100,
		fallback: 'Go to http://bustime.mta.info on your phone',
		html: true,
		live: false,
		offset: -10,
		placement: 'above',
		title: function() { return mobile_title; },
		content: function() { return mobile_text; },
		trigger: 'hover',
		close_btn: false
	});
	
	var wizard_mobile = jQuery("#wizard_mobile");
	wizard_mobile.popover({
		animate: true,
		delayIn: 100,
		delayOut: 100,
		fallback: 'Go to http://bustime.mta.info on your phone',
		html: true,
		live: false,
		offset: 0,
		placement: 'above',
		title: function() { return mobile_title; },
		content: function() { return mobile_text; },
		trigger: 'hover',
		close_btn: false
	});
	
	// SMS tip popups
	var wizard_sms_splash = jQuery("#wizard-col4");
	wizard_sms_splash.popover({
		animate: true,
		delayIn: 100,
		delayOut: 100,
		fallback: 'Text your 6-digit Bus Stop Code # to <span style="font-weight:bold;text-decoration:none;">511123</span>',
		html: true,
		live: false,
		offset: -10,
		placement: 'above',
		title: function() { return sms_title; },
		content: function() { return sms_text; },
		trigger: 'hover', 
		close_btn: false
	});

	var wizard_sms = jQuery("#wizard_sms");
	wizard_sms.popover({
		animate: true,
		delayIn: 100,
		delayOut: 100,
		fallback: 'Text your 6-digit Bus Stop Code # to <span style="font-weight:bold;text-decoration:none;">511123</span>',
		html: true,
		live: false,
		offset: 0,
		placement: 'above',
		title: function() { return sms_title; },
		content: function() { return sms_text; },
		trigger: 'hover',
		close_btn: false
	});
	
	function getSearchInputVal() {
		var searchInput = jQuery("#searchbar form input[type=text]");
		return searchInput.val();
	}
	
	// Share link popup
	var wizard_share  = jQuery("#wizard_share");
	wizard_share.popover({
		animate: true,
		delayIn: 50,
		delayOut: 100,
		fallback: 'Copy this URL: <span style="font-weight:bold;text-decoration:none">http://mta.info/bustime/#' + getSearchInputVal() + '</span>',
		html: true,
		live: false,
		offset: 10,
		placement: 'above',
		title: function() { return share_title; },
		content: function() { return share_text_prefix + getSearchInputVal() + share_text_postfix; },
		trigger: 'hover',
		close_btn: false
	});
	
	wizard_share.hover(function(e) {
		e.preventDefault();
		setTimeout( function() {
			var urlField = jQuery('input#url');
			urlField.focus();
			urlField.select();
		}, 300);
	});
	
	// Final tips stop code popup
	// Stop code inner popup
	 var tips_code_popup = jQuery("#tips_code_popup");
	 tips_code_popup.popover({
			animate: true,
			delayIn: 0,
			delayOut: 0,
			fallback: 'Stop Codes can also be found on bus stop pole boxes.',
			html: true,
			live: false,
			offset: 0,
			placement: 'below',
			title: function() { return "Bus Stop Pole Box"; },
			content: function() { return stop_pole_diagram; },
			trigger: 'hover',
			close_btn: false
		});
	 
	 function closeWizard() {
		 wizardClose.trigger('click');
		 wizard_enabled = false;
	 }
	 	 
	 return  {
		 
		 enabled: function() {
			 return wizard_enabled;
		 }
	 };
};