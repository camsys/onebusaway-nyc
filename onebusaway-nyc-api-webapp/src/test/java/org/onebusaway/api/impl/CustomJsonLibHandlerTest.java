package org.onebusaway.api.impl;

import static org.junit.Assert.assertEquals;

import org.onebusaway.api.model.transit.ArrivalAndDepartureV2Bean;
import org.onebusaway.api.model.transit.EntryWithReferencesBean;

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
    assertEquals(expected, writer.toString());

  }

  private static final String expected = "{\"references\":{\"stops\":[{\"id\":\"myStopId\",\"lon\":0,\"direction\":\"\",\"locationType\":0,\"name\":\"\",\"wheelchairBoarding\":null,\"routeIds\":[\"myRouteId\"],\"code\":\"\",\"lat\":0}],\"situations\":[{\"id\":\"situationId\",\"summary\":{\"value\":\"summary\",\"lang\":\"en\"},\"consequences\":[{\"condition\":\"detour\",\"conditionDetails\":{\"diversionStopIds\":[\"detourStopId\"],\"diversionPath\":{\"levels\":\"\",\"length\":0,\"points\":\"detourPath\"}}}],\"activeWindows\":[{\"to\":100000,\"from\":0}],\"reason\":\"reason\",\"description\":{\"value\":\"description\",\"lang\":\"en\"},\"allAffects\":[{\"directionId\":\"affectsDirectionId\",\"stopId\":\"myStopId\",\"tripId\":\"affectsTripdId\",\"applicationId\":\"myApplication\",\"routeId\":\"affectsRouteId\",\"agencyId\":\"affectsAgencyId\"}],\"creationTime\":300000,\"severity\":\"severe\",\"publicationWindows\":[{\"to\":200000,\"from\":100000}],\"url\":{\"value\":\"url\",\"lang\":\"en\"}}],\"trips\":[{\"id\":\"\",\"shapeId\":\"\",\"tripShortName\":\"\",\"directionId\":\"\",\"serviceId\":\"\",\"blockId\":\"\",\"routeShortName\":\"\",\"tripHeadsign\":\"\",\"routeId\":\"myRouteId\",\"timeZone\":\"\"}],\"routes\":[{\"id\":\"myRouteId\",\"textColor\":\"\",\"color\":\"\",\"description\":\"\",\"longName\":\"\",\"shortName\":\"\",\"type\":0,\"agencyId\":\"\",\"url\":\"\"}],\"agencies\":[{\"id\":\"\",\"privateService\":false,\"phone\":\"\",\"timezone\":\"\",\"disclaimer\":\"\",\"name\":\"\",\"lang\":\"\",\"url\":\"\"}]},\"entry\":{\"distanceFromStop\":0,\"scheduledArrivalTime\":0,\"vehicleId\":\"\",\"serviceDate\":0,\"numberOfStopsAway\":0,\"status\":\"\",\"tripId\":\"\",\"lastUpdateTime\":0,\"routeLongName\":\"\",\"tripHeadsign\":\"\",\"predicted\":false,\"routeId\":\"myRouteId\",\"frequency\":null,\"stopSequence\":0,\"departureEnabled\":false,\"scheduledArrivalInterval\":null,\"situationIds\":[\"situationId\"],\"stopId\":\"myStopId\",\"scheduledDepartureInterval\":null,\"routeShortName\":\"\",\"arrivalEnabled\":false,\"scheduledDepartureTime\":0,\"blockTripSequence\":0}}";

}