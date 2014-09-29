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

var timeout = null;

jQuery(function() {
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
		jQuery("#buildBox #bundleStartDateHolder #startDatePicker").val(qs["startDate"]);
		jQuery("#buildBox #bundleEndDateHolder #endDatePicker").val(qs["endDate"]);
		jQuery("#comments").val(qs["bundleComment"]);
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
			var names = $.map($('#listItem.ui-selected strong, this'), function(element, i) {  
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
	
	jQuery("#compareCurrentDirectories").selectable({
		stop: function() {
			var names = $.map($('#compareListItem.ui-selected strong, this'), function(element, i) {  
			  return $(element).text();  
			}); 
			if (names.length > 0) {
				jQuery.ajax({
					url: "manage-bundles!existingBuildList.action",
					data: {
						"diffBundleName" : names[0]
					},
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
				jQuery.ajax({
					url: "manage-bundles!diffResult.action",
					data: {
						"diffBundleName" : bundleNames[0],
						"diffBuildName" : buildNames[0],
						"bundleDirectory" : jQuery("#createDirectory #directoryName").val(),
						"bundleName": jQuery("#buildBundle_bundleName").val()
					},
					type: "GET",
					async: false,
					success: function(data) {
						$('#diffResult').text('');
						$.each(data, function(index, value) {
							$('#diffResult').append(
								"<div id=\"diffResultItem\">"+value+"</div>");
						});
					}
				})
			}
		}
	});

	jQuery("#create_continue").click(onCreateContinueClick);
	
	jQuery("#prevalidate_continue").click(onPrevalidateContinueClick);
	
	jQuery("#upload_continue").click(onUploadContinueClick);
	
	jQuery("#build_continue").click(onBuildContinueClick);
	
	jQuery("#stage_continue").click(onStageContinueClick);
	
	jQuery("#deploy_continue").click(onDeployContinueClick);
	
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
		}
	});
	disableStageButton();
	disableBuildButton();

	//toggle bundle staging progress list
	jQuery("#stageBundle #stageBundle_progress #expand").bind({
			'click' : toggleStageBundleResultList});

	
	//Handle stage button click event
	jQuery("#stageBundle_stageButton").click(onStageClick);

	
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

function onBuildContinueClick() {
	var $tabs = jQuery("#tabs");
	$tabs.tabs('select', 4);
}

function onStageContinueClick() {
	var $tabs = jQuery("#tabs");
	$tabs.tabs('select', 5);
}

function onDeployContinueClick() {
	var $tabs = jQuery("#tabs");
	$tabs.tabs('select', 6);
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
				disableSelectButton();
				var status = response;
				if (status != undefined) {
					jQuery("#createDirectory #createDirectoryContents #createDirectoryResult").show().css("display","block");
					if(status.selected == true) {
						jQuery("#createDirectoryResult #resultImage").attr("src", "../../css/img/dialog-accept-2.png");
						jQuery("#createDirectoryMessage").text(status.message).css("color", "green");
						enableBuildButton();
					} else {
						jQuery("#createDirectoryResult #resultImage").attr("src", "../../css/img/warning_16.png");
						jQuery("#createDirectoryMessage").text(status.message).css("color", "red");
						disableBuildButton();
					}
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
					disableBuildButton();
				}
			},
			error: function(request) {
				alert("There was an error processing your request. Please try again.");
			}
		});
}


function enableContinueButton(continueButton) {
	jQuery(continueButton).removeAttr("disabled").css("color", "#000");
}

function disableContinueButton(continueButton) {
	jQuery(continueButton).attr("disabled", "disabled").css("color", "#999");
}

function enableSelectButton() {
	jQuery("#createDirectory #createDirectoryContents #directoryButton").removeAttr("disabled").css("color", "#000");
}

function disableSelectButton() {
	jQuery("#createDirectory #createDirectoryContents #directoryButton").attr("disabled", "disabled").css("color", "#999");
}

function enableStageButton() {
	jQuery("#stageBundle_stageButton").removeAttr("disabled").css("color", "#000");
	enableContinueButton($("#stage_continue"));
}

function disableStageButton() {
	jQuery("#stageBundle_stageButton").attr("disabled", "disabled").css("color", "#999");
	disableContinueButton($("#stage_continue"));
}

function enableDeployButton() {
	jQuery("#deployBundle_deployButton").removeAttr("disabled").css("color", "#000");
	enableContinueButton($("#deploy_continue"));
}

function disableDeployButton() {
	jQuery("#deployBundle_deployButton").attr("disabled", "disabled").css("color", "#999");
	disableContinueButton($("#deploy_continue"));
}

function enableBuildButton() {
	jQuery("#buildBundle_buildButton").removeAttr("disabled").css("color", "#000");
	enableContinueButton($("#create_continue"));
}

function disableBuildButton() {
	jQuery("#buildBundle_buildButton").attr("disabled", "disabled").css("color", "#999");
	disableContinueButton($("#create_continue"));
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

function toggleStageBundleResultList() {
	var $image = jQuery("#stageBundle #stageBundle_progress #expand");
	changeImageSrc($image);
	//Toggle progress result list
	jQuery("#stageBundle #stageBundle_resultList").toggle();
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
	jQuery("#createDirectory #createDirectoryContents #directoryButton").attr("disabled", "disabled").css("color", "#999");
		
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
	} 
	else if(~bundleName.indexOf(" ")){
		alert("bundle build name cannot contain spaces");
		return;
	}
	
	else {
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
	var bundleComment = jQuery("#bundleComment").val();
	
	var valid = validateBundleBuildFields(bundleDir, bundleName, startDate, endDate);
	if(valid == false) {
		return;
	}
	jQuery("#buildBundle #buildBox #building #buildingProgress").attr("src","../../css/img/ajax-loader.gif");
	jQuery("#buildBundle_buildProgress").text("Bundle Build in Progress...");
	jQuery("#buildBundle_fileList").html("");
	jQuery("#buildBundle #downloadLogs").hide();
	
	disableBuildButton();
	buildBundle(bundleName, startDate, endDate, bundleComment);
}

function validateBundleBuildFields(bundleDir, bundleName, startDate, endDate) {
	var valid = true;
	var errors = "";
	if (bundleDir == undefined || bundleDir == null || bundleDir == "") {
		errors += "missing bundle directory\n";
		valid = false;
	} 
	if (bundleName == undefined || bundleName == null || bundleName == "") {
		errors += "missing bundle build name\n";
		valid = false;
	} 
	else if(~bundleName.indexOf(" ")){
		errors += "bundle build name cannot contain spaces\n";
		valid = false;
	}
	if (startDate == undefined || startDate == null || startDate == "") {
		errors += "missing bundle start date\n";
		valid = false;
	}
	if (endDate == undefined || endDate == null || endDate == "") {
		errors += "missing bundle end date\n";
		valid = false;
	}
	if(errors.length > 0) {
		alert(errors);
		enableBuildButton();
	}
	return valid;
}

function bundleUrl() {
	var id = jQuery("#buildBundle_id").text();
	jQuery("#buildBundle_exception").hide();
	jQuery("#buildBundle #buildBox #building").show().css("width","300px").css("margin-top", "20px");
	jQuery.ajax({
		url: "../../api/build/" + id + "/url?ts=" +new Date().getTime(),
		type: "GET",
		async: false,
		success: function(response) {
				var bundleResponse = response;
				if(bundleResponse.exception !=null) {
					jQuery("#buildBundle #buildBox #buildBundle_resultLink #resultLink")
							.css("padding-left", "5px")
							.css("font-size", "12px")
							.addClass("adminLabel")
							.css("color", "red");
				} else {
					jQuery("#buildBundle #buildBox #buildBundle_resultLink #resultLink")
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
function buildBundle(bundleName, startDate, endDate, bundleComment){
	var bundleDirectory = jQuery("#buildBundle_bundleDirectory").text();
	var email = jQuery("#buildBundle_email").val();
	if (email == "") { email = "null"; }
	jQuery.ajax({
		url: "../../api/build/create?ts=" +new Date().getTime(),
		type: "POST",
		async: false,
		data: {
			bundleDirectory: bundleDirectory,
			bundleName: bundleName,
			email: email,
			bundleStartDate: startDate,
			bundleEndDate: endDate,
			bundleComment: bundleComment
		},
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
			alert("There was an error processing your request. Please try again.");
		}
	});
}

function updateBuildStatus() {
	disableStageButton();
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
					jQuery("#buildBundle #buildBox #building #buildingProgress").attr("src","../../css/img/dialog-warning-4.png");
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
					enableStageButton();
					enableBuildButton();
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
					disableStageButton();
					enableBuildButton();
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

function onStageClick() {
	stageBundle();
}

function stageBundle() {
	var environment = jQuery("#deploy_environment").text();
	var bundleDir = jQuery("#createDirectory #directoryName").val();
	var bundleName = jQuery("#buildBundle_bundleName").val();
	jQuery.ajax({
		url: "../../api/bundle/stagerequest/" + environment + "/" + bundleDir + "/" + bundleName + "?ts=" +new Date().getTime(), 
		type: "GET",
		async: false,
		success: function(response) {
				/*var bundleResponse = response;
				if (bundleResponse != undefined) {
					if (typeof response=="string") {
						if (bundleResponse.match(/SUCCESS/)) {
							toggleStageBundleResultList();
							jQuery("#bundleStagingResultsHolder #bundleStagingResults #stageBundle_progress").show().css("display","block");
							jQuery("#bundleStagingResultsHolder #bundleStagingResults #stageBundle_resultList").show().css("display","block");
							jQuery("#stageBundle_resultList").html(bundleName);
							jQuery("#stageContentsHolder #stageBox #staging #stagingProgress").attr("src","../../css/img/dialog-accept-2.png");
							jQuery("#stageBundle_stageProgress").text("Staging Complete!");
							var continueButton = jQuery("#stage_continue");
							enableContinueButton(continueButton);
						} else {
							jQuery("#stageBundle_id").text("Failed to Stage requested Bundle!");
							jQuery("#stageBundle_resultList").html("error");
						}
					}*/
				var bundleResponse = response;
				if (bundleResponse != undefined) {
					// the header is set wrong for the proxied object, run eval to correct
					if (typeof response=="string") {
						bundleResponse = eval('(' + response + ')');
					}
					jQuery("#stageBundle_resultList").html("calling...");
					jQuery("#stageBundle_id").text(bundleResponse.id);
					jQuery("#stageBundle #requestLabels").show().css("display","block");
					jQuery("#stageContentsHolder #stageBox #staging").show().css("display","block");
					jQuery("#stageBundle_stageProgress").text("Staging ...");
					jQuery("#stageContentsHolder #stageBox #staging #stagingProgress").attr("src","../../css/img/ajax-loader.gif");
					window.setTimeout(updateStageStatus, 5000);
				} else {
					jQuery("#stageBundle_id").text(error);
					jQuery("#stageBundle_resultList").html("error");
				}
		},
		error: function(request) {
			alert("There was an error processing your request. Please try again.");
		}
	});
	
}

function updateStageStatus() {
	id = jQuery("#stageBundle_id").text();
	jQuery.ajax({
		url: "../../api/bundle/stage/status/" + id + "/list?ts=" +new Date().getTime(),
		type: "GET",
		async: false,
		success: function(response) {
				var txt = "<ul>";
				var bundleResponse = response;
				if (bundleResponse == null) {
					jQuery("#stageBundle_stageProgress").text("Stage Complete!");
					jQuery("#stageContentsHolder #stageBox #staging #stagingProgress").attr("src","../../css/img/dialog-warning-4.png");
					jQuery("#stageBundle_resultList").html("unknown id=" + id);
					return;
				}
				// the header is set wrong for the proxied object, run eval to correct
				if (typeof response=="string") {
					bundleResponse = eval('(' + response + ')');
				}
				if (bundleResponse.status != "complete" && bundleResponse.status != "error") {
					window.setTimeout(updateStageStatus, 5000); // recurse
				} else {
					toggleStageBundleResultList();
					jQuery("#bundleStagingResultsHolder #bundleStagingResults #stageBundle_progress").show().css("display","block");
					jQuery("#bundleStagingResultsHolder #bundleStagingResults #stageBundle_resultList").show().css("display","block");
					if (bundleResponse.status == "complete") {
						jQuery("#stageContentsHolder #stageBox #staging #stagingProgress").attr("src","../../css/img/dialog-accept-2.png");
						jQuery("#stageBundle_stageProgress").text("Staging Complete!");
						// set resultList to bundleNames list
						var size = bundleResponse.bundleNames.length;
						if (size > 0) {
							for (var i=0; i<size; i++) {
								txt = txt + "<li>" + bundleResponse.bundleNames[i] + "</li>";
							}
						}
						var continueButton = jQuery("#stage_continue");
						enableContinueButton(continueButton);
					} else {
						jQuery("#stageContentsHolder #stageBox #staging #stagingProgress").attr("src","../../css/img/dialog-warning-4.png");
						jQuery("#stageBundle_stageProgress").text("Staging Failed!");
						// we've got an error
						txt = txt + "<li><font color=\"red\">ERROR!  Please consult the logs and check the "
							+ "filesystem permissions before continuing</font></li>";
					}
				}
				txt = txt + "</ul>";
				jQuery("#stageBundle_resultList").html(txt).css("font-size", "12px");	
		},
		error: function(request) {
			clearTimeout(timeout);
			
			jQuery("#stageContentsHolder #stagingBox #staging #stagingProgress").attr("src","../../css/img/dialog-warning-4.png");
			jQuery("#stageBundle_stageProgress").text("Staging Failed!");
			jQuery("#bundleStagingResultsHolder #bundleStagingResults #stageBundle_progress").show().css("display","block");
			jQuery("#bundleStagingResultsHolder #bundleStagingResults #stageBundle_resultList").show().css("display","block");
			toggleStageBundleResultList();

			// error out on a 500 error, the session will be lost so it will not recover
			var txt = "<ul>";
			txt = txt + "<li><font color=\"red\">The server returned an internal error.  Please consult the logs" 
				+ " or retry your request</font></li>";
			txt = txt + "</ul>";
			jQuery("#stageBundle_resultList").html(txt).css("font-size", "12px");
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
			alert("There was an error processing your request. Please try again.");
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
						var continueButton = jQuery("#deploy_continue");
						enableContinueButton(continueButton);
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
			alert("There was an error processing your request. Please try again.");
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