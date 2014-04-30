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

  private static final String expected = "{\"entry\":{\"arrivalEnabled\":false,\"blockTripSequence\":0,\"departureEnabled\":false,\"distanceFromStop\":0,\"frequency\":null,\"lastUpdateTime\":0,\"numberOfStopsAway\":0,\"predicted\":false,\"routeId\":\"myRouteId\",\"routeLongName\":\"\",\"routeShortName\":\"\",\"scheduledArrivalInterval\":null,\"scheduledArrivalTime\":0,\"scheduledDepartureInterval\":null,\"scheduledDepartureTime\":0,\"serviceDate\":0,\"situationIds\":[\"situationId\"],\"status\":\"\",\"stopId\":\"myStopId\",\"stopSequence\":0,\"tripHeadsign\":\"\",\"tripId\":\"\",\"vehicleId\":\"\"},\"references\":{\"agencies\":[{\"disclaimer\":\"\",\"id\":\"\",\"lang\":\"\",\"name\":\"\",\"phone\":\"\",\"privateService\":false,\"timezone\":\"\",\"url\":\"\"}],\"routes\":[{\"agencyId\":\"\",\"color\":\"\",\"description\":\"\",\"id\":\"myRouteId\",\"longName\":\"\",\"shortName\":\"\",\"textColor\":\"\",\"type\":0,\"url\":\"\"}],\"situations\":[{\"activeWindows\":[{\"from\":0,\"to\":100000}],\"allAffects\":[{\"agencyId\":\"affectsAgencyId\",\"applicationId\":\"myApplication\",\"directionId\":\"affectsDirectionId\",\"routeId\":\"affectsRouteId\",\"stopId\":\"myStopId\",\"tripId\":\"affectsTripdId\"}],\"consequences\":[{\"condition\":\"detour\",\"conditionDetails\":{\"diversionPath\":{\"length\":0,\"levels\":\"\",\"points\":\"detourPath\"},\"diversionStopIds\":[\"detourStopId\"]}}],\"creationTime\":300000,\"description\":{\"lang\":\"en\",\"value\":\"description\"},\"id\":\"situationId\",\"publicationWindows\":[{\"from\":100000,\"to\":200000}],\"reason\":\"reason\",\"severity\":\"severe\",\"summary\":{\"lang\":\"en\",\"value\":\"summary\"},\"url\":{\"lang\":\"en\",\"value\":\"url\"}}],\"stops\":[{\"code\":\"\",\"direction\":\"\",\"id\":\"myStopId\",\"lat\":0,\"locationType\":0,\"lon\":0,\"name\":\"\",\"routeIds\":[\"myRouteId\"],\"wheelchairBoarding\":null}],\"trips\":[{\"blockId\":\"\",\"directionId\":\"\",\"id\":\"\",\"routeId\":\"myRouteId\",\"routeShortName\":\"\",\"serviceId\":\"\",\"shapeId\":\"\",\"timeZone\":\"\",\"tripHeadsign\":\"\",\"tripShortName\":\"\"}]}}";

}