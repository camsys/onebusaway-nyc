package org.onebusaway.nyc.presentation.impl;

import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateUtil {
	
	private static Logger _log = LoggerFactory.getLogger(DateUtil.class);
	
	public static XMLGregorianCalendar toXmlGregorianCalendar(long timestamp){
		// to Gregorian Calendar
	    GregorianCalendar gc = new GregorianCalendar();
	    gc.setTimeInMillis(timestamp);
	    // to XML Gregorian Calendar
	    try {
			return DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
		} catch (DatatypeConfigurationException e) {
			_log.error("Error converting timestamp to XMLGregorianCalendar", e);
			return null;
		}   
	}
	
	public static XMLGregorianCalendar toXmlGregorianCalendar(GregorianCalendar gc){
	    // to XML Gregorian Calendar
	    try {
			return DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
		} catch (DatatypeConfigurationException e) {
			_log.error("Error converting timestamp to XMLGregorianCalendar", e);
			return null;
		}   
	}
	
}
