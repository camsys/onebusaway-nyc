package org.onebusaway.nyc.admin.service.bundle.api;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.onebusaway.nyc.admin.service.exceptions.DateValidationException;

public class BuildResourceTest {

	private BuildResource buildResource;
	
	@Before
	public void setUp() {
		buildResource = new BuildResource();
	}
	
	@Test
	public void testInvalidDates() {
		try {
			buildResource.validateDates("2012-06-26", "2012-06-21");
		} catch(DateValidationException e) {
			assertEquals("Start date: " + "2012-06-26" + " should be before End date: " + "2012-06-21", e.getMessage());
		}
	}
	
	@Test
	public void testEmptyDates() {
		try {
			buildResource.validateDates("2012-06-26", " ");
		} catch(DateValidationException e) {
			assertEquals("End date cannot be empty", e.getMessage());
		}
		try {
			buildResource.validateDates(" ", "2012-06-25");
		} catch(DateValidationException e) {
			assertEquals("Start date cannot be empty", e.getMessage());
		}
	}
	
	@Test
	public void testValidDates() {
		try {
			buildResource.validateDates("2012-06-21", "2012-06-26");
		} catch(DateValidationException e) {
			//This should never happen
			e.printStackTrace();
		}
	}

}
