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
var currentReportDataset = "";
var currentReportBuildName = "";
var currentReportDate = "";
var compareToDataset =  "";
var compareToBuildName =  "";
var compareToDate = "";
var csrfParameter = "";
var csrfToken = "";
var csrfHeader = "";



jQuery(function() {
	//Initialize tabs
	jQuery("#tabs").tabs();
	csrfParameter = $("meta[name='_csrf_parameter']").attr("content");
	csrfHeader = $("meta[name='_csrf_header']").attr("content");
	csrfToken = $("meta[name='_csrf']").attr("content");

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
	jQuery("#currentReportDate").datepicker(
		{
			dateFormat: "yy-mm-dd"
		});
	jQuery("#compareToDate").datepicker(
		{
			dateFormat: "yy-mm-dd"

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
		jQuery("#buildBox #bundleStartDateHolder #startDatePicker").val(qs["startDate"]);
		jQuery("#buildBox #bundleEndDateHolder #endDatePicker").val(qs["endDate"]);
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
				var $element = jQuery("#createDirectory #directoryName");
				// only return the first selection, as multiple selections are possible
				$element.attr("value", names[0]);
				jQuery("#createDirectory #createDirectoryContents #createDirectoryResult").show().css("display","block");
				jQuery("#createDirectoryResult #resultImage").attr("src", "../../css/img/warning_16.png");
				jQuery("#createDirectoryMessage").text("Click Select button to load your directory")
					.css("font-weight", "bold").css("color", "red");
				//Enable select button
				enableSelectButton();
			}
		}
	});

	jQuery("#currentDatasetList").on("change", onCurrentDatasetChange);
	jQuery("#currentBuildNameList").on("change", onCurrentBuildNameChange);
	jQuery("#currentReportDate").on("change", onCurrentDateChange);
	jQuery("#compareToDatasetList").on("change", onCompareToDatasetChange);
	jQuery("#compareToBuildNameList").on("change", onCompareToBuildNameChange);
	jQuery("#compareToDate").on("change", onCompareToDateChange);

	jQuery("#compareCurrentDirectories").selectable({
		stop: function() {
			var names = $.map($('#compareListItem.ui-selected strong, this'), function(element, i) {
				return $(element).text();
			});
			if (names.length > 0) {
				var data = {};
				data[csrfParameter] = csrfToken;
				data["selectedBundleName"] = names[0];

				jQuery.ajax({
					url: "manage-bundles!existingBuildList.action",
					data: data,
					type: "GET",
					async: false,
					success: function(data) {
						$('#compareSelectedBuild').text('');
						$('#diffResult').text('');
						$.each(data, function(index, value) {
							$('#compareSelectedBuild').append(
								"<div id=\"compareBuildListItem\"><div class=\"listData\"><strong>"+value+"</strong></div></div>");
						});
					}
				})
			}
		}
	});

	jQuery("#compareSelectedBuild").selectable({
		stop: function() {
			var bundleNames = $.map($('#compareListItem.ui-selected strong, this'), function(element, i) {
				return $(element).text();
			});
			var buildNames = $.map($('#compareBuildListItem.ui-selected strong, this'), function(element, i) {
				return $(element).text();
			});
			if (buildNames.length > 0) {
				// Clear any previous results from the tables
				$('#diffResultsTable tr').slice(1).remove();
				$('#fixedRouteDiffTable tr').slice(1).remove();
				var data = {};
				data[csrfParameter] = csrfToken;
				data["datasetName"] = selectedDirectory;
				data["buildName"] = jQuery("#bundleBuildName").val();
				data["datasetName2"] = bundleNames[0];
				data["buildName2"] = buildNames[0];

				jQuery.ajax({
					url: "compare-bundles!diffResult.action",
					data: data,
					type: "GET",
					async: false,
					success: function(data) {
						$.each(data.diffResults, function(index, value) {
							// Skip first three rows of results
							if (index >= 3) {
								var diffRow = formatDiffRow(value);
								$("#diffResultsTable").append(diffRow);
							}
						});
						var baseBundle = selectedDirectory + " / " + jQuery("#bundleBuildName").val();
						var compareToBundle = bundleNames[0] + " / " + buildNames[0];
						$("#baseBundle").text(baseBundle + " (green)");
						$("#compareToBundle").text(compareToBundle + " (red)");
						$.each(data.fixedRouteDiffs, function(index, value) {
							var modeName = value.modeName;
							var modeClass = "";
							var modeFirstLineClass=" modeFirstLine";
							var addSpacer = true;
							if (value.srcCode == 1) {
								modeClass = "currentRpt";
							} else if (value.srcCode == 2) {
								modeClass = "selectedRpt";
							}
							$.each(value.routes, function(index2, value2) {
								var routeNum = value2.routeNum;
								var routeName = value2.routeName;
								var routeFirstLineClass=" routeFirstLine";
								addSpacer = false;
								if (index2 > 0) {
									modeName = "";
									modeFirstLineClass = "";
								}
								var routeClass = modeClass;
								if (value2.srcCode == 1) {
									routeClass = "currentRpt";
								} else if (value2.srcCode == 2) {
									routeClass = "selectedRpt";
								}
								$.each(value2.headsignCounts, function(headsignIdx, headsign) {
									var headsignName = headsign.headsign;
									var headsignBorderClass = "";
									if (headsignIdx > 0) {
										modeName = "";
										routeNum = "";
										routeName = "";
										modeFirstLineClass = "";
										routeFirstLineClass = "";
										headsignBorderClass = " headsignBorder";
										addSpacer = false;
									}
									var headsignClass = routeClass;
									if (headsign.srcCode == 1) {
										headsignClass = "currentRpt";
									} else if (headsign.srcCode == 2) {
										headsignClass = "selectedRpt";
									}
									$.each(headsign.dirCounts, function(dirIdx, direction) {
										var dirName = direction.direction;
										var dirBorderClass = "";
										if (dirIdx > 0) {
											modeName = "";
											routeNum = "";
											routeName = "";
											headsignName = "";
											modeFirstLineClass = "";
											routeFirstLineClass = "";
											headsignBorderClass = "";
											dirBorderClass = " dirBorder";
											addSpacer = false;
										}
										var dirClass = headsignClass;
										if (direction.srcCode == 1) {
											dirClass = "currentRpt";
										} else if (direction.srcCode == 2) {
											dirClass = "selectedRpt";
										}
										$.each(direction.stopCounts, function(index3, value3) {
											var stopCt = value3.stopCt;
											var stopClass = "";
											if (dirClass == "currentRpt") {
												stopClass = "currentStopCt";
											} else if (dirClass == "selectedRpt") {
												stopClass = "selectedStopCt";
											}
											if (value3.srcCode == 1) {
												stopClass = "currentStopCt";
											} else if (value3.srcCode == 2) {
												stopClass = "selectedStopCt";
											}
											var weekdayTrips = value3.tripCts[0];
											var satTrips = value3.tripCts[1];
											var sunTrips = value3.tripCts[2];
											if (index3 > 0) {
												modeName = "";
												modeFirstLineClass = "";
												routeNum = "";
												routeName = "";
												headsignName = "";
												dirName = "";
												routeFirstLineClass = "";
												headsignBorderClass = "";
												dirBorderClass = "";
												addSpacer = false;
											}
											if (index > 0 && headsignIdx == 0
												&& dirIdx == 0 && index3 == 0) {
												addSpacer = true;
											}
											if (addSpacer) {
												var new_spacer_row = '<tr class="spacer"> \
													<td></td> \
													<td></td> \
													<td></td> \
													<td></td> \
													<td></td> \
													<td></td> \
													<td></td> \
													<td></td> \
													<td></td> \
													</tr>';
												$('#fixedRouteDiffTable').append(new_spacer_row);
											}
											var new_row = '<tr class="fixedRouteDiff' + modeFirstLineClass + routeFirstLineClass + '"> \
												<td class="' + modeClass + ' modeName" >' + modeName + '</td> \
												<td class="' + routeClass + routeFirstLineClass + ' rtNum" >' + routeNum + '</td> \
												<td class="' + routeClass + routeFirstLineClass + '">' + routeName + '</td> \
												<td class="' + headsignClass + routeFirstLineClass + headsignBorderClass + '">' + headsignName + '</td> \
												<td class="' + dirClass + routeFirstLineClass + headsignBorderClass + dirBorderClass + '">' + dirName + '</td> \
												<td class="' + stopClass + routeFirstLineClass + headsignBorderClass + dirBorderClass + '">' + stopCt + '</td> \
												<td class="' + stopClass + routeFirstLineClass + headsignBorderClass + dirBorderClass + '">' + weekdayTrips + '</td> \
												<td class="' + stopClass + routeFirstLineClass + headsignBorderClass + dirBorderClass + '">' + satTrips + '</td> \
												<td class="' + stopClass + routeFirstLineClass + headsignBorderClass + dirBorderClass + '">' + sunTrips + '</td> \
												</tr>';
											$('#fixedRouteDiffTable').append(new_row);
										});
									});
								});
							});
						});
						// Add bottom border to reprot
						var new_spacer_row = '<tr class="spacer"> \
							<td></td> \
							<td></td> \
							<td></td> \
							<td></td> \
							<td></td> \
							<td></td> \
							<td></td> \
							<td></td> \
							</tr>';
						$('#fixedRouteDiffTable').append(new_spacer_row);
					}
				})
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

	//Enable or disable create/select button when user enters/removes directory name
	//Using bind() with propertychange event as live() does not work in IE for unknown reasons
	jQuery("#createDirectoryContents #directoryName").bind("input propertychange", function() {
		var text = jQuery("#createDirectory #directoryName").val();
		if(text.length > 0) {
			enableSelectButton();
		} else {
			disableSelectButton();
			jQuery("#createDirectory #createDirectoryContents #createDirectoryResult").hide();
			disableContinueButton(jQuery("#create_continue"));
		}
	});

	//toggle bundle deploy progress list
	jQuery("#deployBundle #deployBundle_progress #expand").bind({
		'click' : toggleDeployBundleResultList});

	//Handle deploy button click event
	jQuery("#deployBundle_deployButton").click(onDeployClick);
	jQuery("#deployBundle_listButton").click(onDeployListClick);
	onDeployListClick();
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
	var bundleDir = jQuery("#createDirectory #directoryName").val();
	var actionName = "selectDirectory";

	if (jQuery("#create").is(":checked")) {
		actionName = "createDirectory";
	}
	jQuery.ajax({
		url: "manage-bundles!" + actionName + ".action?ts=" +new Date().getTime(),
		type: "GET",
		data: {"directoryName" : bundleDir},
		async: false,
		success: function(response) {
			var status = response;
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
				jQuery("#s3_location").text(status.bucketName);
				jQuery("#gtfs_location").text(bundleDir + "/" + status.gtfsPath + " directory");
				jQuery("#stif_location").text(bundleDir + "/" + status.stifPath + " directory");
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

function disableContinueButton(continueButton) {
	jQuery(continueButton).attr("disabled", "disabled")
		.removeClass("submit_enabled").addClass("submit_disabled");
}

function enableSelectButton() {
	jQuery("#createDirectory #createDirectoryContents #directoryButton").removeAttr("disabled")
		.css("color", "#666");
}

function disableSelectButton() {
	jQuery("#createDirectory #createDirectoryContents #directoryButton").attr("disabled", "disabled")
		.css("color", "#999");
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

function toggleDeployBundleResultList() {
	var $image = jQuery("#deployBundle #deployBundle_progress #expand");
	changeImageSrc($image);
	//Toggle progress result list
	jQuery("#deployBundle #deployBundle_resultList").toggle();
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
	jQuery("#createDirectory #directoryName").val("");
	jQuery("#createDirectory #createDirectoryContents #directoryButton").attr("disabled", "disabled")
		.css("color", "#999");

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
		url: "../../api/validate/" + bundleDirectory + "/" + bundleName + "/create?ts=" +new Date().getTime(),
		type: "GET",
		async: false,
		success: function(response) {
			var bundleResponse = response;
			if (bundleResponse != undefined) {
				jQuery("#prevalidate_id").text(bundleResponse.id);
				//jQuery("#prevalidate_resultList").text("calling...");
				window.setTimeout(updateValidateStatus, 5000);
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
		url: "../../api/validate/" + id + "/list?ts=" +new Date().getTime(),
		type: "GET",
		async: false,
		success: function(response) {
			var txt = "<ul>";
			var bundleResponse = response;
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
				window.setTimeout(updateValidateStatus, 5000); // recurse
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
		url: "manage-bundles!fileList.action?ts=" +new Date().getTime(),
		type: "GET",
		data: {"id": id},
		async: false,
		success: function(response) {
			var txt = "<ul>";

			var list = response;
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
	var bundleDir = jQuery("#createDirectory #directoryName").val();
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
		url: "../../api/build/" + id + "/url?ts=" +new Date().getTime(),
		type: "GET",
		async: false,
		success: function(response) {
			var bundleResponse = response;
			if(bundleResponse.exception !=null) {
				jQuery("#buildBundle #buildBox #buildBundle_resultLink #resultLink")
					.text("(exception)")
					.css("padding-left", "5px")
					.css("font-size", "12px")
					.addClass("adminLabel")
					.css("color", "red");
			} else {
				jQuery("#buildBundle #buildBox #buildBundle_resultLink #resultLink")
					.text(bundleResponse.bundleResultLink)
					.css("padding-left", "5px")
					.css("font-size", "12px")
					.addClass("adminLabel")
					.css("color", "green");
			}
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
		url: "../../api/build/" + bundleDirectory + "/" + bundleName + "/" + email + "/" + startDate + "/" + endDate + "/create?ts=" +new Date().getTime(),
		type: "GET",
		async: false,
		success: function(response) {
			var bundleResponse = response;
			if (bundleResponse != undefined) {
				//display exception message if there is any
				if(bundleResponse.exception !=null) {
					alert(bundleResponse.exception.message);
				} else {
					jQuery("#buildBundle_resultList").html("calling...");
					jQuery("#buildBundle_id").text(bundleResponse.id);
					window.setTimeout(updateBuildStatus, 5000);
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
		url: "../../api/build/" + id + "/list?ts=" +new Date().getTime(),
		type: "GET",
		async: false,
		success: function(response) {
			var txt = "<ul>";
			var bundleResponse = response;
			if (bundleResponse == null) {
				jQuery("#buildBundle_buildProgress").text("Bundle Status Unkown!");
				jQuery("#buildBundle #buildBox #building #buildProgress").attr("src","../../css/img/dialog-warning-4.png");
				jQuery("#buildBundle_resultList").html("unknown id=" + id);
			}
			var size = bundleResponse.statusList.length;
			if (size > 0) {
				for (var i=0; i<size; i++) {
					txt = txt + "<li>" + bundleResponse.statusList[i] + "</li>";
				}
			}
			if (bundleResponse.complete == false) {
				window.setTimeout(updateBuildStatus, 5000); // recurse
			} else {
				jQuery("#buildBundle_buildProgress").text("Bundle Complete!");
				jQuery("#buildBundle #buildBox #building #buildingProgress").attr("src","../../css/img/dialog-accept-2.png");
				updateBuildList(id);

			}
			txt = txt + "</ul>";
			jQuery("#buildBundle_resultList").html(txt).css("font-size", "12px");
			// check for exception
			if (bundleResponse.exception != null) {
				jQuery("#buildBundle_buildProgress").text("Bundle Failed!");
				jQuery("#buildBundle #buildBox #building #buildingProgress").attr("src","../../css/img/dialog-warning-4.png");
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
		url: "manage-bundles!downloadOutputFile.action?ts=" +new Date().getTime(),
		type: "GET",
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
		url: "manage-bundles!buildList.action?ts=" +new Date().getTime(),
		type: "GET",
		data: {"id": id},
		async: false,
		success: function(response) {
			var txt = "<ul>";

			var list = response;
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
			// append log file
			txt = txt + "<li>" + "Bundle Builder Log:" + "&nbsp;"
				+ " " + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
				+ "<img src=\"../../css/img/go-down-5.png\" />"
				+ "<a href=\"manage-bundles!downloadOutputFile.action?id="
				+ id+ "&downloadFilename="
				+ encodeURIComponent("bundleBuilder.out.txt") + "\">" + ".txt" +  "</a></li>";

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


function onDeployClick() {
	deployBundle();
}

function deployBundle(){
	var environment = jQuery("#deploy_environment").text();

	jQuery.ajax({
		url: "../../api/bundle/deploy/from/" + environment + "?ts=" +new Date().getTime(),
		type: "GET",
		async: false,
		success: function(response) {
			var bundleResponse = response;
			if (bundleResponse != undefined) {
				// the header is set wrong for the proxied object, run eval to correct
				if (typeof response=="string") {
					bundleResponse = eval('(' + response + ')');
				}
				jQuery("#deployBundle_resultList").html("calling...");
				jQuery("#deployBundle_id").text(bundleResponse.id);
				jQuery("#deployBundle #requestLabels").show().css("display","block");
				jQuery("#deployContentsHolder #deployBox #deploying").show().css("display","block");
				jQuery("#deployBundle_deployProgress").text("Deploying ...");
				jQuery("#deployContentsHolder #deployBox #deploying #deployingProgress").attr("src","../../css/img/ajax-loader.gif");
				window.setTimeout(updateDeployStatus, 5000);
			} else {
				jQuery("#deployBundle_id").text(error);
				jQuery("#deployBundle_resultList").html("error");
			}
		},
		error: function(request) {
			alert("There was an error processing your request. Please try again");
		}
	});
}

function updateDeployStatus() {
	id = jQuery("#deployBundle_id").text();
	jQuery.ajax({
		url: "../../api/bundle/deploy/status/" + id + "/list?ts=" +new Date().getTime(),
		type: "GET",
		async: false,
		success: function(response) {
			var txt = "<ul>";
			var bundleResponse = response;
			if (bundleResponse == null) {
				jQuery("#deployBundle_deployProgress").text("Deploy Complete!");
				jQuery("#deployContentsHolder #deployBox #deploying #deployingProgress").attr("src","../../css/img/dialog-warning-4.png");
				jQuery("#deployBundle_resultList").html("unknown id=" + id);
				return;
			}
			// the header is set wrong for the proxied object, run eval to correct
			if (typeof response=="string") {
				bundleResponse = eval('(' + response + ')');
			}
			if (bundleResponse.status != "complete" && bundleResponse.status != "error") {
				window.setTimeout(updateDeployStatus, 5000); // recurse
			} else {
				toggleDeployBundleResultList();
				jQuery("#bundleResultsHolder #bundleResults #deployBundle_progress").show().css("display","block");
				jQuery("#bundleResultsHolder #bundleResults #deployBundle_resultList").show().css("display","block");
				if (bundleResponse.status == "complete") {
					jQuery("#deployContentsHolder #deployBox #deploying #deployingProgress").attr("src","../../css/img/dialog-accept-2.png");
					jQuery("#deployBundle_deployProgress").text("Deploy Complete!");
					// set resultList to bundleNames list
					var size = bundleResponse.bundleNames.length;
					if (size > 0) {
						for (var i=0; i<size; i++) {
							txt = txt + "<li>" + bundleResponse.bundleNames[i] + "</li>";
						}
					}
				} else {
					jQuery("#deployContentsHolder #deployBox #deploying #deployingProgress").attr("src","../../css/img/dialog-warning-4.png");
					jQuery("#deployBundle_deployProgress").text("Deploy Failed!");
					// we've got an error
					txt = txt + "<li><font color=\"red\">ERROR!  Please consult the logs and check the "
						+ "filesystem permissions before continuing</font></li>";
				}
			}
			txt = txt + "</ul>";
			jQuery("#deployBundle_resultList").html(txt).css("font-size", "12px");
		},
		error: function(request) {
			clearTimeout(timeout);
			toggleDeployBundleResultList();
			jQuery("#deployContentsHolder #deployBox #deploying #deployingProgress").attr("src","../../css/img/dialog-warning-4.png");
			jQuery("#deployBundle_deployProgress").text("Deploy Failed!");
			jQuery("#bundleResultsHolder #bundleResults #deployBundle_progress").show().css("display","block");
			jQuery("#bundleResultsHolder #bundleResults #deployBundle_resultList").show().css("display","block");

			// error out on a 500 error, the session will be lost so it will not recover
			var txt = "<ul>";
			txt = txt + "<li><font color=\"red\">The server returned an internal error.  Please consult the logs"
				+ " or retry your request</font></li>";
			txt = txt + "</ul>";
			jQuery("#deployBundle_resultList").html(txt).css("font-size", "12px");
		}
	});
}


function onDeployListClick(){
	var environment = jQuery("#deploy_environment").text();
	jQuery.ajax({
		url: "../../api/bundle/deploy/list/" + environment + "?ts=" +new Date().getTime(),
		type: "GET",
		async: false,
		success: function(response) {
			var bundleResponse = response;
			if (bundleResponse != undefined) {
				var txt = "<ul>";
				// the header is set wrong for the proxied object, run eval to correct
				if (typeof response=="string") {
					bundleResponse = eval('(' + response + ')');
				}
				// parse array of bundle names
				var size = bundleResponse.length;
				if (size > 0) {
					for (var i=0; i<size; i++) {
						txt = txt + "<li>" + bundleResponse[i] + "</li>";
					}
				}
				txt = txt + "</ul>";
				jQuery("#deployBundle_bundleList").html(txt).css("font-size", "12px");

			}
		},
		error: function(request) {
			alert("There was an error processing your request. Please try again");
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





/**
 * Functions used with the Compare tab for generating reports on differences
 * between two bundle builds.
 */
function onCurrentDatasetChange() {
	// Clear any previous reports
	$("#diffResultsTable tbody").empty();
	$('#fixedRouteDiffTable tbody').empty();

	if ($("#currentDatasetList option:selected").val() == "0") {
		console.log("Current Dataset:" + $("#currentDatasetList option:selected").text());
		resetCurrentReportDataset();
	} else {
		console.log("Current Dataset:" + $("#currentDatasetList option:selected").text());
		currentReportDataset = $("#currentDatasetList option:selected").text();
		currentReportBuildName = "";
		var buildNameList = getExistingBuildList(currentReportDataset);
		initBuildNameList($("#currentBuildNameList"), buildNameList);
	}
}

function onCurrentBuildNameChange() {
	// Clear any previous reports
	$("#diffResultsTable tbody").empty();
	$('#fixedRouteDiffTable tbody').empty();

	if ($("#currentBuildNameList option:selected").val() == 0) {
		currentReportBuildName = "";
	} else {
		currentReportBuildName = $("#currentBuildNameList option:selected").text();
		if (currentReportDataset && currentReportBuildName
			&& compareToDataset && compareToBuildName) {
			buildDiffReport();
		}
	}
}

function onCurrentDateChange(){
	currentReportDate = jQuery("#currentReportDate").val();
	if (currentReportDate == 0) {
		onCompareToBuildNameChange();
	} else {
		// Clear any previous reports
		$("#diffResultsTable tbody").empty();
		$('#fixedRouteDiffTable tbody').empty();
		if (currentReportDataset && currentReportBuildName
			&& compareToDataset && compareToBuildName
			&& currentReportDate && compareToDate) {
			buildDiffReport();
			buildDailyDiffReport();
		}
	}
}


function onCompareToDatasetChange() {
	// Clear any previous reports
	$("#diffResultsTable tbody").empty();
	$('#fixedRouteDiffTable tbody').empty();

	if ($("#compareToDatasetList option:selected").val() == "0") {
		resetCompareToDataset();
	} else {
		compareToDataset = $("#compareToDatasetList option:selected").text();
		compareToBuildName = "";
		var buildNameList = getExistingBuildList(compareToDataset);
		initBuildNameList($("#compareToBuildNameList"), buildNameList);
	}
}

function onCompareToBuildNameChange() {
	// Clear any previous reports
	$("#diffResultsTable tbody").empty();
	$('#fixedRouteDiffTable tbody').empty();

	if ($("#compareToBuildNameList option:selected").val() == 0) {
		compareToBuildName = "";
	} else {
		compareToBuildName = $("#compareToBuildNameList option:selected").text();
		if (currentReportDataset && currentReportBuildName
			&& compareToDataset && compareToBuildName) {
			buildDiffReport();
		}
	}
}
function onCompareToDateChange(){
	compareToDate = jQuery("#compareToDate").val();
	if (compareToDate == 0) {
		onCompareToBuildNameChange();
	} else {
		// Clear any previous reports
		$("#diffResultsTable tbody").empty();
		$('#fixedRouteDiffTable tbody').empty();
		if (currentReportDataset && currentReportBuildName
			&& compareToDataset && compareToBuildName
			&& currentReportDate && compareToDate) {
			buildDiffReport();
			buildDailyDiffReport();
		}
	}
}

// Called when a dataset is selected on the Choose tab.
function updateFixedRouteParams(datasetName) {
	// Clear any previous reports
	$("#diffResultsTable tbody").empty();
	$('#fixedRouteDiffTable tbody').empty();

	currentReportDataset = datasetName;
	currentReportBuildName = $("#bundleBuildName").val();
	// Select the current dataset name
	$("#currentDatasetList option").filter(function() {
		return $(this).text() == datasetName;
	}).prop('selected', true);
	// Populate list of build names for this dataset and select the current one
	var buildNameList = getExistingBuildList(datasetName);
	initBuildNameList($("#currentBuildNameList"), buildNameList);
	$("#currentBuildNameList option").filter(function() {
		return $(this).text() == $("#bundleBuildName").val();
	}).prop('selected', true);

	resetCompareToDataset();
	return;
}

function getExistingBuildList(datasetName) {
	var buildNameList;
	var useArchivedGtfs = jQuery("#useArchiveCheckbox").is(":checked");
	var data = {};
	data[csrfParameter] = csrfToken;
	data["selectedBundleName"] = datasetName;
	data["useArchivedGtfs"] = useArchivedGtfs;

	if (datasetName) {
		jQuery.ajax({
			url: "manage-bundles!existingBuildList.action",
			data: data,
			type: "POST",
			async: false,
			success: function(data) {
				buildNameList=data;
			}
		})
	}
	return buildNameList;
}

function initBuildNameList($buildNameList, buildNameMap) {
	console.log(buildNameMap);
	var row_0 = '<option value="0">Select a build name</option>';
	$buildNameList.find('option').remove().end().append(row_0);
	var i;
	var getKeys = function(buildNameMap) {
		var keys = [];
		for(var key in buildNameMap){
			keys.push(key);
		}
		return keys;
	}
	for (var key in buildNameMap) {
		var name = key;
		var gid = buildNameMap[key];
		//var nextRow = '<option value="' + (i+1) + '">' + buildNameList[i] + '</option>';
		var nextRow = '<option value="' + buildNameMap[key] + '">' + key + '</option>';
		$buildNameList.append(nextRow);
	}
	$buildNameList.val("0");
	return;
}

function resetCurrentReportDataset() {
	if (!jQuery("#useArchiveCheckbox").is(":checked")) {
		currentReportDataset = "";
		currentReportBuildName = "";
		$("#currentDatasetList").val("0");
		var row_0 = '<option value="0">Select a build name</option>';
		$("#currentBuildNameList").find('option').remove().end().append(row_0);
	} else {
		currentArchivedReportDataset = "";
		currentArchivedReportBuildName = "";
		$("#currentArchivedDatasetList").val("0");
		var row_0 = '<option value="0">Select an archived build name</option>';
		$("#currentArchivedBuildNameList").find('option').remove().end().append(row_0);
	}
}
function resetCompareToDataset() {
	if (!jQuery("#useArchiveCheckbox").is(":checked")) {
		compareToDataset = "";
		compareToBuildName = "";
		$("#compareToDatasetList").val("0");
		var row_0 = '<option value="0">Select a build name</option>';
		$("#compareToBuildNameList").find('option').remove().end().append(row_0);
	} else {
		compareToArchivedDataset = "";
		compareToArchivedBuildName = "";
		$("#compareToArchivedDatasetList").val("0");
		var row_0 = '<option value="0">Select an archived build name</option>';
		$("#compareToBuildNameList").find('option').remove().end().append(row_0);
	}
}

function addToDatasetLists(directoryName) {
	var exists = false;
	$('#currentDatasetList option').each(function() {
		if (this.text == directoryName) {
			exists = true;
		}
	});

	if (!exists) {
		var datasetAdded = false;
		$("#currentDatasetList option").each(function() {
			if (this.value > 0 && (directoryName < this.text)) {
				var newRow = '<option value=' + this.value + '>' + directoryName + '</option>';
				$(this).before(newRow);
				datasetAdded = true;
				return false;
			}
		});
		$("#compareToDatasetList option").each(function() {
			if (this.value > 0 && (directoryName < this.text)) {
				var newRow = '<option value=' + this.value + '>' + directoryName + '</option>';
				$(this).first().before(newRow);
				return false;
			}
		});
		if (!datasetAdded) {
			var datasetVal = $("#currentDatasetList > option").length;
			var newRow = '<option value=' + datasetVal + '>Select a build name</option>';
			$("#currentDatasetList").find('option').end().append(newRow);
			$("#compareToDatasetList").find('option').end().append(newRow);
		}
	}
}

function buildDiffReport() {
	// Clear any previous reports
	$("#diffResultsTable tbody").empty();
	$('#fixedRouteDiffTable tbody').empty();
	var useArchived = jQuery("#useArchiveCheckbox").is(":checked");
	if (!useArchived) {
		var dataset_1 = currentReportDataset;
		var dataset_1_build_id = 0;
		var dataset_2 = compareToDataset;
		var dataset_2_build_id = 0;
		var buildName_1 = currentReportBuildName;
		var buildName_2 = compareToBuildName;
		var buildDate1 = currentReportDate;
		var buildDate2 = compareToDate;
	} else {
		$('#Compare #buildingReportDiv').show();
		var dataset_1 = currentArchivedReportDataset;
		var dataset_1_build_id = $('#currentArchivedBuildNameList option:selected').val();
		var dataset_2 = compareToArchivedDataset;
		var dataset_2_build_id = $('#compareToArchivedBuildNameList option:selected').val();
		var buildName_1 = currentArchivedReportBuildName;
		var buildName_2 = compareToArchivedBuildName;
	}
	var data = {};
	data[csrfParameter] = csrfToken;
	data["useArchived"] = useArchived;
	data["datasetName"] = dataset_1;
	data["dataset_1_build_id"] = dataset_1_build_id;
	data["buildName"] =buildName_1;
	data["buildDate"] = "";
	data["datasetName2"] = dataset_2;
	data["dataset_2_build_id"] =dataset_2_build_id;
	data["buildName2"] = buildName_2;
	data["buildDate2"] = "";

	jQuery.ajax({
		url: "compare-bundles!diffResult.action",
		data: data,
		type: "POST",
		async: false,
		success: function(data) {
			console.log(data);
			$('#Compare #buildingReportDiv').hide();
			$.each(data.diffResults, function(index, value) {
				// Skip first three rows of results
				if (index >= 3) {
					var diffRow = formatDiffRow(value);
					$("#diffResultsTable").append(diffRow);
				}
			});
			var baseBundle = dataset_1 + " / " + buildName_1;
			var compareToBundle = dataset_2 + " / " + buildName_2;
			$("#baseBundle").text(baseBundle + " (green)");
			$("#compareToBundle").text(compareToBundle + " (red)");
			$.each(data.fixedRouteDiffs, function(index, value) {
				var modeName = value.modeName;
				var modeClass = "";
				var modeFirstLineClass=" modeFirstLine";
				var addSpacer = true;
				if (value.srcCode == 1) {
					modeClass = "currentRpt";
				} else if (value.srcCode == 2) {
					modeClass = "selectedRpt";
				}
				$.each(value.routes, function(index2, value2) {
					var routeNum = value2.routeNum;
					var routeName = value2.routeName;
					var routeFirstLineClass=" routeFirstLine";
					addSpacer = false;
					if (index2 > 0) {
						modeName = "";
						modeFirstLineClass = "";
					}
					var routeClass = modeClass;
					if (value2.srcCode == 1) {
						routeClass = "currentRpt";
					} else if (value2.srcCode == 2) {
						routeClass = "selectedRpt";
					}
					$.each(value2.headsignCounts, function(headsignIdx, headsign) {
						var headsignName = headsign.headsign;
						var headsignBorderClass = "";
						if (headsignIdx > 0) {
							modeName = "";
							routeNum = "";
							routeName = "";
							modeFirstLineClass = "";
							routeFirstLineClass = "";
							headsignBorderClass = " headsignBorder";
							addSpacer = false;
						}
						var headsignClass = routeClass;
						if (headsign.srcCode == 1) {
							headsignClass = "currentRpt";
						} else if (headsign.srcCode == 2) {
							headsignClass = "selectedRpt";
						}
						$.each(headsign.dirCounts, function(dirIdx, direction) {
							var dirName = direction.direction;
							var dirBorderClass = "";
							if (dirIdx > 0) {
								modeName = "";
								routeNum = "";
								routeName = "";
								headsignName = "";
								modeFirstLineClass = "";
								routeFirstLineClass = "";
								headsignBorderClass = "";
								dirBorderClass = " dirBorder";
								addSpacer = false;
							}
							var dirClass = headsignClass;
							if (direction.srcCode == 1) {
								dirClass = "currentRpt";
							} else if (direction.srcCode == 2) {
								dirClass = "selectedRpt";
							}
							$.each(direction.stopCounts, function(index3, value3) {
								var stopCt = value3.stopCt;
								var stopClass = "";
								if (dirClass == "currentRpt") {
									stopClass = "currentStopCt";
								} else if (dirClass == "selectedRpt") {
									stopClass = "selectedStopCt";
								}
								if (value3.srcCode == 1) {
									stopClass = "currentStopCt";
								} else if (value3.srcCode == 2) {
									stopClass = "selectedStopCt";
								}
								var weekdayTrips = value3.tripCts[0];
								var satTrips = value3.tripCts[1];
								var sunTrips = value3.tripCts[2];
								if (index3 > 0) {
									modeName = "";
									modeFirstLineClass = "";
									routeNum = "";
									routeName = "";
									headsignName = "";
									dirName = "";
									routeFirstLineClass = "";
									headsignBorderClass = "";
									dirBorderClass = "";
									addSpacer = false;
								}
								if (index > 0 && headsignIdx == 0
									&& dirIdx == 0 && index3 == 0) {
									addSpacer = true;
								}
								if (addSpacer) {
									var new_spacer_row = '<tr class="spacer"> \
										<td></td> \
										<td></td> \
										<td></td> \
										<td></td> \
										<td></td> \
										<td></td> \
										<td></td> \
										<td></td> \
										<td></td> \
										</tr>';
									$('#fixedRouteDiffTable').append(new_spacer_row);
								}
								var new_row = '<tr class="fixedRouteDiff' + modeFirstLineClass + routeFirstLineClass + '"> \
									<td class="' + modeClass + ' modeName" >' + modeName + '</td> \
									<td class="' + routeClass + routeFirstLineClass + ' rtNum" >' + routeNum + '</td> \
									<td class="' + routeClass + routeFirstLineClass + '">' + routeName + '</td> \
									<td class="' + headsignClass + routeFirstLineClass + headsignBorderClass + '">' + headsignName + '</td> \
									<td class="' + dirClass + routeFirstLineClass + headsignBorderClass + dirBorderClass + '">' + dirName + '</td> \
									<td class="' + stopClass + routeFirstLineClass + headsignBorderClass + dirBorderClass + '">' + stopCt + '</td> \
									<td class="' + stopClass + routeFirstLineClass + headsignBorderClass + dirBorderClass + '">' + weekdayTrips + '</td> \
									<td class="' + stopClass + routeFirstLineClass + headsignBorderClass + dirBorderClass + '">' + satTrips + '</td> \
									<td class="' + stopClass + routeFirstLineClass + headsignBorderClass + dirBorderClass + '">' + sunTrips + '</td> \
									</tr>';
								$('#fixedRouteDiffTable').append(new_row);
							});
						});
					});
				});
			});
			// Add bottom border to reprot
			var new_spacer_row = '<tr class="spacer"> \
				<td></td> \
				<td></td> \
				<td></td> \
				<td></td> \
				<td></td> \
				<td></td> \
				<td></td> \
				<td></td> \
				<td></td> \
				</tr>';
			$('#fixedRouteDiffTable').append(new_spacer_row);
		}
	})
}





function buildDailyDiffReport() {
	// Clear any previous reports
	$("#diffResultsTable tbody").empty();
	$('#dailyRouteDiffTable tbody').empty();
	var useArchived = jQuery("#useArchiveCheckbox").is(":checked");
	if (!useArchived) {
		var dataset_1 = currentReportDataset;
		var dataset_1_build_id = 0;
		var dataset_2 = compareToDataset;
		var dataset_2_build_id = 0;
		var buildName_1 = currentReportBuildName;
		var buildName_2 = compareToBuildName;
		var buildDate1 = currentReportDate;
		var buildDate2 = compareToDate;
	} else {
		$('#Compare #buildingReportDiv').show();
		var dataset_1 = currentArchivedReportDataset;
		var dataset_1_build_id = $('#currentArchivedBuildNameList option:selected').val();
		var dataset_2 = compareToArchivedDataset;
		var dataset_2_build_id = $('#compareToArchivedBuildNameList option:selected').val();
		var buildName_1 = currentArchivedReportBuildName;
		var buildName_2 = compareToArchivedBuildName;
	}
	var data = {};
	data[csrfParameter] = csrfToken;
	data["useArchived"] = useArchived;
	data["datasetName"] = dataset_1;
	data["dataset_1_build_id"] = dataset_1_build_id;
	data["buildName"] = buildName_1;
	data["buildDate"] = buildDate1;
	data["datasetName2"] = dataset_2;
	data["dataset_2_build_id"] = dataset_2_build_id;
	data["buildName2"] = buildName_2;
	data["buildDate2"] = buildDate2;

	jQuery.ajax({
		url: "compare-bundles!diffResult.action",
		data: data,
		type: "POST",
		async: false,
		success: function (data) {
			console.log(data);
			$('#Compare #buildingReportDiv').hide();
			$.each(data.diffResults, function (index, value) {
				// Skip first three rows of results
				if (index >= 3) {
					var diffRow = formatDiffRow(value);
					$("#diffResultsTable").append(diffRow);
				}
			});
			var baseBundle = dataset_1 + " / " + buildName_1;
			var compareToBundle = dataset_2 + " / " + buildName_2;
			$("#baseBundle").text(baseBundle + " (green)");
			$("#compareToBundle").text(compareToBundle + " (red)");
			$.each(data.fixedRouteDiffs, function (index, value) {
				var modeName = value.modeName;
				var modeClass = "";
				var modeFirstLineClass = " modeFirstLine";
				var addSpacer = true;
				if (value.srcCode == 1) {
					modeClass = "currentRpt";
				} else if (value.srcCode == 2) {
					modeClass = "selectedRpt";
				}
				$.each(value.routes, function (index2, value2) {
					var routeNum = value2.routeNum;
					var routeName = value2.routeName;
					var routeFirstLineClass = " routeFirstLine";
					addSpacer = false;
					if (index2 > 0) {
						modeName = "";
						modeFirstLineClass = "";
					}
					var routeClass = modeClass;
					if (value2.srcCode == 1) {
						routeClass = "currentRpt";
					} else if (value2.srcCode == 2) {
						routeClass = "selectedRpt";
					}
					$.each(value2.headsignCounts, function (headsignIdx, headsign) {
						var headsignName = headsign.headsign;
						var headsignBorderClass = "";
						if (headsignIdx > 0) {
							modeName = "";
							routeNum = "";
							routeName = "";
							modeFirstLineClass = "";
							routeFirstLineClass = "";
							headsignBorderClass = " headsignBorder";
							addSpacer = false;
						}
						var headsignClass = routeClass;
						if (headsign.srcCode == 1) {
							headsignClass = "currentRpt";
						} else if (headsign.srcCode == 2) {
							headsignClass = "selectedRpt";
						}
						$.each(headsign.dirCounts, function (dirIdx, direction) {
							var dirName = direction.direction;
							var dirBorderClass = "";
							if (dirIdx > 0) {
								modeName = "";
								routeNum = "";
								routeName = "";
								headsignName = "";
								modeFirstLineClass = "";
								routeFirstLineClass = "";
								headsignBorderClass = "";
								dirBorderClass = " dirBorder";
								addSpacer = false;
							}
							var dirClass = headsignClass;
							if (direction.srcCode == 1) {
								dirClass = "currentRpt";
							} else if (direction.srcCode == 2) {
								dirClass = "selectedRpt";
							}
							$.each(direction.stopCounts, function (index3, value3) {
								var stopCt = value3.stopCt;
								var stopClass = "";
								if (dirClass == "currentRpt") {
									stopClass = "currentStopCt";
								} else if (dirClass == "selectedRpt") {
									stopClass = "selectedStopCt";
								}
								if (value3.srcCode == 1) {
									stopClass = "currentStopCt";
								} else if (value3.srcCode == 2) {
									stopClass = "selectedStopCt";
								}
								var weekdayTrips = value3.tripCts[0];
								var satTrips = value3.tripCts[1];
								var sunTrips = value3.tripCts[2];
								if (index3 > 0) {
									modeName = "";
									modeFirstLineClass = "";
									routeNum = "";
									routeName = "";
									headsignName = "";
									dirName = "";
									routeFirstLineClass = "";
									headsignBorderClass = "";
									dirBorderClass = "";
									addSpacer = false;
								}
								if (index > 0 && headsignIdx == 0
									&& dirIdx == 0 && index3 == 0) {
									addSpacer = true;
								}
								if (addSpacer) {
									var new_spacer_row = '<tr class="spacer"> \
										<td></td> \
										<td></td> \
										<td></td> \
										<td></td> \
										<td></td> \
										<td></td> \
										<td></td> \
										</tr>';
									$('#dailyRouteDiffTable').append(new_spacer_row);
								}
								var new_row = '<tr class="dailyRouteDiff' + modeFirstLineClass + routeFirstLineClass + '"> \
									<td class="' + modeClass + ' modeName" >' + modeName + '</td> \
									<td class="' + routeClass + routeFirstLineClass + ' rtNum" >' + routeNum + '</td> \
									<td class="' + routeClass + routeFirstLineClass + '">' + routeName + '</td> \
									<td class="' + headsignClass + routeFirstLineClass + headsignBorderClass + '">' + headsignName + '</td> \
									<td class="' + dirClass + routeFirstLineClass + headsignBorderClass + dirBorderClass + '">' + dirName + '</td> \
									<td class="' + stopClass + routeFirstLineClass + headsignBorderClass + dirBorderClass + '">' + stopCt + '</td> \
									<td class="' + stopClass + routeFirstLineClass + headsignBorderClass + dirBorderClass + '">' + weekdayTrips + '</td> \
									</tr>';
								$('#dailyRouteDiffTable').append(new_row);
							});
						});
					});
				});
			});
			// Add bottom border to reprot
			var new_spacer_row = '<tr class="spacer"> \
				<td></td> \
				<td></td> \
				<td></td> \
				<td></td> \
				<td></td> \
				<td></td> \
				<td></td> \
				</tr>';
			$('#dailyRouteDiffTable').append(new_spacer_row);
		}
	})
}

/*
 * This is used in the Compare tab to format each row for the
 * Diff Results table.
 */
function formatDiffRow(value) {
	var tokens = value.split(",");
	var newRow = "";
	var dataClass = "redListData";
	var testChar = tokens[0].charAt(0);
	if (tokens[0].charAt(0) == '+') {
		dataClass = "greenListData";
	}
	var teststr = tokens[0].substr(1);
	tokens[0] = tokens[0].substr(1);
	var dataItems = "";
	tokens.forEach(function(entry, idx) {
		if (entry == "null") {
			entry = "";
		}
		var tdClass = "";
		if (!isNaN(entry) && idx > 0) {
			tdClass = " class=numericTd ";
		}
		dataItems += "<td" + tdClass + ">" + entry + "</td>";
	});
	var newRow = "<tr class=" + dataClass + " >"
		+ dataItems
		+ "</tr>";
	return newRow;
}
