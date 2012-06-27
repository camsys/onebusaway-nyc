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

var timeout = null;

function startup(){
	
	
	//Initialize tabs
	jQuery("#tabs").tabs();
	
	//Initialize date pickers
	jQuery("#startDatePicker").datepicker(
			{ 
				dateFormat: "yy-mm-dd",
				altField: "#startDate",
				onSelect: function(selectedDate) {
					jQuery("#endDatePicker").datepicker("option", "minDate", selectedDate);
				}
			});
	jQuery("#endDatePicker").datepicker(
			{ 
				dateFormat: "yy-mm-dd",
				altField: "#endDate",
				onSelect: function(selectedDate) {
					jQuery("#startDatePicker").datepicker("option", "maxDate", selectedDate);
				}
			});
	
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
		jQuery("#buildBundle_bundleName").val(qs["name"]);
		//hide the result link when reentering from email
		jQuery("#buildBundle_resultLink").hide();
		// just in case set the tab
		var $tabs = jQuery("#tabs");
		$tabs.tabs('select', 3);
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
	
	jQuery("#upload_continue").click(onUploadContinueClick);
	
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
	
}



	return {
		initialize : function () {
			startup();
		}
	};
};

jQuery(document).ready(function() { OBA.Bundles.initialize(); });*/

var timeout = null;

jQuery(document).ready(function() {
	
	
	//Initialize tabs
	jQuery("#tabs").tabs();
	
	//Initialize date pickers
	jQuery("#startDatePicker").datepicker(
			{ 
				dateFormat: "yy-mm-dd",
				altField: "#startDate",
				onSelect: function(selectedDate) {
					jQuery("#endDatePicker").datepicker("option", "minDate", selectedDate);
				}
			});
	jQuery("#endDatePicker").datepicker(
			{ 
				dateFormat: "yy-mm-dd",
				altField: "#endDate",
				onSelect: function(selectedDate) {
					jQuery("#startDatePicker").datepicker("option", "maxDate", selectedDate);
				}
			});
	
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
		jQuery("#buildBundle_bundleName").val(qs["name"]);
		//hide the result link when reentering from email
		jQuery("#buildBundle_resultLink").hide();
		// just in case set the tab
		var $tabs = jQuery("#tabs");
		$tabs.tabs('select', 3);
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
	
	jQuery("#upload_continue").click(onUploadContinueClick);
	
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

function onUploadContinueClick() {
	var $tabs = jQuery("#tabs");
	$tabs.tabs('select', 2);
}

function onPrevalidateContinueClick() {
	var $tabs = jQuery("#tabs");
	$tabs.tabs('select', 3);
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
					var continueButton = jQuery("#create_continue");
					enableContinueButton(continueButton);
					var bundleDir = status.directoryName;
					jQuery("#prevalidate_bundleDirectory").text(bundleDir);
					jQuery("#buildBundle_bundleDirectory").text(bundleDir);
					jQuery("#uploadFiles #s3_details #s3_location").text(status.bucketName);
					jQuery("#uploadFiles #gtfs_details #gtfs_location").text(bundleDir + "/" + status.gtfsPath + " directory");
					jQuery("#uploadFiles #stif_details #stif_location").text(bundleDir + "/" + status.stifPath + " directory");
					enableContinueButton(jQuery("#upload_continue"));
				} else {
					alert("null status");
				}
			},
			error: function(request) {
				alert("There was an error processing your request. Please try again.");
			}
		});
}

function enableContinueButton(continueButton) {
	jQuery(continueButton).removeAttr("disabled")
		.removeClass("submit_disabled").addClass("submit_enabled");	
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
	var bundleDirectory = jQuery("#prevalidate_bundleDirectory").text();
	if (bundleDirectory == undefined || bundleDirectory == null || bundleDirectory == "") {
		alert("missing bundle directory");
		return;
	}
	var bundleName = jQuery("#prevalidate_bundleName").val();
	if (bundleName == undefined || bundleName == null || bundleName == "") {
		alert("missing bundle build name");
		return;
	} else {
		jQuery("#buildBundle_bundleName").val(bundleName);
	}

	jQuery("#prevalidate_exception").hide();
	jQuery("#prevalidateInputs #validateBox #validateButton").attr("disabled", "disabled");
	jQuery("#prevalidateInputs #validateBox #validating").show().css("display","inline");
	jQuery.ajax({
		url: "../../api/validate/" + bundleDirectory + "/" + bundleName + "/create",
		type: "GET",
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
			alert("There was an error processing your request. Please try again.");
		}
	});
}

function updateValidateStatus() {
	var id = jQuery("#prevalidate_id").text();
	jQuery.ajax({
		url: "../../api/validate/" + id + "/list",
		type: "GET",
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
			clearTimeout(timeout);
			timeout = setTimeout(updateValidateStatus, 10000);
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
							txt = txt + "<li><a href=\"manage-bundles!downloadValidateFile.action?id="
							+ id+ "&downloadFilename=" 
							+ encoded + "\">" + encoded +  "</a></li>";
						}
					}
				}
				txt = txt + "</ul>";
				jQuery("#prevalidate_fileList").html(txt);
				jQuery("#prevalidateInputs #validateBox #validateButton").removeAttr("disabled");
				var continueButton = jQuery("#prevalidate_continue");
				enableContinueButton(continueButton);
		},
		error: function(request) {
			clearTimeout(timeout);
			timeout = setTimeout(function() {
				updateValidateList(id);
			}, 10000);
		}
	});	
}

function onBuildClick() {
	var bundleDir = jQuery("#manage-bundles_directoryName").val();
	var bundleName = jQuery("#buildBundle_bundleName").val();
	var startDate = jQuery("#startDate").val();
	var endDate = jQuery("#endDate").val();
	
	var valid = validateBundleBuildFields(bundleDir, bundleName, startDate, endDate);
	if(valid == false) {
		return;
	}
	buildBundle(bundleName, startDate, endDate);
}

function validateBundleBuildFields(bundleDir, bundleName, startDate, endDate) {
	var valid = true;
	var errors = "";
	if (bundleDir == undefined || bundleDir == null || bundleDir == "") {
		errors += "missing bundle directory" + "\n";
		valid = false;
	}
	if (bundleName == undefined || bundleName == null || bundleName == "") {
		errors += "missing bundle build name" + "\n";
		valid = false;
	}
	if (startDate == undefined || startDate == null || startDate == "") {
		errors += "missing bundle start date" + "\n";
		valid = false;
	}
	if (endDate == undefined || endDate == null || endDate == "") {
		errors += "missing bundle end date" + "\n";
		valid = false;
	}
	if(errors.length > 0) {
		alert(errors);
	}
	return valid;
}

function bundleUrl() {
	var id = jQuery("#buildBundle_id").text();
	jQuery("#buildBundle_exception").hide();
	jQuery("#buildBundle #buildBox #buildBundle_buildButton").attr("disabled", "disabled");
	jQuery("#buildBundle #buildBox #building").show().css("width","300px").css("margin-top", "20px");
	jQuery.ajax({
		url: "../../api/build/" + id + "/url",
		type: "GET",
		async: false,
		success: function(response) {
				var bundleResponse = eval(response);
				jQuery("#buildBundle #buildBox #buildBundle_resultLink #resultLink")
						.text(bundleResponse.bundleResultLink)
						.css("padding-left", "5px")
						.css("font-size", "12px")
						.addClass("adminLabel")
						.css("color", "green");
		},
		error: function(request) {
			clearTimeout(timeout);
			timeout = setTimeout(bundleUrl, 10000);
		}
	});
	var url = jQuery("#buildBundle #buildBox #buildBundle_resultLink #resultLink").text();
	if (url == null || url == "") {
		window.setTimeout(bundleUrl, 5000);
	}
}
function buildBundle(bundleName, startDate, endDate){
	var bundleDirectory = jQuery("#buildBundle_bundleDirectory").text();
	var email = jQuery("#buildBundle_email").val();
	if (email == "") { email = "null"; }
	jQuery.ajax({
		url: "../../api/build/" + bundleDirectory + "/" + bundleName + "/" + email + "/" + startDate + "/" + endDate + "/create",
		type: "GET",
		async: false,
		success: function(response) {
				var bundleResponse = eval(response);
				if (bundleResponse != undefined) {
					//display exception message if there is any
					if(bundleResponse.exception !=null) {
						alert(bundleResponse.exception.message);
					} else {
						jQuery("#buildBundle_resultList").html("calling...");
						jQuery("#buildBundle_id").text(bundleResponse.id);
						window.setTimeout(updateBuildStatus, 1000);
						bundleUrl();
					}
				} else {
					jQuery("#buildBundle_id").text(error);
					jQuery("#buildBundle_resultList").html("error");
				}
		},
		error: function(request) {
			alert("There was an error processing your request. Please try again");
		}
	});
}

function updateBuildStatus() {
	id = jQuery("#buildBundle_id").text();
	jQuery.ajax({
		url: "../../api/build/" + id + "/list",
		type: "GET",
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
			clearTimeout(timeout);
			timeout = setTimeout(updateBuildStatus, 10000);
		}
	});
}

// populate list of files that were result of building
function updateBuildList(id) {
	var summaryList = null;
		jQuery.ajax({
		url: "manage-bundles!downloadOutputFile.action",
		type: "POST",
		data: {"id": id,
			   "downloadFilename": "summary.csv"},
		async: false,
		success: function(response){
			summaryList = response;
			}
		});

		var lines = summaryList.split(/\n/);
		lines.pop(lines.length-1); // discard header
		var fileDescriptionMap = new Array();
		var fileCountMap = new Array();
		for (var i = 0; i < lines.length; i++) {
			var dataField = lines[i].split(',');

			fileDescriptionMap[dataField[0]] = dataField[1];
			fileCountMap[dataField[0]] = dataField[2];
		}
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
							var description = fileDescriptionMap[list[i]];
							var lineCount = fileCountMap[list[i]];
							if (description != undefined) {
								var encoded = encodeURIComponent(list[i]);
								txt = txt + "<li>" + description + ":" + "&nbsp;"
								+ lineCount + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
								+ "<img src=\"../../css/img/go-down-5.png\" />"
								+ "<a href=\"manage-bundles!downloadOutputFile.action?id="
								+ id+ "&downloadFilename=" 
								+ encoded + "\">" + ".csv" +  "</a></li>";
							}
						}
					}
				}
				txt = txt + "</ul>";
				jQuery("#buildBundle_fileList").html(txt).css("display", "block");
				jQuery("#buildBundle #downloadLogs").show().css("display", "block");
				jQuery("#buildBundle #downloadLogs #downloadButton").attr("href", "manage-bundles!buildOutputZip.action?id=" + id);
				jQuery("#buildBundle #buildBox #buildBundle_buildButton").removeAttr("disabled");
				var continueButton = jQuery("#build_continue");
				enableContinueButton(continueButton);
		},
		error: function(request) {
			clearTimeout(timeout);
			timeout = setTimeout(function() {
				updateBuildList(id);
			}, 10000);
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

