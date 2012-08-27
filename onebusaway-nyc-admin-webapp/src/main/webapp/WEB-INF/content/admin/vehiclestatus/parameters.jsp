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
	<div id="parametersBox">
		<div id="parameters">
			<ul id="accordion">
				<li>
					<h3><a href="#" class="heading">Module: TDM</a></h3>
					<div id="tdmSection">
						<div id="crewAssignments" class="propertyHolder">
							<label class="propertyHeader">Crew Assignment Refresh Interval</label>
							<div class="propertyDescription">
								<p>This is the number of minutes after which crew assignments
								are refreshed.</p>
							</div>
							<div>
								<input type="hidden" id="crewAssignmentKey" value="tdmCrewAssignmentRefreshKey" />
								<input type="text" id="crewAssignmentValue" class="propertyValue" />
								<label id="propertyUnit">minutes</label>
							</div>
						</div>
						<div id="vehicleAssignments" class="propertyHolder rightProperty">
							<label class="propertyHeader">Vehicle Assignment Refresh Interval</label>
							<div class="propertyDescription">
								<p>This is the number of minutes after which vehicle assignments
								are refreshed.</p>
							</div>
							<div>
								<input type="hidden" id="vehicleAssignmentKey" value="tdmVehicleAssignmentRefreshKey" />
								<input type="text" id="vehicleAssignmentValue" class="propertyValue" />
								<label>minutes</label>
							</div>
						</div>
						
					</div>
				</li>
				<li>
					<h3><a href="#" class="heading">Module: Admin</a></h3>
					<div id="adminSection">
						
						<div id="senderEmailAddress" class="propertyHolder">
							<label class="propertyHeader">Sender Email Address</label>
							<div class="propertyDescription">
								<p>This is the email address to which, bundle building service
								will send email once bundle build process is complete.</p>
							</div>
							<div>
								<input type="hidden" id="emailAddressKey" value="adminSenderEmailAddressKey"/>
								<input type="text" id="emailAddressValue" class="textPropertyValue"/>
							</div>
						</div>
						<div id="instanceId" class="propertyHolder rightProperty">
							<label class="propertyHeader">Admin Server instance id</label>
							<div class="propertyDescription">
								<p>This is amazon ec2 instance id for admin server.</p>
							</div>
							<div>
								<input type="hidden" id="instanceIdKey" value="adminInstanceIdKey"/>
								<input type="text" id="instanceIdValue" class="textPropertyValue"/>
							</div>
						</div>
						
					</div>
				</li>
				<li>
					<h3><a href="#" class="heading">Module: Vehicle Tracking</a></h3>
					<div id="vehicleTrackingSection">
						
						<div class="topSection">
							<div id="stalledTimeout" class="propertyHolder">
								<label class="propertyHeader">Stalled Timeout</label>
								<div class="propertyDescription">
									<p>This is the number of minutes after which a bus, if it does not 
									   move, is considered to not be making progress. "Stalled" 
									   buses that are not on a journey are considered to be at the 
									   depot, and hidden from users. 
									</p>
								</div>
								<div>
									<input type="hidden" id="stalledTimeoutKey" value="displayStalledTimeoutKey"/>
									<input type="text" id="stalledTimeoutValue" class="propertyValue"/>
									<label>seconds</label>
								</div>
							</div>
							<div id="staleMessages" class="propertyHolder rightProperty">
								<label class="propertyHeader">No Messages: Stale</label>
								<div class="propertyDescription">
									<p>Bus is not sending messages. After this period, the data is
									   considered "stale".
									</p>
								</div>
								<div>
									<input type="hidden" id="staleMessagesKey" value="displayStaleTimeoutKey"/>
									<input type="text" id="staleMessagesValue" class="propertyValue"/>
									<label>seconds</label>
								</div>
							</div>
						</div>
						<div class="followingSection">
							<div id="offRouteMessages" class="propertyHolder">
								<label class="propertyHeader">"Off Route" Messages</label>
								<div class="propertyDescription">
									<p>This is the distance away from a matched route that a bus is 
										allowed to deviate before it is considered "off route". Such 
										buses are hidden in the UI, reported as such in the API. 
									</p>
								</div>
								<div>
									<input type="hidden" id="offRouteMessageKey" value="displayOffrouteDistanceKey"/>
									<input type="text" id="offRouteMessageValue" class="propertyValue"/>
									<label>meters</label>
								</div>
							</div>
							<div id="hideMessages" class="propertyHolder rightProperty">
								<label class="propertyHeader">No Messages: Hide</label>
								<div class="propertyDescription">
									<p>Bus is not sending messages. After this period, the bus is
										hidden from users.
									</p>
								</div>
								<div>
									<input type="hidden" id="hideMessagesKey" value="displayHideTimeoutKey"/>
									<input type="text" id="hideMessagesValue" class="propertyValue"/>
									<label>seconds</label>
								</div>
							</div>
						</div>
						<div class="followingSection">
							<div id="bingMapsKey" class="propertyHolder">
								<label class="propertyHeader">Bing maps key</label>
								<div class="propertyDescription">
									<p>Bing Maps license key.</p>
								</div>
								<div>
									<input type="hidden" id="bingMapsKey" value="displayBingMapsKey"/>
									<input type="text" id="bingMapsKeyValue" />
								</div>
							</div>
							<div id="previousTripFilterDistance" class="propertyHolder rightProperty">
								<label class="propertyHeader">Previous Trip Filter Distance</label>
								<div class="propertyDescription">
									<p>The distance beyond which a bus will be filtered out of SIRI SM calls, 
									when on the previous trip, that the monitored call is on.
									</p>
								</div>
								<div>
									<input type="hidden" id="previousFilterKey" value="displayPreviousFilterKey"/>
									<input type="text" id="previousFilterValue" class="propertyValue"/>
									<label>miles</label>
								</div>
							</div>
						</div>
						<div class="followingSection">
							<div id="googleAnalyticsSiteId" class="propertyHolder">
								<label class="propertyHeader">Google Analytics site id</label>
								<div class="propertyDescription">
									<p>The google analytics site ID used for tracking visits to all interfaces.
									</p>
								</div>
								<div>
									<input type="hidden" id="googleAnalyticsIdKey" value="displayGoogleAnalyticsIdKey"/>
									<input type="text" id="googleAnalyticsIdValue" class="textPropertyValue"/>
								</div>
							</div>
							<div id="googleMapsClientId" class="propertyHolder rightProperty">
								<label class="propertyHeader">Google Maps Client Id</label>
								<div class="propertyDescription">
									<p>The google maps site ID used when using the google maps enterprise geocoder
									 and styled maps.
									</p>
								</div>
								<div>
									<input type="hidden" id="googleMapsIdKey" value="displayGoogleMapsIdKey"/>
									<input type="text" id="googleMapsIdValue" class="textPropertyValue"/>
								</div>
							</div>
						</div>
						<div class="followingSection">
							<div id="googleMapsSecretKey" class="propertyHolder">
								<label class="propertyHeader">Google Maps Secret Key</label>
								<div class="propertyDescription">
									<p>The google maps secret key used when using the google maps enterprise geocoder 
									and styled maps.
									</p>
								</div>
								<div>
									<input type="hidden" id="googleMapsSecretKey" value="displayGoogleMapsSecretKey"/>
									<input type="text" id="googleMapsSecretKeyValue" class="textPropertyValue"/>
								</div>
							</div>
							<div id="useTimePredictions" class="propertyHolder rightProperty">
								<label class="propertyHeader">Use Time Predictions</label>
								<div class="propertyDescription">
									<p>Indicator if the application should use time predictions. Should
									be <span>true</span> or <span>false</span>.
									</p>
								</div>
								<div>
									<input type="hidden" id="timePredictionsKey" value="displayUseTimePredictionsKey"/>
									<input type="text" id="timePredictionsValue" />
								</div>
							</div>
						</div>
						
					</div>
				</li>
				<li>
					<h3><a href="#" class="heading">Module: Inferrence Engine</a></h3>
					<div id="ieSection">
						
						<div class="topSection">
							<div id="inputQueueName" class="propertyHolder">
								<label class="propertyHeader">Input Queue name</label>
								<div class="propertyDescription">
									<p>This is the name of the queue, from which inferrence engine reads
									 inbound messages. 
									</p>
								</div>
								<div>
									<input type="hidden" id="inputQueueNameKey" value="ieInputQueueNameKey"/>
									<input type="text" id="inputQueueNameValue" class="textPropertyValue"/>
								</div>
							</div>
							<div id="outputQueueName" class="propertyHolder rightProperty">
								<label class="propertyHeader">Output Queue name</label>
								<div class="propertyDescription">
									<p>This is the name of the queue, on which inferrence engine puts
									processed messages. 
									</p>
								</div>
								<div>
									<input type="hidden" id="outputQueueNameKey" value="ieOutputQueueNameKey"/>
									<input type="text" id="outputQueueNameValue" class="textPropertyValue"/>
								</div>
							</div>
						</div>
						<div class="followingSection">
							<div id="inputQueuePort" class="propertyHolder">
								<label class="propertyHeader">Input Queue Port</label>
								<div class="propertyDescription">
									<p>This is the port number on which, inferrence engine connects to
									input queue for processing inbound messages. 
									</p>
								</div>
								<div>
									<input type="hidden" id="inputQueuePortKey" value="ieInputQueuePortKey"/>
									<input type="text" id="inputQueuePortValue" class="propertyValue"/>
								</div>
							</div>
							<div id="outputQueuePort" class="propertyHolder rightProperty">
								<label class="propertyHeader">Output Queue Port</label>
								<div class="propertyDescription">
									<p>This is the port number on which inferrence engine connects with output 
									queue for putting processed messages.
									</p>
								</div>
								<div>
									<input type="hidden" id="outputQueuePortKey" value="ieOutputQueuePortKey"/>
									<input type="text" id="outputQueuePortValue" class="propertyValue"/>
								</div>
							</div>
						</div>
						<div class="followingSection">
							<div id="inputQueueHost" class="propertyHolder">
								<label class="propertyHeader">Input Queue Host</label>
								<div class="propertyDescription">
									<p>This is the machine, that hosts input queue for inferrence engine. 
									</p>
								</div>
								<div>
									<input type="hidden" id="inputQueueHostKey" value="ieInputQueueHostKey"/>
									<input type="text" id="inputQueueHostValue" class="textPropertyValue"/>
								</div>
							</div>
							<div id="outputQueueHost" class="propertyHolder rightProperty">
								<label class="propertyHeader">Output Queue Host</label>
								<div class="propertyDescription">
									<p>This is the machine, that hosts output queue for inferrence engine.
									</p>
								</div>
								<div>
									<input type="hidden" id="outputQueueHostKey" value="ieOutputQueueHostKey"/>
									<input type="text" id="outputQueueHostValue" class="textPropertyValue"/>
								</div>
							</div>
						</div>
						
					</div>
				</li>
				<li>
					<h3><a href="#" class="heading">Module: TDS</a></h3>
					<div id="tdsSection">
						
						<div class="topSection">
							<div id="tdsInputQueueName" class="propertyHolder">
								<label class="propertyHeader">Input Queue name</label>
								<div class="propertyDescription">
									<p>This is the name of the queue, from which transit data service reads
									 inbound messages. 
									</p>
								</div>
								<div>
									<input type="hidden" id="tdsInputQueueNameKey" value="tdsInputQueueNameKey"/>
									<input type="text" id="tdsInputQueueNameValue" class="textPropertyValue"/>
								</div>
							</div>
							<div id="tdsInputQueuePort" class="propertyHolder rightProperty">
								<label class="propertyHeader">Input Queue Port</label>
								<div class="propertyDescription">
									<p>This is the port number on which, transit data service connects to
									input queue for processing messages. 
									</p>
								</div>
								<div>
									<input type="hidden" id="tdsInputQueuePortKey" value="tdsInputQueuePortKey"/>
									<input type="text" id="tdsInputQueuePortValue" class="propertyValue"/>
								</div>
							</div>
						</div>
						<div class="followingSection">
							<div id="tdsInputQueueHost" class="propertyHolder">
								<label class="propertyHeader">Input Queue Host</label>
								<div class="propertyDescription">
									<p>This is the machine, that hosts input queue for transit data service. 
									</p>
								</div>
								<div>
									<input type="hidden" id="tdsInputQueueHostKey" value="tdsInputQueueHostKey"/>
									<input type="text" id="tdsInputQueueHostValue" class="textPropertyValue"/>
								</div>
							</div>
						</div>
						
						
					</div>
				</li>
				<li>
					<h3><a href="#" class="heading">Module: Operational API</a></h3>
					<div id="opsAPISection">
						<div id="apiHost" class="propertyHolder">
							<label class="propertyHeader">Operational API Host</label>
							<div class="propertyDescription">
								<p>Server name that hosts operational API.</p>
							</div>
							<div>
								<input type="hidden" id="apiHostKey" value="opsApiHostKey"/>
								<input type="text" id="apiHostValue" class="textPropertyValue"/>
							</div>
						</div>
					</div>
				</li>
			</ul>
		</div>
		<div id="results">
			<div id="messageBox">
				<s:url var="url" value="/css/img/dialog-accept-2.png"></s:url>
				<img id="resultImg" alt="Not Found" src="${url}" />
				<label id="message"><s:property value=""/></label>
			</div>
			<div id="submitBox">
				<input type="button" id="reset" class="inlineButton" value="Reset Previous" disabled="disabled"/>
				<input type="button" id="save" class="inlineButton" value="Save" disabled="disabled"/>
			</div>
		</div>
	</div>
</html>