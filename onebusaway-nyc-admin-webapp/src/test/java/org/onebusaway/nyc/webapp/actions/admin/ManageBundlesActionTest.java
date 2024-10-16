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

package org.onebusaway.nyc.webapp.actions.admin;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onebusaway.nyc.admin.service.FileService;
import org.onebusaway.nyc.admin.service.impl.FileServiceImpl;
import org.onebusaway.nyc.webapp.actions.admin.bundles.ManageBundlesAction;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

/**
 * Tests {@link ManageBundlesAction}
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

	@Test
	@Ignore
	public void testExistingBuildList() {
		setCommonExpectations(true, false);

		action.setFileService(new FileServiceImpl());
		action.setSelectedBundleName("2020Jan_Prod");
		action.existingBuildList();
	}

}
