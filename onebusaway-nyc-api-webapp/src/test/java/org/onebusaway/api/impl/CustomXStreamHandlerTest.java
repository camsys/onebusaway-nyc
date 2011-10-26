package org.onebusaway.api.impl;

//import static org.junit.Assert.*;

import static org.junit.Assert.assertEquals;

import org.onebusaway.api.model.transit.ArrivalAndDepartureV2Bean;
import org.onebusaway.api.model.transit.EntryWithReferencesBean;

import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public class CustomXStreamHandlerTest extends CustomXStreamHandler {

  @Test
  public void testFromObject() throws IOException {

    EntryWithReferencesBean<ArrivalAndDepartureV2Bean> response = new HandlerTestHelper().createTestObject();

    Writer writer = new StringWriter();
    fromObject(response, "arg1", writer);
    writer.close();
    assertEquals(expected, writer.toString().replaceAll("\n", ""));

  }

  final private static String expected = "<entryWithReferences>" +
      "  <references>" +
      "    <agencies>" +
      "      <agency>" +
      "        <privateService>false</privateService>" +
      "      </agency>" +
      "    </agencies>" +
      "    <routes>" +
      "      <route>" +
      "        <id>myRouteId</id>" +
      "        <type>0</type>" +
      "      </route>" +
      "    </routes>" +
      "    <stops>" +
      "      <stop>" +
      "        <id>myStopId</id>" +
      "        <lat>0.0</lat>" +
      "        <lon>0.0</lon>" +
      "        <locationType>0</locationType>" +
      "        <routeIds>" +
      "          <string>myRouteId</string>" +
      "        </routeIds>" +
      "      </stop>" +
      "    </stops>" +
      "    <trips>" +
      "      <trip>" +
      "        <routeId>myRouteId</routeId>" +
      "      </trip>" +
      "    </trips>" +
      "    <situations>" +
      "      <situation>" +
      "        <id>situationId</id>" +
      "        <creationTime>300000</creationTime>" +
      "        <activeWindows>" +
      "          <timeRange>" +
      "            <from>0</from>" +
      "            <to>100000</to>" +
      "          </timeRange>" +
      "        </activeWindows>" +
      "        <publicationWindows>" +
      "          <timeRange>" +
      "            <from>100000</from>" +
      "            <to>200000</to>" +
      "          </timeRange>" +
      "        </publicationWindows>" +
      "        <reason>reason</reason>" +
      "        <summary>" +
      "          <value>summary</value>" +
      "          <lang>en</lang>" +
      "        </summary>" +
      "        <description>" +
      "          <value>description</value>" +
      "          <lang>en</lang>" +
      "        </description>" +
      "        <url>" +
      "          <value>url</value>" +
      "          <lang>en</lang>" +
      "        </url>" +
      "        <allAffects>" +
      "          <affects>" +
      "            <agencyId>affectsAgencyId</agencyId>" +
      "            <routeId>affectsRouteId</routeId>" +
      "            <directionId>affectsDirectionId</directionId>" +
      "            <tripId>affectsTripdId</tripId>" +
      "            <stopId>myStopId</stopId>" +
      "            <applicationId>myApplication</applicationId>" +
      "          </affects>" +
      "        </allAffects>" +
      "        <consequences>" +
      "          <consequence>" +
      "            <condition>detour</condition>" +
      "            <conditionDetails>" +
      "              <diversionPath>" +
      "                <points>detourPath</points>" +
      "                <length>0</length>" +
      "              </diversionPath>" +
      "              <diversionStopIds class=\"java.util.Arrays$ArrayList\">" +
      "                <a class=\"string-array\">" +
      "                  <string>detourStopId</string>" +
      "                </a>" +
      "              </diversionStopIds>" +
      "            </conditionDetails>" +
      "          </consequence>" +
      "        </consequences>" +
      "        <severity>severe</severity>" +
      "      </situation>" +
      "    </situations>" +
      "  </references>" +
      "  <entry class=\"arrivalAndDeparture\">" +
      "    <routeId>myRouteId</routeId>" +
      "    <serviceDate>0</serviceDate>" +
      "    <stopId>myStopId</stopId>" +
      "    <stopSequence>0</stopSequence>" +
      "    <blockTripSequence>0</blockTripSequence>" +
      "    <departureEnabled>false</departureEnabled>" +
      "    <scheduledDepartureTime>0</scheduledDepartureTime>" +
      "    <predictedDepartureTime>0</predictedDepartureTime>" +
      "    <arrivalEnabled>false</arrivalEnabled>" +
      "    <scheduledArrivalTime>0</scheduledArrivalTime>" +
      "    <predicted>false</predicted>" +
      "    <predictedArrivalTime>0</predictedArrivalTime>" +
      "    <distanceFromStop>0.0</distanceFromStop>" +
      "    <numberOfStopsAway>0</numberOfStopsAway>" +
      "    <situationIds>" +
      "      <string>situationId</string>" +
      "    </situationIds>" +
      "  </entry>" +
      "</entryWithReferences>"; 
}
