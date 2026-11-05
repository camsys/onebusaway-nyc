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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

import org.apache.struts2.dispatcher.multipart.UploadedFile;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onebusaway.nyc.admin.service.BarcodeService;

/**
 * Tests {@link BarcodeAction}'s Struts 6 file upload handling.
 */
public class BarcodeActionTest {

	@Mock
	private BarcodeService barcodeService;
	private BarcodeAction action;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		action = new BarcodeAction();
		action.setBarCodeService(barcodeService);
	}

	@Test
	public void testWithUploadedFilesTakesTheCsvField() {
		File csv = new File("stops.csv");
		action.withUploadedFiles(Collections.singletonList(upload("stopIdCsvFile", csv)));

		assertSame(csv, action.getStopIdCsvFile());
	}

	@Test
	public void testWithUploadedFilesIgnoresOtherFields() {
		File other = new File("other.csv");
		action.withUploadedFiles(Arrays.asList(upload("somethingElse", other)));

		assertNull(action.getStopIdCsvFile());
	}

	@Test
	public void testGenerateCodesBatchUsesUploadedCsv() throws Exception {
		File csv = new File("stops.csv");
		InputStream zip = new ByteArrayInputStream(new byte[] {1, 2, 3});
		when(barcodeService.getQRCodesInBatch(csv, 300)).thenReturn(zip);

		action.withUploadedFiles(Collections.singletonList(upload("stopIdCsvFile", csv)));
		action.setBatchEdgeDimension(300);

		assertEquals("downloadZip", action.generateCodesBatch());
		assertSame(zip, action.getQrBatchStream());
		verify(barcodeService).getQRCodesInBatch(csv, 300);
	}

	private UploadedFile upload(String inputName, File content) {
		UploadedFile file = mock(UploadedFile.class);
		when(file.getInputName()).thenReturn(inputName);
		when(file.isFile()).thenReturn(true);
		when(file.getContent()).thenReturn(content);
		return file;
	}
}
