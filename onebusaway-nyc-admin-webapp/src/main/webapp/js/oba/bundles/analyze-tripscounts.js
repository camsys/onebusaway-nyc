/*
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var analyzeDataset = "";
var analyzeBuildName = "";
var analyzeStartDate = "";
var analyzeEndDate = "";
var analyzeZones = [];
var analyzeData =  map();
var analyzeDataMaterialChartOptions = {
    chart: {
        title: 'Trip counts by Zones'
    },
    width: 900,
    height: 500,
    series: {
        // Gives each series an axis name that matches the Y-axis below.
        0: {axis: 'TripCounts'}
    },
    axes: {
        // Adds labels to each axis; they don't have to match the axis names.
        y: {
            TripCounts: {label: 'Trips'},
        }
    }
};

jQuery(function() {
    jQuery("#analyzeStartDate").datepicker(
        {
            dateFormat: "yy-mm-dd"

        });
    jQuery("#analyzeEndDate").datepicker(
        {
            dateFormat: "yy-mm-dd"

        });

    jQuery("#analyzeDatasetList").on("change", analyzeDatasetChange);
    jQuery("#analyzeBuildNameList").on("change", analyzeBuildNameChange);
    jQuery("#analyzeStartDate").on("change", analyzeStartDateChange);
    jQuery("#analyzeEndDate").on("change", analyzeEndDateChange);
});



function analyzeDatasetChange() {
    // Clear any previous reports
    $("#zone_selection").empty();
    clearChart();

    if ($("#analyzeDatasetList option:selected").val() == "0") {
        resetAnalyzeDataset();
    } else {
        analyzeDataset = $("#analyzeDatasetList option:selected").text();
        analyzeBuildName = "";
        var buildNameList = getExistingBuildList(analyzeDataset);
        initBuildNameList($("#analyzeBuildNameList"), buildNameList);
    }
}

function analyzeBuildNameChange() {
    // Clear any previous reports
    $("#zone_selection").empty();
    if ($("#analyzeNameList option:selected").val() == 0) {
        analyzeBuildName = "";
    } else {
        analyzeBuildName = $("#analyzeBuildNameList option:selected").text();
        if (analyzeDataset && analyzeBuildName) {
            updateZoneSelection();
        }
    }
    updateChart()
}
function analyzeStartDateChange(){
    analyzeStartDate = jQuery("#analyzeStartDate").val();
    updateChart();
}

function analyzeEndDateChange(){
    analyzeEndDate = jQuery("#analyzeEndDate").val();
    updateChart();
}

function resetAnalyzeDataset(){
    AnalyzeDataset = "";
    AnalyzeBuildName = "";
    $("#analyzeDatasetList").val("0");
    var row_0 = '<option value="0">Select a build name</option>';
    $("#analyzeBuildNameList").find('option').remove().end().append(row_0);
}

function addZone(zone){
    if(!analyzeData.containsKey(analyzeDataset + "," + analyzeBuildName + "," + zone)){
        analyzeData.add(analyzeDataset + "," + analyzeBuildName + "," + zone, getAnalyzeData(zone));
    }
}



function getAnalyzeData(zone){
    var data = {};
    data[csrfParameter] = csrfToken;
    data["datasetName"] = analyzeDataset;
    data["dataset_build_id"] = 0;
    data["buildName"] = analyzeBuildName;
    data["zone"] = zone;

    jQuery.ajax({
        url: "analyze-bundle!getZoneData.action",
        data: data,
        type: "POST",
        async: false,
        success: function (zoneData) {
            console.log(zoneData);
            $('#Compare #buildingReportDiv').hide();
            return zoneData;
        }
    })
}



function updateChart(){
    var requestedAnalyzeData = getRequestedAnalyzeData();

    var data = new google.visualization.DataTable();
    data.addColumn('date', 'Month');
    var requestKeys = [];
    for (requestKey in requestedAnalyzeData.keys){
        data.addColumn('number', requestKey);
        requestKeys.push(requestKey);
    }
    var formattedRequestedAnalyzeData = getFormattedRequestedAnalyzeData(requestedAnalyzeData, requestKeys);
    data.addRows(formattedRequestedAnalyzeData);

    var chartDiv = document.getElementById('chart_div');
    var materialChart = new google.charts.Line(chartDiv);
    materialChart.draw(data, analyzeDataMaterialChartOptions);
}

function updateZoneSelection(){
    var zoneData = getZonesForBuild();
    $("#zone_selection").child



    
}

function getZonesForBuild(){
    var data = {};
    data[csrfParameter] = csrfToken;
    data["datasetName"] = analyzeDataset;
    data["dataset_build_id"] = 0;
    data["buildName"] = analyzeBuildName;

    jQuery.ajax({
        url: "analyze-bundle!getZoneList.action",
        data: data,
        type: "POST",
        async: false,
        success: function (zoneData) {
            console.log(zoneData);
            $('#Compare #buildingReportDiv').hide();
            return zoneData;
        }
    })
}

function clearChart(){
    analyzeZones = [];
    analyzeData = map();

    var chartDiv = document.getElementById('chart_div');
    var data = new google.visualization.DataTable();
    var materialChart = new google.charts.Line(chartDiv);
    materialChart.draw(data, analyzeDataMaterialChartOptions);
}

function getFormattedRequestedAnalyzeData(requestedAnalyzeData, requestKeys){
    formattedRequestAnalyzeData = []
    //while loop making dates
        //formattedRequestAnalyzeDatum = [date]
        //for loop of requestKeys
            //formattedRequestAnalyzeDatum.push(requestedAnalyzeData.get(requestKey).get(date))
        //date ++

    return formattedRequestAnalyzeData;
}



function getRequestedAnalyzeData(){
    var requestedAnalyzeData = Map();
    var selectedZones = getSelectedZones();
    for (zone in selectedZones)
        requestedAnalyzeData.add(zone, analyzeData.get(analyzeDataset + "," + analyzeBuildName + "," + zone));
    return requestedAnalyzeData;
}

