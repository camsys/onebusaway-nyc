package org.onebusaway.api.impl;

import org.onebusaway.api.model.transit.ArrivalAndDepartureV2Bean;
import org.onebusaway.api.model.transit.EntryWithReferencesBean;

import net.sf.json.JSONObject;
import net.sf.json.test.JSONAssert;

import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public class CustomJsonLibHandlerTest extends CustomJsonLibHandler {

  @Test
  public void testFromObject() throws IOException {

    EntryWithReferencesBean<ArrivalAndDepartureV2Bean> response = new HandlerTestHelper().createTestObject();

    Writer writer = new StringWriter();
    fromObject(response, "arg1", writer, null);
    writer.close();

    JSONAssert.assertEquals(writer.toString(), JSONObject.fromObject(expected));
  }

  private static final String expected = "{\"entry\":{\"status\":null,\"serviceDate\":0,\"vehicleId\":null,\"stopSequence\":0,\"blockTripSequence\":0,\"arrivalEnabled\":false,\"scheduledArrivalTime\":0,\"scheduledArrivalInterval\":null,\"predictedArrivalTime\":0,\"predictedArrivalInterval\":null,\"departureEnabled\":false,\"scheduledDepartureTime\":0,\"scheduledDepartureInterval\":null,\"predictedDepartureTime\":0,\"predictedDepartureInterval\":null,\"frequency\":null,\"predicted\":false,\"lastUpdateTime\":null,\"distanceFromStop\":0.0,\"numberOfStopsAway\":0,\"routeShortName\":null,\"tripHeadsign\":null,\"tripStatus\":null,\"routeId\":\"myRouteId\",\"tripId\":null,\"stopId\":\"myStopId\",\"routeLongName\":null,\"situationIds\":[\"situationId\"]},\"references\":{\"routes\":[{\"id\":\"myRouteId\",\"type\":0,\"description\":null,\"shortName\":null,\"longName\":null,\"url\":null,\"color\":null,\"textColor\":null,\"agencyId\":null}],\"situations\":[{\"id\":\"situationId\",\"description\":{\"value\":\"description\",\"lang\":\"en\"},\"allAffects\":[{\"directionId\":\"affectsDirectionId\",\"agencyId\":\"affectsAgencyId\",\"routeId\":\"affectsRouteId\",\"tripId\":\"affectsTripdId\",\"stopId\":\"myStopId\",\"applicationId\":\"myApplication\"}],\"consequences\":[{\"condition\":\"detour\",\"conditionDetails\":{\"diversionPath\":{\"length\":0,\"points\":\"detourPath\",\"levels\":null},\"diversionStopIds\":[\"detourStopId\"]}}],\"url\":{\"value\":\"url\",\"lang\":\"en\"},\"creationTime\":300000,\"activeWindows\":[{\"from\":0,\"to\":100000}],\"publicationWindows\":[{\"from\":100000,\"to\":200000}],\"reason\":\"reason\",\"severity\":\"severe\",\"summary\":{\"value\":\"summary\",\"lang\":\"en\"}}],\"stops\":[{\"name\":null,\"id\":\"myStopId\",\"lat\":0.0,\"lon\":0.0,\"direction\":null,\"code\":null,\"locationType\":0,\"routeIds\":[\"myRouteId\"]}],\"trips\":[{\"id\":null,\"timeZone\":null,\"routeShortName\":null,\"tripHeadsign\":null,\"tripShortName\":null,\"serviceId\":null,\"shapeId\":null,\"directionId\":null,\"blockId\":null,\"routeId\":\"myRouteId\"}],\"agencies\":[{\"name\":null,\"id\":null,\"timezone\":null,\"url\":null,\"lang\":null,\"phone\":null,\"disclaimer\":null,\"privateService\":false}]}}";
}
