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

VehicleStatus.VehicleView = Ember.View.extend({
	tagName: "table",
	didInsertElement: function() {
		$("#vehicleGrid").jqGrid({
			dataType: "local",
			colNames: ["Status","Vehicle Id", "Last Update", "Inferred State", "Inferred DSC, Route + Direction", "Observed DSC", "Pull-out", "Pull-in", "Details"],
			colModel:[ {name:"status",index:"status", width:65, sortable:false}, 
			           {name:"vehicleId",index:"vehicleId", width:60}, 
			           {name:"lastUpdate",index:"lastUpdate", width:60, sorttype:"date"}, 
			           {name:"inferredState",index:"inferredState", width:65}, 
			           {name:"routeAndDirection",index:"routeAndDirection", width:120, sortable:false}, 
			           {name:"observedDsc",index:"observedDsc", width:65}, 
			           {name:"pullout",index:"pullout", width:65, sorttype:"date"},
			           {name:"pullin",index:"pullin", width:65, sorttype:"date"},
			           {name:"details",index:"details", width:50, sortable:false}
			         ],
			width: "100%",
			height: "394",
			pager: "#pager"
		}).navGrid("#pager", {edit:false,add:false,del:false});
	}

});

VehicleStatus.ParametersView = Ember.View.extend({
	

});

VehicleStatus.ParametersController = Ember.ArrayController.create({
	content: [],
});

VehicleStatus.VehiclesController = Ember.ArrayController.create({
	content: [],
});