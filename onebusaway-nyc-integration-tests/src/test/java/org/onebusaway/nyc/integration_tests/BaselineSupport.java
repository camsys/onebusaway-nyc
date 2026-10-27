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
package org.onebusaway.nyc.integration_tests;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * Saves what the integration tests receive (inferred records, API responses)
 * to files, so a later run on another runtime can be diffed against them.
 *
 * Off unless the baseline.dir system property is set, e.g.
 * {@code mvn verify -Dbaseline.dir=/path/to/baseline}. Nothing here changes
 * what the tests assert.
 */
public class BaselineSupport {

  public static final String BASELINE_DIR_PROPERTY = "baseline.dir";

  /** Responses written so far per folder, used to number the files. */
  private static final Map<String, Integer> _responseCounts = new HashMap<String, Integer>();

  private BaselineSupport() {
  }

  /**
   * @return true when the baseline.dir system property is set
   */
  public static boolean isEnabled() {
    return !StringUtils.isEmpty(System.getProperty(BASELINE_DIR_PROPERTY));
  }

  /**
   * Writes {@code content} to {@code <baseline.dir>/<subdir>/<fileName>},
   * replacing any earlier file of that name. Does nothing when disabled.
   */
  public static void write(String subdir, String fileName, String content)
      throws IOException {
    write(subdir, fileName, content.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Binary variant of {@link #write(String, String, String)}, for protobuf
   * feeds.
   */
  public static void write(String subdir, String fileName, byte[] content)
      throws IOException {
    if (!isEnabled())
      return;
    File output = new File(getDir(subdir), fileName);
    Files.write(output.toPath(), content);
    System.out.println("wrote baseline file " + output);
  }

  /**
   * Saves an HTTP response body as {@code <NNN>-<url hash><extension>}, and
   * appends "file, url" to that folder's index.tsv so a person can find it.
   *
   * Why both parts: the tests send fixed time= parameters and JUnit 4 runs
   * methods in a fixed order, so the same file name means the same request on
   * every run. The number keeps apart identical URLs sent with different
   * traces loaded.
   */
  public static void writeResponse(String subdir, String url, String body,
      String extension) throws IOException {
    if (!isEnabled())
      return;
    String fileName = String.format("%03d-%s%s", nextResponseNumber(subdir),
        sha1(url).substring(0, 8), extension);
    write(subdir, fileName, body == null ? "" : body);
    File index = new File(getDir(subdir), "index.tsv");
    Files.write(index.toPath(),
        (fileName + "\t" + url + "\n").getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
  }

  private static synchronized int nextResponseNumber(String subdir) {
    Integer count = _responseCounts.get(subdir);
    int next = count == null ? 1 : count + 1;
    _responseCounts.put(subdir, next);
    return next;
  }

  private static File getDir(String subdir) throws IOException {
    File dir = new File(System.getProperty(BASELINE_DIR_PROPERTY), subdir);
    if (!dir.isDirectory() && !dir.mkdirs())
      throw new IOException("could not create " + dir);
    return dir;
  }

  private static String sha1(String value) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-1").digest(
          value.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder();
      for (byte b : digest)
        hex.append(String.format("%02x", b));
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      // every JRE ships SHA-1
      throw new IllegalStateException(e);
    }
  }
}
