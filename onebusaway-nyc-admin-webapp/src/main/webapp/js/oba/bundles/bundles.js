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

/*var OBA = window.OBA || {};

OBA.Bundles = function() {
	
	function toggleAdvancedOptionsContents() {
		jQuery("#createDirectory #advancedOptions #expand").bind({
			'click' : function () {
				var $image = jQuery("#createDirectory #advancedOptions #expand");
				var $imageSource = $image.attr("src");
				if($imageSource.indexOf("right-3") != -1) {
					//Change the img to down arrow
					$image.attr("src", "/onebusaway-nyc-admin-webapp/css/img/arrow-down-3.png");
				} else {
					//Change the img to right arrow
					$image.attr("src", "/onebusaway-nyc-admin-webapp/css/img/arrow-right-3.png");
				}
				//Toggle advanced options box
				jQuery("#advancedOptionsContents").toggle();
			}	});
	} 

	return {
		initialize : function () {
			toggleAdvancedOptionsContents();
		}
	};
};

jQuery(document).ready(function() { OBA.Bundles.initialize(); });*/

jQuery(document).ready(function() {
	jQuery("#tabs").tabs();
	jQuery("#currentDirectories").selectable({ 
		stop: function() {
			var names = $.map($('.ui-selected strong, this'), function(element, i) {  
				  return $(element).text();  
				}); 
			if (names.length > 0) {
				var $element = jQuery("#manage-bundles_directoryName");
				// only return the first selection, as multiple selections are possible
				$element.attr("value", names[0]);
			}
		}
	});
	
	// hookup ajax call to select
	jQuery("#directoryButton").click(onSelectClick);
	//toggle advanced option contents
	jQuery("#createDirectory #advancedOptions #expand").bind({
			'click' : toggleAdvancedOptions	});
	
	//handle create and select radio buttons
	jQuery("input[name='options']").change(directoryOptionChanged);
	
	//Handle validate button click event
	jQuery("#prevalidateInputs #validateBox #validateButton").click(onValidateClick);
	
	//Handle build button click event
	jQuery("#buildBundle_buildButton").click(onBuildClick);
	
});


function onSelectClick() {
	var bundleDir = jQuery("#manage-bundles_directoryName").val();
		jQuery.ajax({
			url: "/onebusaway-nyc-admin-webapp/admin/bundles/manage-bundles!selectDirectory.action",
			type: "POST",
			data: {"directoryName":  bundleDir,
				"method:selectDirectory": "Select"},
			async: false,
			success: function(response) {
				var $tabs = jQuery("#tabs");
				$tabs.tabs('select', 1);
				jQuery("#prevalidate_bundleDirectory").text(bundleDir);
				jQuery("#buildBundle_bundleDirectory").text(bundleDir);
			},
			error: function(request) {
				alert(request.statustext);
			}
		});
}


function toggleAdvancedOptions() {
	var $image = jQuery("#createDirectory #advancedOptions #expand");
	var $imageSource = $image.attr("src");
	if($imageSource.indexOf("right-3") != -1) {
		//Change the img to down arrow
		$image.attr("src", "/onebusaway-nyc-admin-webapp/css/img/arrow-down-3.png");
	} else {
		//Change the img to right arrow
		$image.attr("src", "/onebusaway-nyc-admin-webapp/css/img/arrow-right-3.png");
	}
	//Toggle advanced options box
	jQuery("#advancedOptionsContents").toggle();
}

function directoryOptionChanged() {
	if(jQuery("#create").is(":checked")) {
		//Change the button text and hide select directory list
		jQuery("#createDirectoryContents #directoryButton").val("Create");
		jQuery("#createDirectoryContents #directoryButton").attr("name","method:createDirectory");
		jQuery("#selectExistingContents").hide();
	} else {
		//Change the button text and show select directory list
		jQuery("#createDirectoryContents #directoryButton").val("Select");
		jQuery("#createDirectoryContents #directoryButton").attr("name","method:selectDirectory");
		jQuery("#selectExistingContents").show();
		// TODO replace the exitingDirectories form call with this below
//		jQuery.ajax({
//			url: "/onebusaway-nyc-admin-webapp/admin/bundles/manage-bundles!requestExistingDirectories.action",
//			type: "GET",
//			async: false,
//			success: function(response) {
//				jQuery("#selectExistingContents").show();
//				
//			},
//			error: function(request) {
//				alert(request.statustext);
//			}
//		});
	}
	
}

function onValidateClick() {
	jQuery("#prevalidateInputs #validateBox #validating").show().css("display","inline");
	jQuery.ajax({
		url: "/onebusaway-nyc-admin-webapp/admin/bundles/manage-bundles!validateBundle.action",
		type: "POST",
		data: {"bundleDirectory": jQuery("#prevalidate_bundleDirectory").text(),
			"method:validateBundle": "Validate"},
		async: false,
		success: function(response) {
				var bundleResponse = eval(response);
				if (bundleResponse != undefined) {
					jQuery("#prevalidate_id").text(bundleResponse.id);
					jQuery("#prevalidate_resultList").text("calling...");
					window.setTimeout(updateValidateStatus, 1000);
				} else {
					jQuery("#prevalidate_id").text(error);
					jQuery("#prevalidate_resultList").text("error");
				}
		},
		error: function(request) {
			alert(request.statustext);
		}
	});
}

function updateValidateStatus() {
	var id = jQuery("#prevalidate_id").text();
	jQuery.ajax({
		url: "/onebusaway-nyc-admin-webapp/admin/bundles/manage-bundles!validateStatus.action",
		type: "POST",
		data: {"id": id,
			"method:validateBundle": "Validate"},
		async: false,
		success: function(response) {
				var txt = "<ul>";
				var bundleResponse = eval(response);
				if (bundleResponse == null) {
					jQuery("#prevalidate_validationProgress").text("Complete.");
					jQuery("#prevalidateInputs #validateBox #validating #validationProgress").hide();
					jQuery("#prevalidate_resultList").html("unknown id=" + id);
				}
				var size = bundleResponse.statusMessages.length;
				if (size > 0) {
					for (var i=0; i<size; i++) {
						txt = txt + "<li>" + bundleResponse.statusMessages[i] + "</li>";
					}
				}
				if (bundleResponse.complete == false) {
					window.setTimeout(updateValidateStatus, 1000); // recurse
				} else {
					jQuery("#prevalidate_validationProgress").text("Complete.");
					jQuery("#prevalidateInputs #validateBox #validating #validationProgress").hide();
				}
				txt = txt + "</ul>";
				jQuery("#prevalidate_resultList").html(txt);	
		},
		error: function(request) {
			alert(request.statustext);
		}
	});
}

function onBuildClick() {
	jQuery("#buildBundle #buildBox #building").show().css("display","inline");
	jQuery.ajax({
		url: "/onebusaway-nyc-admin-webapp/admin/bundles/manage-bundles!buildBundle.action",
		type: "POST",
		data: {"bundleDirectory": jQuery("#buildBundle_bundleDirectory").text(),
			"method:buildBundle": "Build"},
		async: false,
		success: function(response) {
				var bundleResponse = eval(response);
				if (bundleResponse != undefined) {
					jQuery("#buildBundle_id").text(bundleResponse.id);
					jQuery("#buildBundle_resultList").html("calling...");
					window.setTimeout(updateBuildStatus, 1000);
				} else {
					jQuery("#buildBundle_id").text(error);
					jQuery("#buildBundle_resultList").html("error");
				}
		},
		error: function(request) {
			alert(request.statustext);
		}
	});
}

function updateBuildStatus() {
	id = jQuery("#buildBundle_id").text();
	jQuery.ajax({
		url: "/onebusaway-nyc-admin-webapp/admin/bundles/manage-bundles!buildStatus.action",
		type: "POST",
		data: {"id": id,
			"method:buildStatus": "Status"},
		async: false,
		success: function(response) {
				var txt = "<ul>";
				var bundleResponse = eval(response);
				if (bundleResponse == null) {
					jQuery("#buildBundle_buildProgress").text("Complete.");
					jQuery("#buildBundle #buildBox #building #buildProgress").hide();
					jQuery("#buildBundle_resultList").html("unknown id=" + id);
				}
				var size = bundleResponse.statusList.length;
				if (size > 0) {
					for (var i=0; i<size; i++) {
						txt = txt + "<li>" + bundleResponse.statusList[i] + "</li>";
					}
				}
				if (bundleResponse.complete == false) {
					window.setTimeout(updateBuildStatus, 1000); // recurse
				} else {
					jQuery("#buildBundle_buildProgress").text("Complete.");
					jQuery("#buildBundle #buildBox #building #buildingProgress").hide();
				}
				txt = txt + "</ul>";
				jQuery("#buildBundle_resultList").html(txt);	
		},
		error: function(request) {
			alert(request.statustext);
		}
	});
}
