// Copyright 2010, OpenPlans
// Licensed under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

var OBA = window.OBA || {};

OBA.Config = {
    debug: true,

    // urls to fetch various data
    searchUrl: "search.action",
    routeShapeUrl: "/onebusaway-api-webapp/api/where/stops-for-route",
    stopsUrl: "/onebusaway-api-webapp/api/where/stops-for-location.json",
    stopUrl: "/onebusaway-api-webapp/api/where/arrivals-and-departures-for-stop",
    vehiclesUrl:"/onebusaway-api-webapp/api/where/trips-for-route",
    vehicleUrl: "/onebusaway-api-webapp/api/where/trip-for-vehicle",

    // milliseconds to wait in between polls for bus locations
    pollingInterval: 5000,

    // image url
    vehicleIcon: "img/vehicle.png",
    stopIcon: "img/stop.png",

    // api key used for webapp
    apiKey: "TEST",
};
