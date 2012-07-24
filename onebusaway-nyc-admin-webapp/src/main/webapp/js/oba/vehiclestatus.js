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


var VehicleStatus = Ember.Application.create({
	ready: function() {
		$("#menu").tabs();
	}
		
});

/******************* Views ************************************/
VehicleStatus.VehicleView = Ember.View.extend({
	tagName: "table",
	didInsertElement: function() {
		var controller = this.get('controller');
		controller.loadGridData();
	},
	controllerBinding: "VehicleStatus.VehiclesController"
});

VehicleStatus.FilterView = Ember.View.extend({
	tagName: "ul",
	didInsertElement: function() {
		var controller = this.get('controller');
		controller.loadFiltersData();
	},
	controllerBinding: "VehicleStatus.FiltersController"
});

VehicleStatus.TopBarView = Ember.View.extend({
	refreshDialog: null,
	didInsertElement: function() {
		refreshDialog = $("<div id='refreshDialog'>" +
				"<input type='text' id='refreshRate'/>" +
		"seconds<input type='button' id='set' value='Set'/></div>").dialog({
		autoOpen: false,
		title: "Edit Refresh-rate",
		height: 100
		});
	},
	autoRefreshClick: function(event) {
		var controller = this.get('controller');
		controller.autoRefreshGrid(event.target.checked);
	},
	refreshClick: function() {
		var controller = this.get('controller');
		controller.refreshGrid();
	},
	refreshLabelClick: function() {
		refreshDialog.dialog('open');
		var set = refreshDialog.find('#set');
		set.bind({'click' : function() {
			 $("#autoRefreshBox #autoRefresh").text($("#refreshRate").val() + " sec");
			 refreshDialog.dialog('close');
		}});
	},
	controllerBinding: "VehicleStatus.TopBarController"
});

VehicleStatus.ParametersView = Ember.View.extend({
	

});

/******************* Controllers ************************************/
VehicleStatus.ParametersController = Ember.ArrayController.create({
	content: [],
});

VehicleStatus.VehiclesController = Ember.ArrayController.create({
	content: [],
	loadGridData : function() {
		var grid = $("#vehicleGrid");
		grid.jqGrid({
			url: "vehicle-status!getVehicleData.action?ts=" + new Date().getTime(),
			datatype: "json",
			mType: "GET",
			colNames: ["Status","Vehicle Id", "Last Update", "Inferred State", "Inferred DSC, Route + Direction", "Observed DSC", "Pull-out", "Pull-in", "Details"],
			colModel:[ {name:'status',index:'status', width:70, sortable:false,
						formatter: function(cellValue, options) {
							var cellImg = "<img src='../../css/img/" +cellValue +"' alt='Not Found' />";
							return cellImg;
						}}, 
			           {name:'vehicleId',index:'vehicleId', width:70}, 
			           {name:'lastUpdateTime',index:'lastUpdateTime', width:70}, 
			           {name:'inferredState',index:'inferredState', width:100, sortable:false}, 
			           {name:'inferredDestination',index:'inferredDestination', width:170, sortable:false}, 
			           {name:'observedDSC',index:'observedDSC', width:80}, 
			           {name:'pulloutTime',index:'pulloutTime', width:70},
			           {name:'pullinTime',index:'pullinTime', width:70},
			           {name:'details',index:'details', width:65, 
			        	formatter: function(cellValue, options) {
			        	   var linkHtml = "<a href='#' style='color:blue'>" + cellValue + "</a>";
			        	   return linkHtml;
			           }, sortable:false}
			         ],
			height: "390",
			width: "670",
			//width: "auto",
			viewrecords: true,
			loadonce:false,
			jsonReader: {
				root: "rows",
			    page: "page",
			    total: "total",
			    records: "records",
				repeatitems: false
			},
			pager: "#pager",
			loadComplete: function() {
				var lastUpdateTime = new Date();
				var time = lastUpdateTime.getHours() + ":" + lastUpdateTime.getMinutes() + " , " +lastUpdateTime.toDateString();
				$("#lastUpdateBox #lastUpdate").text(time);
			}
		}).navGrid("#pager", {edit:false,add:false,del:false});
	}
});

VehicleStatus.FiltersController = Ember.ArrayController.create({
	content: [],
	loadFiltersData : function() {
		$.ajax({
			type: "GET",
			url: "../../filters/vehicle-filters.xml",
			dataType: "xml",
			success: function(xml) {
				//Add depot options
				$(xml).find("Depot").each(function(){
					$("#depot").append("<option value=\"" +$(this).text() + "\"" + ">" +$(this).text() + "</option>");
				});
				//Add inferred state options
				$(xml).find("InferredState").each(function(){
					$("#inferredState").append("<option value=\"" +$(this).text() + "\"" + ">" +$(this).text() + "</option>");
				});
				//Add pullout options
				$(xml).find("PulloutStatus").each(function(){
					$("#pulloutStatus").append("<option value=\"" +$(this).text() + "\"" + ">" +$(this).text() + "</option>");
				});
			},
			error: function(request) {
				alert("Error: " + request.statusText);
			}
		});
	}	
});

VehicleStatus.TopBarController = Ember.ArrayController.create({
	content: [],
	interval: 0,
	autoRefreshGrid: function(checked) {
		if(checked) {
			var refreshInterval = $("#autoRefreshBox #autoRefresh").text().split(/ +/)[0];
			interval = setInterval(function(){$("#vehicleGrid").trigger("reloadGrid");},refreshInterval * 1000);
		} else {
			window.clearInterval(interval);
		}
	},
	refreshGrid: function() {
		$("#vehicleGrid").trigger("reloadGrid");
	}
});

/******************* Model ************************************/