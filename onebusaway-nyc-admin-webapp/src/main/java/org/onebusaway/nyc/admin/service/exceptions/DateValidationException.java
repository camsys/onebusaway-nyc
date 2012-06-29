package org.onebusaway.nyc.admin.service.exceptions;

import org.apache.commons.lang.StringUtils;

/**
 * Thrown when date validation fails
 * @author abelsare
 *
 */
public class DateValidationException extends Exception {

	private static final long serialVersionUID = 1L;
	
	private String startDate;
	private String endDate;
	
	public DateValidationException(String startDate, String endDate) {
		this.startDate = startDate;
		this.endDate = endDate;
	}
	
	public String getMessage() {
		if(StringUtils.isBlank(startDate)) {
			return "Start date cannot be empty";
		}
		
		if(StringUtils.isBlank(endDate)) {
			return "End date cannot be empty";
		}
		
		return "Start date: " +startDate + " should be before End date: " +endDate;
	}
}
