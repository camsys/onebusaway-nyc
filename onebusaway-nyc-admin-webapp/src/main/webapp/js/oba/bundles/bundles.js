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
		data: {"method:validateBundle": "Validate"},
		async: false,
		success: function(response) {
				alert("response=" + response);
		},
		error: function(request) {
			alert(request.statustext);
		}
	});
}
