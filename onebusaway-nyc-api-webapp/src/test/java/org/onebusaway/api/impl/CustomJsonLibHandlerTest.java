/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
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

package org.onebusaway.api.impl;

import static org.junit.Assert.assertEquals;

import org.onebusaway.api.model.transit.ArrivalAndDepartureV2Bean;
import org.onebusaway.api.model.transit.EntryWithReferencesBean;

import org.junit.Test;
import org.onebusaway.api.serializers.json.CustomJsonLibHandler;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public class CustomJsonLibHandlerTest {

  @Test
  public void testFromObject() throws IOException {

    /*ActionProxy proxy = getActionProxy("/test/testAction.action");
    assertNotNull(proxy);*/

    EntryWithReferencesBean<ArrivalAndDepartureV2Bean> response = new HandlerTestHelper().createTestObject();
    CustomJsonLibHandler jackson = new CustomJsonLibHandler();

    Writer writer = new StringWriter();
    jackson.fromObject(null,response, "arg1", writer);
    writer.close();
    assertEquals(expected, writer.toString());

  }

  private static final String expected = "{\"entry\":{\"arrivalEnabled\":false,\"blockTripSequence\":0,\"departureEnabled\":false,\"distanceFromStop\":0.0,\"frequency\":null,\"lastUpdateTime\":0,\"numberOfStopsAway\":0,\"predicted\":false,\"routeId\":\"myRouteId\",\"routeLongName\":\"\",\"routeShortName\":\"\",\"scheduledArrivalInterval\":null,\"scheduledArrivalTime\":0,\"scheduledDepartureInterval\":null,\"scheduledDepartureTime\":0,\"serviceDate\":0,\"situationIds\":[\"situationId\"],\"status\":\"\",\"stopId\":\"myStopId\",\"stopSequence\":0,\"tripHeadsign\":\"\",\"tripId\":\"\",\"vehicleId\":\"\"},\"references\":{\"agencies\":[{\"disclaimer\":\"\",\"id\":\"\",\"lang\":\"\",\"name\":\"\",\"phone\":\"\",\"privateService\":false,\"timezone\":\"\",\"url\":\"\"}],\"routes\":[{\"agencyId\":\"\",\"color\":\"\",\"description\":\"\",\"id\":\"myRouteId\",\"longName\":\"\",\"shortName\":\"\",\"textColor\":\"\",\"type\":0,\"url\":\"\"}],\"situations\":[{\"activeWindows\":[{\"from\":0,\"to\":100000}],\"allAffects\":[{\"agencyId\":\"affectsAgencyId\",\"applicationId\":\"myApplication\",\"directionId\":\"affectsDirectionId\",\"routeId\":\"affectsRouteId\",\"stopId\":\"myStopId\",\"tripId\":\"affectsTripdId\"}],\"consequences\":[{\"condition\":\"detour\",\"conditionDetails\":{\"diversionPath\":{\"length\":0,\"levels\":\"\",\"points\":\"detourPath\"},\"diversionStopIds\":[\"detourStopId\"]}}],\"creationTime\":300000,\"description\":{\"lang\":\"en\",\"value\":\"description\"},\"id\":\"situationId\",\"publicationWindows\":[{\"from\":100000,\"to\":200000}],\"reason\":\"reason\",\"severity\":\"severe\",\"summary\":{\"lang\":\"en\",\"value\":\"summary\"},\"url\":{\"lang\":\"en\",\"value\":\"url\"}}],\"stops\":[{\"code\":\"\",\"direction\":\"\",\"id\":\"myStopId\",\"lat\":0.0,\"locationType\":0,\"lon\":0.0,\"name\":\"\",\"routeIds\":[\"myRouteId\"],\"wheelchairBoarding\":null}],\"trips\":[{\"blockId\":\"\",\"directionId\":\"\",\"id\":\"\",\"routeId\":\"myRouteId\",\"routeShortName\":\"\",\"serviceId\":\"\",\"shapeId\":\"\",\"timeZone\":\"\",\"tripHeadsign\":\"\",\"tripShortName\":\"\"}]}}";

}