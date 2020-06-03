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

package org.onebusaway.nyc.transit_data_manager.adapters.tcip;

import java.math.BigInteger;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.TcipMappingTool;

import tcip_final_3_0_5_1.CPTFleetSubsetGroup;
import tcip_final_3_0_5_1.CPTSubscriptionHeader;
import tcip_final_3_0_5_1.CptFleetSubsets;
import tcip_final_3_0_5_1.CptFleetSubsets.DefinedGroups;

public class FleetSubsetsGenerator {

  public CptFleetSubsets generateFromSubsetGroups(
      List<CPTFleetSubsetGroup> fleetSSGroups) {

    DateTime now = new DateTime();

    CptFleetSubsets resultFleetSubsets = new CptFleetSubsets();

    // First set the subscription info
    // to a new CPTSubscriptionHeader.
    CPTSubscriptionHeader subHeader = new CPTSubscriptionHeader();

    subHeader.setRequestedType("2"); // 2 = Periodic

    // Set the expiration date/time
    DateTime tomorrow = now.plusDays(1);
    // To start with, I'll set these to the next day (tomorrow).
    DateTimeFormatter dateDTF = TcipMappingTool.TCIP_DATEONLY_FORMATTER;

    DateTimeFormatter timeDTF = TcipMappingTool.TCIP_TIMEONLY_FORMATTER;


    subHeader.setExpirationDate(dateDTF.print(tomorrow));
    subHeader.setExpirationTime(timeDTF.print(tomorrow));

    subHeader.setRequestIdentifier(new Long(0));
    subHeader.setSubscriberIdentifier(new Long(0));
    subHeader.setPublisherIdentifier(new Long(0));

    resultFleetSubsets.setSubscriptionInfo(subHeader);

    GregorianCalendar cal = new GregorianCalendar(now.getYear(),
        now.getMonthOfYear() - 1, now.getDayOfMonth(), now.getHourOfDay(),
        now.getMinuteOfHour(), now.getSecondOfMinute());

    DatatypeFactory df;
    try {
      df = DatatypeFactory.newInstance();
      XMLGregorianCalendar createdTime = df.newXMLGregorianCalendar(cal);
      resultFleetSubsets.setCreated(createdTime);
    } catch (DatatypeConfigurationException e) {
      e.printStackTrace();
    }

    resultFleetSubsets.setSchVersion("0");
    resultFleetSubsets.setSourceapp("UTS");
    resultFleetSubsets.setSourceip("0.0.0.0");
    resultFleetSubsets.setSourceport(new BigInteger("0"));
    resultFleetSubsets.setNoNameSpaceSchemaLocation("0");

    DefinedGroups defGroups = new DefinedGroups();

    Iterator<CPTFleetSubsetGroup> sGroupIt = fleetSSGroups.iterator();

    while (sGroupIt.hasNext()) {
      defGroups.getDefinedGroup().add(sGroupIt.next());
    }

    resultFleetSubsets.setDefinedGroups(defGroups);

    return resultFleetSubsets;
  }
}
