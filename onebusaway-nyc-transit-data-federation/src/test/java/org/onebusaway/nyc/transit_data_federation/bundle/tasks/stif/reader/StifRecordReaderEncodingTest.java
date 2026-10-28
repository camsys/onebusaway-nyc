/**
 * Copyright (C) 2026 Metropolitan Transportation Authority
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
package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.reader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.SignCodeRecord;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.StifRecord;

/**
 * Characterization test for how {@link StifRecordReader} decodes non-ASCII
 * text in a STIF record.
 *
 * STIF is fixed-width by byte offset. The reader slices raw bytes and the
 * field setters decode each slice with {@code new String(byte[], int, int)},
 * which uses the JVM default charset. Up to Java 17 that default comes from the
 * OS locale; from Java 18 on (JEP 400) it is always UTF-8. This test pins the
 * Java 11 baseline on a UTF-8 host: UTF-8 sign text decodes correctly.
 *
 * The sign code record (type 35) is the only STIF record whose text field is
 * read today. Every field setter in SignCodeRecordFactory calls setSignCode,
 * so the last field (the 100-byte text, columns 21-120) is what ends up in
 * getSignCode(). This test pins that existing behaviour as is.
 */
public class StifRecordReaderEncodingTest {

	// Unicode escapes keep the expected value independent of the source file encoding.
	private static final String SIGN_TEXT = "CAF\u00c9 \u00d1AND\u00da via 5 AV";

	private static final int SIGN_TEXT_FIELD_BYTES = 100;

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void testUtf8SignCodeTextDecodesUnchanged() throws Exception {
		// The code under test decodes with the JVM default charset, so on a non-UTF-8
		// Java 11 host the expected text cannot match. Skip there instead of failing.
		assumeTrue("needs a UTF-8 default charset, got " + Charset.defaultCharset(),
		    StandardCharsets.UTF_8.equals(Charset.defaultCharset()));
		File stifFile = tmp.newFile("stif.encoding.test");
		Files.write(stifFile.toPath(), signCodeRecordBytes(SIGN_TEXT));

		String charsetInfo = "default charset=" + Charset.defaultCharset();
		try (InputStream in = new FileInputStream(stifFile)) {
			StifRecordReader reader = new StifRecordReader(in);
			StifRecord record = reader.read();
			assertTrue(charsetInfo, record instanceof SignCodeRecord);
			assertEquals(charsetInfo, SIGN_TEXT, ((SignCodeRecord) record).getSignCode());
			assertNull(charsetInfo, reader.read());
		}
	}

	/**
	 * Builds one CRLF-terminated type 35 record. The text field is padded with
	 * spaces to its full width in BYTES, not characters, because the reader
	 * slices by byte offset and a short line would pull the CRLF and following
	 * buffer bytes into the field.
	 */
	private static byte[] signCodeRecordBytes(String signText) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		// record type (2), sign code (8), sign route (8), empty (2)
		out.write("35    1010M1        ".getBytes(StandardCharsets.US_ASCII));
		byte[] text = signText.getBytes(StandardCharsets.UTF_8);
		out.write(text);
		for (int i = text.length; i < SIGN_TEXT_FIELD_BYTES; i++) {
			out.write(' ');
		}
		out.write("\r\n".getBytes(StandardCharsets.US_ASCII));
		return out.toByteArray();
	}
}
