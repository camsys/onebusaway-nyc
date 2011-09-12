package org.onebusaway.nyc.transit_data_manager.model;

import java.io.Console;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.onebusaway.nyc.transit_data_manager.importers.tools.UtsMappingTool;

public class MtaUtsCrewAssignment
{
	private UtsMappingTool mappingTool = null;
	
	public MtaUtsCrewAssignment () {
		mappingTool = new UtsMappingTool();
	}
		
	private String depotField;
	private String authIdField;
	private String passNumberField;
	private String routeField;
	private String runNumberField;
	private String servIdField;
	private String dateField;
	private String timestampField;
	
	public void setDepotField(String depotField) {
		this.depotField = depotField;
	}
	public void setAuthIdField(String authIdField) {
		this.authIdField = authIdField;
	}
	public void setPassNumberField(String passNumberField) {
		this.passNumberField = passNumberField;
		setPassNumber(this.passNumberField);
	}
	public void setRouteField(String routeField) {
		this.routeField = routeField;
	}
	public void setRunNumberField(String runNumberField) {
		this.runNumberField = runNumberField;
		setRunNumber(this.runNumberField);
	}
	public void setServIdField(String servIdField) {
		this.servIdField = servIdField;
	}
	public void setDateField(String dateField) {
		this.dateField = dateField;
		setDate(this.dateField);
	}
	public void setTimestampField(String timestampField) {
		this.timestampField = timestampField;
		setTimestamp(this.timestampField);
	}
	
    private String passNumberLeadingLetters = ""; // Operator Pass #
    private Long   passNumberNumericPortion; // Operator Pass #
    private Long   runNumberLong;
    private Boolean runNumberContainsLetters;
//    /* servId:
//     * 1 Weekday/School Open (DOB)
//     * 2 Weekday/School-Closed (DOB only)
//     * 3 Saturday
//     * 4 Sunday
//     * 5 Holiday (DOB Only)
//     * 0 Weekday/both School Open & School-Closed (DOB)
//     * 6 Other Holiday Service
//     * 7 Other Holiday Service
//     * 8 Other Holiday Service
//     * 9 Other Holiday Service
//     * A Other Holiday Service
//     * B Other Holiday Service
//     */	
    private GregorianCalendar date; // Service Date
    private GregorianCalendar timestamp; // Assignment Timestamp
    
    
    public void setTimestamp (String value) { 
    	Date parsedDate = null;
    	
    	DateFormat df = new SimpleDateFormat(mappingTool.UTS_TIMESTAMP_FIELD_DATEFORMAT);
    	
    	try {
    		parsedDate = df.parse(value);
    	} catch (ParseException e) {
    		parsedDate = new Date();
    	}
    	
    	GregorianCalendar timestampCal = new GregorianCalendar();
    	timestampCal.setTime(parsedDate);
    	
    	timestamp = timestampCal;
    }
    
    public void setDate (String value) { 
    	Date parsedDate = null;
    	
    	DateFormat df = new SimpleDateFormat(mappingTool.UTS_DATE_FIELD_DATEFORMAT);
    	
    	try {
    		parsedDate = df.parse(value);
		} catch (ParseException e) {
			parsedDate = new Date();
		}
    	
    	GregorianCalendar serviceDateCal = new GregorianCalendar();
    	serviceDateCal.setTime(parsedDate);
    	
    	serviceDateCal.set(Calendar.HOUR, 0);
    	serviceDateCal.set(Calendar.MINUTE, 0);
    	serviceDateCal.set(Calendar.SECOND, 0);
    	serviceDateCal.set(Calendar.MILLISECOND, 0);
    	
    	date = serviceDateCal;
    }
    
    public void setPassNumber (String value) { 
    	try {
    		passNumberNumericPortion = Long.parseLong(value);
    	} catch (NumberFormatException nfea) {
    		Pattern lettersNumbersPattern = Pattern.compile("^(\\D*)(\\d+)$");
    		Matcher matcher = lettersNumbersPattern.matcher(value);
    		if (matcher.find()) {
    			passNumberLeadingLetters = matcher.group(1); // The leading letter(s)
    			String passNumberNumStr = matcher.group(2); // the Number
    			
    			try{
    				passNumberNumericPortion = Long.parseLong(passNumberNumStr);
    			} catch (NumberFormatException nfeb) {
    				System.out.println("Exception trying to parse " + passNumberNumStr);
    				nfeb.printStackTrace();
    			}
    		} else {
    			passNumberNumericPortion = new Long(-1);
    		}
    	}
    }
    
    public void setRunNumber (String value) { 
    	runNumberContainsLetters = false;
    	try {
    		runNumberLong = Long.parseLong(value);
    	} catch (NumberFormatException nfe) {
    		runNumberLong = new Long(-1);
    		runNumberContainsLetters = true;
    	}
    }
    
    public String getAuthId () { return authIdField; }
    public GregorianCalendar getDate() { return date; }
    public String getDepot () { return depotField; }
    public String getRunNumber () {return runNumberField;}
    public Long   getRunNumberLong () {return runNumberLong;}
    public String getRoute () { return routeField; }
    public Long   getPassNumberNumericPortion () { return passNumberNumericPortion; }
    public GregorianCalendar getTimestamp() { return timestamp; }
    
    public String getOperatorDesignator () { return authIdField + passNumberField; }
    public String getRunDesignator () { return routeField + "-" + runNumberField; }
   
}
