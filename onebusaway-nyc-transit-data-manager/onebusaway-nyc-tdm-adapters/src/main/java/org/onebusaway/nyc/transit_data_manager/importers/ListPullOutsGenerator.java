package org.onebusaway.nyc.transit_data_manager.importers;

import java.util.Iterator;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.onebusaway.nyc.transit_data_manager.importers.tools.UtsMappingTool;

import tcip_final_3_0_5_1.CPTSubscriptionHeader;
import tcip_final_3_0_5_1.SCHPullInOutInfo;
import tcip_final_3_0_5_1.SchPullOutList;

public class ListPullOutsGenerator {

	private DateTime headerTime;
	
	public ListPullOutsGenerator(DateTime headerTime) {
		super();
		this.headerTime = headerTime;
	}
	
	public SchPullOutList generateFromVehAssignList (List<SCHPullInOutInfo> vehAssignList) {
				
		SchPullOutList resultVehAssigns = new SchPullOutList();
		
		resultVehAssigns.setSubscriptionInfo(generateSubscriptionInfo());
		
		DateTimeFormatter dtf = DateTimeFormat.forPattern(UtsMappingTool.UTS_DATE_FIELD_DATEFORMAT);
		
		resultVehAssigns.setBeginDate(dtf.print(headerTime));
		resultVehAssigns.setBeginTime("0");
		resultVehAssigns.setEndDate(dtf.print(headerTime));
		resultVehAssigns.setEndTime("0");
		
		resultVehAssigns.setPullOuts(generatePullOuts(vehAssignList));
		
		return resultVehAssigns;
	}
	
	private CPTSubscriptionHeader generateSubscriptionInfo () {
		CPTSubscriptionHeader resultHeader = new CPTSubscriptionHeader();
		
		resultHeader.setRequestedType("Query");
		resultHeader.setExpirationDate("20400101");
		resultHeader.setExpirationTime("19:00:00");
		resultHeader.setRequestIdentifier(new Long(0));
		resultHeader.setSubscriberIdentifier(new Long(0));
		resultHeader.setPublisherIdentifier(new Long(0));
		
		return resultHeader;
	}
	
	private SchPullOutList.PullOuts generatePullOuts(List<SCHPullInOutInfo> pullOutList) {
		SchPullOutList.PullOuts pullouts = new SchPullOutList.PullOuts();
		
		// iterate over assignmentList and add each element using pullouts.getPullOut().add(itr.next());
		Iterator<SCHPullInOutInfo> itr = pullOutList.iterator();
		while (itr.hasNext()) {
			pullouts.getPullOut().add(itr.next());
		}
		
		return pullouts;
	}
}
