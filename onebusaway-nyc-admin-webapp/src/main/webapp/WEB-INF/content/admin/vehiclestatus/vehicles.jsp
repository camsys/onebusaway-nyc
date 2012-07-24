<?xml version="1.0" encoding="UTF-8"?>
<!--

    Copyright (c) 2011 Metropolitan Transportation Authority

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy of
    the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
    License for the specific language governing permissions and limitations under
    the License.

-->

<%@ taglib prefix="s" uri="/struts-tags" %>
<jsp:directive.page contentType="text/html" />
	<div id="topBox">
		<script type=text/x-handlebars>
			{{#view VehicleStatus.TopBarView}}
				<input type="text" id="search" />
				<div id="lastUpdateBox">
					<label class="vehicleLabel">Last Update:</label>
					<label id="lastUpdate" class="vehicleLabel"></label>
				</div>
				<input type="button" id="refresh" value="Refresh" class="inlineFormButton"
					{{action "refreshClick" on="click" }}/>
				<div id="autoRefreshBox">
					<input type="checkbox" id="enableAutoRefresh" class="vehicleLabel"
						{{action "autoRefreshClick" on="click" }}>Auto Refresh:
					<label id="autoRefresh" class="vehicleLabel" {{action "refreshLabelClick" on="click"}}>30 sec</label>
				</div>
			{{/view}}
		</script>
	</div>
	<div id="mainBox">
		<div id="filterBox">
			<label>Filter by:</label>
			<script type=text/x-handlebars>
			{{#view VehicleStatus.FilterView}}
			<ul id="filters">
				<li>
					<label class="vehicleLabel">Vehicle ID:</label>
					<input type="text" id="vehicleId" />
				</li>
				<li>
					<label class="vehicleLabel">Route:</label>
					<input type="text" id="route" />
				</li>
				<li>
					<label class="vehicleLabel">Depot:</label>
					<select name="depot" id="depot">
						<option selected="selected" value="all">All</option>
					</select>
				</li>
				<li>
					<label class="vehicleLabel">DSC:</label>
					<input type="text" id="dsc" />
				</li>
				<li>
					<label class="vehicleLabel">Inferred State:</label>
					<select name="inferredState" id="inferredState">
						<option selected="selected" value="all">All</option>
					</select>
				</li>
				<li>
					<label class="vehicleLabel">Pullout Status:</label>
					<select name="pulloutStatus" id="pulloutStatus">
						<option selected="selected" value="all">All</option>
					</select>
				</li>
			</ul>
			<div id="emergencyBox">
				<input type="checkbox" id="emergencyCheck" />
				<label class="vehicleLabel">Emergency Status</label>
			</div>
			<div id="filterButtons">
				<input type="button" id="reset" value="Reset" />
				<input type="button" id="apply" value="Apply" />
			</div>
			{{/view}}
			</script>
		</div>
		<div id="vehiclesBox">
			<script type=text/x-handlebars>
				{{#view VehicleStatus.VehicleView}}
					<table id="vehicleGrid" />
					<div id="pager" />	
				{{/view}}
			</script>
		</div>
	</div>
	<div id="bottomBox">
		<div id="scheduleBox" class="infoBox">
			<label class="vehicleLabel">Run/blocks scheduled to be active</label>
			<div id="scheduleInfo" class="boxData">
				<s:url var="url" value="/css/img/view-calendar-day.ico" />
				<img src="${url}" alt="Not found" />
				<label><s:property value=""/></label>
			</div>	
		</div>
		<div id="busBox" class="infoBox">
			<label class="vehicleLabel">Buses tracked in past 5 minutes</label>
			<div id="busInfo" class="boxData">
				<s:url var="url" value="/css/img/user-away-2.png" />
				<img src="${url}" alt="Not found" />
				<label><s:property value=""/></label>
			</div>	
		</div>
		<div id="inferrenceBox" class="infoBox">
			<label class="vehicleLabel">Buses inferred in revenue service</label>
			<div id="inferrenceInfo" class="boxData">
				<s:url var="url" value="/css/img/sign_dollar_icon.jpg" />
				<img src="${url}" alt="Not found" />
				<label><s:property value=""/></label>
			</div>	
		</div>
		<div id="emergencyBox" class="infoBox">
			<label class="vehicleLabel">Buses reporting emergency</label>
			<div id="emergencyInfo" class="boxData">
				<s:url var="url" value="/css/img/dialog-warning-4.png" />
				<img src="${url}" alt="Not found" />
				<label><s:property value=""/></label>
			</div>	
		</div>
	</div>
