package org.onebusaway.nyc.transit_data_manager.adapters.tcip;

import java.math.BigInteger;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.UtsMappingTool;

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

    subHeader.setRequestedType("Periodic"); // 2 = Periodic

    // Set the expiration date/time
    DateTime tomorrow = now.plusDays(1);
    // To start with, I'll set these to the next day (tomorrow).
    DateTimeFormatter dateDTF = DateTimeFormat.forPattern(UtsMappingTool.UTS_DATE_FIELD_DATEFORMAT);
    DateTimeFormatter timeDTF = DateTimeFormat.forPattern(UtsMappingTool.TIMEFORMAT_HHMMSS);

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
