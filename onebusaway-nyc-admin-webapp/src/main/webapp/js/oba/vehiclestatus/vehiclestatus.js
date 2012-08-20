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

function showVehiclePopup(vehicleId) {
	//alert("showVehiclePopup(" + vehicleId + ")");
	if (vehicleId == undefined || vehicleId == "") {
		var id = jQuery("#vehicleGrid").jqGrid('getGridParam', 'selrow');
		vehicleId = jQuery("#vehicleGrid").jqGrid('getRowData', id).vehicleId;
		if (vehicleId == undefined || vehicleId == "") { 
			//alert("vehicleId=" + vehicleId);
			return;
		}
	}
	
	//Change these values to style your modal popup
	var align = 'center';										//Valid values; left, right, center
	var top = 100; 												//Use an integer (in pixels)
	var padding = 10;											//Use an integer (in pixels)
	var backgroundColor = '#FFFFFF'; 							//Use any hex code
	var borderColor = '#000000'; 								//Use any hex code
	var borderWeight = 4; 										//Use an integer (in pixels)
	var borderRadius = 5; 										//Use an integer (in pixels)
	var fadeOutTime = 300; 										//Use any integer, 0 = no fade
	var disableColor = '#666666'; 								//Use any hex code
	var disableOpacity = 40; 									//Valid range 0-100
	var loadingImage = '../../css/img/loading.gif';	//Use relative path from this page	
	
	var source = './popup!input.action?vehicleId=' + vehicleId;	//Refer to any page on your server, external pages are not valid
	var width = 500; 					//Use an integer (in pixels)
	modalPopup(align, top, width, padding, disableColor, disableOpacity, backgroundColor, borderColor, borderWeight, borderRadius, fadeOutTime, source, loadingImage, createMaps);

};

var VehicleStatus = Ember.Application.create({
	ready: function() {
		$("#menu").tabs();
		$("#accordion").accordion({
			autoHeight: false
		});
	}
		
});


