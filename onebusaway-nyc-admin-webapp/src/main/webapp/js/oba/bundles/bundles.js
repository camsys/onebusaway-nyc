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
	
	
	//handle tab clicks
	jQuery("#breadcrumb").bind('click', onTabClick);
	
	//toggle advanced option contents
	jQuery("#createDirectory #advancedOptions #expand").bind({
			'click' : toggleAdvancedOptions	});
	
	//handle create and select radio buttons
	jQuery("input[name='options']").change(directoryOptionChanged);
	
	//Handle validate button click event
	jQuery("#prevalidateInputs #validateBox #validateButton").click(onValidateClick);
	
});

function onTabClick(event) {
	var $target = event.target;
	
	if($target.hash == "#Create") {
		jQuery("#Create").load("bundles/create-bundle-directory.action");
	}
	if($target.hash == "#Validate") {
		jQuery("#Validate").load("bundles/prevalidate-inputs.action");
	}
	if($target.hash == "#Build") {
		jQuery("#Build").load("bundles/build-bundle.action");
	}
	if($target.hash == "#Deploy") {
		jQuery("#Deploy").load("bundles/deploy-bundle.action");
	}
	
	
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
		jQuery.ajax({
			url: "/onebusaway-nyc-admin-webapp/admin/bundles/manage-bundles!getExistingDirectories.action",
			type: "GET",
			dataType: "json",
			success: function(response) {
				alert(response);
			},
			error: function(request) {
				alert(request.statustext);
			}
		});
		jQuery("#selectExistingContents").show();
	}
	
}

function onValidateClick() {
	jQuery("#prevalidateInputs #validateBox #validating").show().css("display","inline");
	jQuery.ajax({
		url: "/onebusaway-nyc-admin-webapp/admin/bundles/manage-bundles!validateBundle.action",
		type: "GET",
		success: function(response) {
			
		}
	});
}





