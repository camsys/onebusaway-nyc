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
					$image.attr("src", "../../css/img/arrow-down-3.png");
				} else {
					//Change the img to right arrow
					$image.attr("src", "../../css/img/arrow-right-3.png");
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
	// check if we were called with a hash -- re-enter from email link
	if (window.location.hash) {
		var hash = window.location.hash;
		hash = hash.split('?')[0];
		// TODO this doesn't work when fromEmail query string is present 
		// alert("hash=" + hash);
		$(hash).click();
	}
	var qs = parseQuerystring();
	if (qs["fromEmail"] == "true") {
		//alert("called from email!");
		jQuery("#prevalidate_id").text(qs["id"]);
		jQuery("#buildBundle_id").text(qs["id"]);
		// just in case set the tab
		var $tabs = jQuery("#tabs");
		$tabs.tabs('select', 2);
		updateBuildStatus();
	}
	// politely set our hash as tabs are changed
	jQuery("#tabs").bind("tabsshow", function(event, ui) {
		window.location.hash = ui.tab.hash;
	});
	jQuery("#currentDirectories").selectable({ 
		stop: function() {
			var names = $.map($('.ui-selected strong, this'), function(element, i) {  
				  return $(element).text();  
				}); 
			if (names.length > 0) {
				var $element = jQuery("#manage-bundles_directoryName");
				// only return the first selection, as multiple selections are possible
				$element.attr("value", names[0]);
				jQuery("#createDirectory #createDirectoryContents #createDirectoryResult").show().css("display","block");
				jQuery("#createDirectoryResult #resultImage").attr("src", "../../css/img/warning_16.png");
				jQuery("#createDirectoryMessage").text("Click Select button to load your directory")
								.css("font-weight", "bold").css("color", "red");
			}
		}
	});

	jQuery("#create_continue").click(onCreateContinueClick);
	
	jQuery("#prevalidate_continue").click(onPrevalidateContinueClick);
	
	// hookup ajax call to select
	jQuery("#directoryButton").click(onSelectClick);
	
	//toggle advanced option contents
	jQuery("#createDirectory #advancedOptions #expand").bind({
			'click' : toggleAdvancedOptions	});
	
	//toggle validation progress list
	jQuery("#prevalidateInputs #prevalidate_progress #expand").bind({
			'click' : toggleValidationResultList});
	
	//toggle bundle build progress list
	jQuery("#buildBundle #buildBundle_progress #expand").bind({
			'click' : toggleBuildBundleResultList});
	
	//handle create and select radio buttons
	jQuery("input[name='options']").change(directoryOptionChanged);
	
	//Handle validate button click event
	jQuery("#prevalidateInputs #validateBox #validateButton").click(onValidateClick);
	
	//Handle build button click event
	jQuery("#buildBundle_buildButton").click(onBuildClick);
	
});

function onCreateContinueClick() {
	var $tabs = jQuery("#tabs");
	$tabs.tabs('select', 1);
}

function onPrevalidateContinueClick() {
	var $tabs = jQuery("#tabs");
	$tabs.tabs('select', 2);
}

function onSelectClick() {
	var bundleDir = jQuery("#manage-bundles_directoryName").val();
	var actionName = "selectDirectory";
	if (jQuery("#create").is(":checked")) {
		actionName = "createDirectory";
	}
		jQuery.ajax({
			url: "manage-bundles!" + actionName + ".action",
			type: "POST",
			data: {"directoryName":  bundleDir},
			async: false,
			success: function(response) {
				var status = eval(response);
				if (status != undefined) {
					jQuery("#createDirectory #createDirectoryContents #createDirectoryResult").show().css("display","block");
					if(status.selected == true) {
						jQuery("#createDirectoryResult #resultImage").attr("src", "../../css/img/dialog-accept-2.png");
					} else {
						jQuery("#createDirectoryResult #resultImage").attr("src", "../../css/img/warning_16.png");
					}
					jQuery("#createDirectoryMessage").text(status.message).css("color", "green");
					jQuery("#create_continue").removeAttr("disabled")
							.removeClass("submit_disabled").addClass("submit_enabled");
					var bundleDir = status.directoryName;
					jQuery("#prevalidate_bundleDirectory").text(bundleDir);
					jQuery("#buildBundle_bundleDirectory").text(bundleDir);
				} else {
					alert("null status");
				}
			},
			error: function(request) {
				alert("onSelectClick error=" + request.statustext);
			}
		});
}


function toggleAdvancedOptions() {
	var $image = jQuery("#createDirectory #advancedOptions #expand");
	changeImageSrc($image);
	//Toggle advanced options box
	jQuery("#advancedOptionsContents").toggle();
}

function toggleValidationResultList() {
	var $image = jQuery("#prevalidateInputs #prevalidate_progress #expand");
	changeImageSrc($image);
	//Toggle progress result list
	jQuery("#prevalidateInputs #prevalidate_resultList").toggle();
}

function toggleBuildBundleResultList() {
	var $image = jQuery("#buildBundle #buildBundle_progress #expand");
	changeImageSrc($image);
	//Toggle progress result list
	jQuery("#buildBundle #buildBundle_resultList").toggle();
}

function changeImageSrc($image) {
	
	var $imageSource = $image.attr("src");
	if($imageSource.indexOf("right-3") != -1) {
		//Change the img to down arrow
		$image.attr("src", "../../css/img/arrow-down-3.png");
	} else {
		//Change the img to right arrow
		$image.attr("src", "../../css/img/arrow-right-3.png");
	}
}

function directoryOptionChanged() {
	//Clear the results regardless of the selection
	jQuery("#createDirectory #createDirectoryContents #createDirectoryResult").hide();
	jQuery("#manage-bundles_directoryName").val("");
	
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
//			url: "manage-bundles!requestExistingDirectories.action",
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
	jQuery("#prevalidate_exception").hide();
	jQuery("#prevalidateInputs #validateBox #validateButton").attr("disabled", "disabled");
	jQuery("#prevalidateInputs #validateBox #validating").show().css("display","inline");
	jQuery.ajax({
		url: "manage-bundles!validateBundle.action",
		type: "POST",
		data: {"bundleDirectory": jQuery("#prevalidate_bundleDirectory").text(),
			"method:validateBundle": "Validate"},
		async: false,
		success: function(response) {
				var bundleResponse = eval(response);
				if (bundleResponse != undefined) {
					jQuery("#prevalidate_id").text(bundleResponse.id);
					//jQuery("#prevalidate_resultList").text("calling...");
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
		url: "manage-bundles!validateStatus.action",
		type: "POST",
		data: {"id": id,
			"method:validateBundle": "Validate"},
		async: false,
		success: function(response) {
				var txt = "<ul>";
				var bundleResponse = eval(response);
				if (bundleResponse == null) {
					jQuery("#prevalidate_validationProgress").text("Complete.");
					jQuery("#prevalidateInputs #validateBox #validating #validationProgress").attr("src","../../css/img/dialog-accept-2.png");
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
					jQuery("#prevalidateInputs #validateBox #validating #validationProgress").attr("src","../../css/img/dialog-accept-2.png");
					updateValidateList(id);
				}
				txt = txt + "</ul>";
				jQuery("#prevalidate_resultList").html(txt).css("font-size", "12px");
				if (bundleResponse.exception != null) {
					if (bundleResponse.exception.message != undefined) {
						jQuery("#prevalidate_exception").show().css("display","inline");
						jQuery("#prevalidate_exception").html(bundleResponse.exception.message);
					}
				}

		},
		error: function(request) {
			alert(request.statustext);
		}
	});
}

// populate list of files that were result of validation
function updateValidateList(id) {
	jQuery.ajax({
		url: "manage-bundles!fileList.action",
		type: "POST",
		data: {"id": id},
		async: false,
		success: function(response) {
				var txt = "<ul>";
				
				var list = eval(response);
				if (list != null) {
					var size = list.length;
					if (size > 0) {
						for (var i=0; i<size; i++) {
							var encoded = encodeURIComponent(list[i]);
							txt = txt + "<li><a href=\"manage-bundles!download.action?id="
							+ id+ "&downloadFilename=" 
							+ encoded + "\">" + encoded +  "</a></li>";
						}
					}
				}
				txt = txt + "</ul>";
				jQuery("#prevalidate_fileList").html(txt);
				jQuery("#prevalidateInputs #validateBox #validateButton").removeAttr("disabled");
		},
		error: function(request) {
			alert(request.statustext);
		}
	});	
}

function onBuildClick() {
	jQuery("#buildBundle_exception").hide();
	jQuery("#buildBundle #buildBox #buildBundle_buildButton").attr("disabled", "disabled");
	jQuery("#buildBundle #buildBox #building").show().css("width","300px").css("margin-top", "20px");
	jQuery.ajax({
		url: "manage-bundles!buildBundle.action",
		type: "POST",
		data: {"bundleDirectory": jQuery("#buildBundle_bundleDirectory").text(),
			"bundleName": jQuery("#buildBundle_bundleName").val(),
			"emailTo": jQuery("#buildBundle_email").val(),
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
		url: "manage-bundles!buildStatus.action",
		type: "POST",
		data: {"id": id,
			"method:buildStatus": "Status"},
		async: false,
		success: function(response) {
				var txt = "<ul>";
				var bundleResponse = eval(response);
				if (bundleResponse == null) {
					jQuery("#buildBundle_buildProgress").text("Bundle Complete!");
					jQuery("#buildBundle #buildBox #building #buildProgress").attr("src","../../css/img/dialog-accept-2.png");
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
					jQuery("#buildBundle_buildProgress").text("Bundle Complete!");
					jQuery("#buildBundle #buildBox #building #buildingProgress").attr("src","../../css/img/dialog-accept-2.png");
					updateBuildList(id);

				}
				txt = txt + "</ul>";
				jQuery("#buildBundle_resultList").html(txt).css("font-size", "12px");	
				// check for exception
				if (bundleResponse.exception != null) {
					if (bundleResponse.exception.message != undefined) {
						jQuery("#buildBundle_exception").show().css("display","inline");
						jQuery("#buildBundle_exception").html(bundleResponse.exception.message);
					}
				}
		},
		error: function(request) {
			alert(request.statustext);
		}
	});
}

// populate list of files that were result of building
function updateBuildList(id) {
	jQuery.ajax({
		url: "manage-bundles!buildList.action",
		type: "POST",
		data: {"id": id},
		async: false,
		success: function(response) {
				var txt = "<ul>";
				
				var list = eval(response);
				if (list != null) {
					var size = list.length;
					if (size > 0) {
						for (var i=0; i<size; i++) {
							var encoded = encodeURIComponent(list[i]);
							txt = txt + "<li><a href=\"manage-bundles!downloadOutputFile.action?id="
							+ id+ "&downloadFilename=" 
							+ encoded + "\">" + encoded +  "</a></li>";
						}
					}
				}
				txt = txt + "</ul>";
				jQuery("#buildBundle_fileList").html(txt);
				jQuery("#buildBundle #buildBox #buildBundle_buildButton").removeAttr("disabled");
		},
		error: function(request) {
			alert(request.statustext);
		}
	});	
}

// add support for parsing query string
  function parseQuerystring (){
    var nvpair = {};
    var qs = window.location.hash.replace('#', 'hash=');
    qs = qs.replace('?', '&');
    var pairs = qs.split('&');
    $.each(pairs, function(i, v){
      var pair = v.split('=');
      nvpair[pair[0]] = pair[1];
    });
    return nvpair;
  }

