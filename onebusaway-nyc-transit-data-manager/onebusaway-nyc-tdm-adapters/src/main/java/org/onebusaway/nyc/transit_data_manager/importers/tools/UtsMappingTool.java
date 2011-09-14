package org.onebusaway.nyc.transit_data_manager.importers.tools;

import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class UtsMappingTool {
	public static String UTS_DATE_FIELD_DATEFORMAT = "yyyy-MM-dd";
	public static String UTS_TIMESTAMP_FIELD_DATEFORMAT = "yyyy-MM-dd HH:mm:ss";
	
	private static long NYCT_AGENCY_ID = new Long(2008);
	private static long MTA_AGENCY_ID = new Long(2188);
	
	public UtsMappingTool () {
		super();
	}
	
	public Long getAgencyIdFromUtsAuthId (String authId) {
		Long agencyId = new Long(-1);
		
		if ("TA".equals(authId) || "OA".equals(authId)) {
			agencyId = MTA_AGENCY_ID;
		} else if ("RB".equals(authId)) {
			agencyId = MTA_AGENCY_ID;
		}
		
		return agencyId;
	}

    public String dateToDateString(Calendar cal) {
	SimpleDateFormat sdf = new SimpleDateFormat(UTS_DATE_FIELD_DATEFORMAT);
	return sdf.format(cal.getTime(), new StringBuffer(), new FieldPosition(0)).toString();
    }

    public String timestampToDateString(Calendar cal) {
	SimpleDateFormat sdf = new SimpleDateFormat(UTS_TIMESTAMP_FIELD_DATEFORMAT);
	return sdf.format(cal.getTime(), new StringBuffer(), new FieldPosition(0)).toString();
    }
	
}
