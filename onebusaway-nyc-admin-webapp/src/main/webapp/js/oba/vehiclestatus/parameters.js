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


jQuery(function() {
	//Initialize the accordion control
	$("#accordion").accordion({
		autoHeight: false
	});
	
	getConfigParameters();
	
	//Handle reset and save click events
	$("#results #reset").click(resetToPrevious);
	$("#results #save").click(saveParameters);
	
});

function getConfigParameters() {
	$.ajax({
		url: "parameters!getParameters.action?ts=" + new Date().getTime(),
		type: "GET",
		dataType: "json",
		success: function(response) {
			updateParametersView(response.configParameters);
		},
		error: function(request) {
			alert("Error getting parameters from server : ", request.statusText);
		}
		
	});
}

function updateParametersView(configParameters) {
	var panels = $("#accordion").children("li");
	//Update view by looping through sections in each accordion panel
	for(var i=0; i<panels.length; i++) {
		var sections = $(panels[i]).children("div").children("div");
		for(var j=0; j<sections.length; j++) {
			if($(sections[j]).children(".propertyHolder").length > 0) {
				//We have multiple sections per panel here. Process each one of them
				var properties = $(sections[j]).children(".propertyHolder");
				for(var k=0; k<properties.length; k++) {
					var keyElement = $(properties[k]).find("input[type='hidden']");
					var configKey = keyElement.val();
					$(keyElement).next().val(configParameters[configKey]);
				}
			} else {
				//Sections are properties in this case. Set the values to the next sibling 
				//of the hidden child
				var keyElement = $(sections[j]).find("input[type='hidden']");
				var configKey = keyElement.val();
				$(keyElement).next().val(configParameters[configKey]);
			}
		}
	}
}

function resetToPrevious() {
	//Reset parameter values to last saved values on server
	getConfigParameters();
}

function saveParameters() {
	
}
