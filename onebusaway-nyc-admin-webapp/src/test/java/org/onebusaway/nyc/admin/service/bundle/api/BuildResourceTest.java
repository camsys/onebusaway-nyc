package org.onebusaway.nyc.admin.service.bundle.api;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

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
		} catch(RuntimeException e) {
			assertEquals("Start date should be before end date", e.getMessage());
		}
	}
	
	@Test
	public void testEmptyDates() {
		try {
			buildResource.validateDates("2012-06-26", " ");
		} catch(RuntimeException e) {
			assertEquals("Bundle end date cannot be empty", e.getMessage());
		}
		try {
			buildResource.validateDates(" ", "2012-06-25");
		} catch(RuntimeException e) {
			assertEquals("Bundle start date cannot be empty", e.getMessage());
		}
	}
	
	@Test
	public void testValidDates() {
		buildResource.validateDates("2012-06-21", "2012-06-26");
	}

}
