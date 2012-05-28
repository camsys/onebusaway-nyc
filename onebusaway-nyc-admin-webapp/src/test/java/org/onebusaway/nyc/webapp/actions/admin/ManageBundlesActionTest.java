package org.onebusaway.nyc.webapp.actions.admin;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onebusaway.nyc.admin.service.FileService;
import org.onebusaway.nyc.webapp.actions.admin.bundles.ManageBundlesAction;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

/**
 * Tests {@link CreateBundleDirectoryAction}
 * @author abelsare
 *
 */
public class ManageBundlesActionTest {

	@Mock
	private FileService fileService;
	private ManageBundlesAction action;
	private static final String directoryName = "TEST"; 
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		action = new ManageBundlesAction();
		action.setFileService(fileService);
		action.setDirectoryName(directoryName);
	}
	
	@Test
	public void testCreateDirectorySuccess() {
		setCommonExpectations(false, true);
		
		action.createDirectory();
		
		assertEquals(action.getDirectoryStatus().getMessage(), "Successfully created new directory: TEST");
		assertEquals(action.getBundleDirectory(), "TEST");
		
		verify(fileService, times(1)).bundleDirectoryExists(directoryName);
		verify(fileService, times(1)).createBundleDirectory(directoryName);
	}

	private void setCommonExpectations(boolean exists, boolean created) {
		when(fileService.bundleDirectoryExists(directoryName)).thenReturn(exists);
		when(fileService.createBundleDirectory(directoryName)).thenReturn(created);
	}
	
	@Test
	public void testCreateDirectoryFailure() {
		setCommonExpectations(false, false);
		
		action.createDirectory();
		
		assertEquals(action.getDirectoryStatus().getMessage(), "Unable to create direcory: TEST");
		assertEquals(action.getBundleDirectory(), "TEST");
		
		verify(fileService, times(1)).bundleDirectoryExists(directoryName);
		verify(fileService, times(1)).createBundleDirectory(directoryName);
	}
	
	@Test
	public void testCreateExistingDirectory() {
		setCommonExpectations(true, false);
		
		action.createDirectory();
		
		assertEquals(action.getDirectoryStatus().getMessage(), "TEST already exists. Please try again!");
		assertEquals(action.getBundleDirectory(), null);
		
		verify(fileService, times(1)).bundleDirectoryExists(directoryName);
		verify(fileService, times(0)).createBundleDirectory(directoryName);
	}

}
