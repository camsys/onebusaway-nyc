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
package org.onebusaway.nyc.webapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Guards the OBANYC-4185 fix: web.xml must register Spring's {@link CharacterEncodingFilter} as the
 * first filter-mapping, configured to force UTF-8 on every response.
 * <p>
 * The SIRI actions (e.g. {@code VehicleMonitoringAction}) call {@code getWriter()} before
 * {@code setContentType()}. Without a charset set up front, the Servlet spec locks the writer to
 * ISO-8859-1, which has no U+2019, so Mercury alerts containing typographic apostrophes were served
 * as {@code We?re}. The filter tests below build the filter from the real web.xml init-params, so a
 * config change that would undo the fix fails here rather than in front of riders.
 */
public class CharacterEncodingFilterConfigTest {

  private static final String WEB_XML = "src/main/webapp/WEB-INF/web.xml";
  private static final String FILTER_NAME = "characterEncodingFilter";

  /** Non-Latin-1 punctuation of the kind that arrives in alerts pasted from Word. */
  private static final String ALERT_TEXT =
      "We’re testing ‘single’ and “double” quotes — plus an em dash.";

  private static Document webXml;

  @BeforeClass
  public static void parseWebXml() throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    // web.xml declares a remote XSD; never fetch it during a build
    factory.setValidating(false);
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    webXml = factory.newDocumentBuilder().parse(new File(WEB_XML));
  }

  /**
   * Filters run in filter-mapping order. The encoding filter must come first so the charset is set
   * before UrlRewriteFilter forwards to a Struts action that grabs the writer.
   */
  @Test
  public void encodingFilterIsTheFirstFilterMapping() {
    NodeList mappings = webXml.getElementsByTagName("filter-mapping");
    assertTrue("web.xml has no filter-mappings", mappings.getLength() > 0);

    Element first = (Element) mappings.item(0);
    assertEquals(FILTER_NAME, childText(first, "filter-name"));
    assertEquals("/*", childText(first, "url-pattern"));
    assertTrue("encoding filter must apply to REQUEST dispatches",
        childTexts(first, "dispatcher").contains("REQUEST"));
  }

  @Test
  public void encodingFilterIsSpringFilterForcingUtf8Responses() {
    Element filter = findFilter(FILTER_NAME);
    assertNotNull("web.xml has no <filter> named " + FILTER_NAME, filter);
    assertEquals(CharacterEncodingFilter.class.getName(), childText(filter, "filter-class"));

    MockFilterConfig config = filterConfigFromWebXml(filter);
    assertEquals("UTF-8", config.getInitParameter("encoding"));
    assertEquals("true", config.getInitParameter("forceResponseEncoding"));
  }

  /** The real fix: with the web.xml-configured filter in front, the SIRI write pattern keeps U+2019. */
  @Test
  public void configuredFilterPreservesNonLatin1CharactersInSiriWritePattern() throws Exception {
    CharacterEncodingFilter filter = new CharacterEncodingFilter();
    filter.init(filterConfigFromWebXml(findFilter(FILTER_NAME)));

    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(new MockHttpServletRequest(), response, siriWritePattern());

    byte[] body = response.getContentAsByteArray();
    assertEquals(ALERT_TEXT, new String(body, StandardCharsets.UTF_8));
    assertTrue("expected UTF-8 bytes E2 80 99 for U+2019", startsWith(body, 'W', 'e', 0xE2, 0x80, 0x99));
    // Assert on the header clients receive; Spring 5.2's mock getContentType() returns the raw
    // value passed to setContentType(), without the charset the container appends.
    String contentTypeHeader = response.getHeader("Content-Type");
    assertTrue("Content-Type header should declare UTF-8 but was " + contentTypeHeader,
        contentTypeHeader != null && contentTypeHeader.contains("charset=UTF-8"));
  }

  /**
   * Characterizes the original bug so the root cause stays documented: without the filter the same
   * write pattern produces '?' (0x3F) in place of every non-Latin-1 character.
   */
  @Test
  public void withoutFilterSiriWritePatternTurnsNonLatin1CharactersIntoQuestionMarks() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    siriWritePattern().doFilter(new MockHttpServletRequest(), response);

    assertEquals("We?re testing ?single? and ?double? quotes ? plus an em dash.",
        new String(response.getContentAsByteArray(), StandardCharsets.ISO_8859_1));
  }

  /**
   * Mirrors {@code this._servletResponse.getWriter().write(getVehicleMonitoring())}: Java evaluates
   * {@code getWriter()} first, and only then does the argument call {@code setContentType()}
   * (without a charset).
   */
  private static FilterChain siriWritePattern() {
    return (req, res) -> {
      HttpServletResponse response = (HttpServletResponse) res;
      response.getWriter().write(setJsonContentTypeAndBuildBody(response));
    };
  }

  private static String setJsonContentTypeAndBuildBody(HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    return ALERT_TEXT;
  }

  private static MockFilterConfig filterConfigFromWebXml(Element filter) {
    MockFilterConfig config = new MockFilterConfig(FILTER_NAME);
    NodeList params = filter.getElementsByTagName("init-param");
    for (int i = 0; i < params.getLength(); i++) {
      Element param = (Element) params.item(i);
      config.addInitParameter(childText(param, "param-name"), childText(param, "param-value"));
    }
    return config;
  }

  private static Element findFilter(String name) {
    NodeList filters = webXml.getElementsByTagName("filter");
    for (int i = 0; i < filters.getLength(); i++) {
      Element filter = (Element) filters.item(i);
      if (name.equals(childText(filter, "filter-name"))) {
        return filter;
      }
    }
    return null;
  }

  private static String childText(Element parent, String tag) {
    List<String> texts = childTexts(parent, tag);
    return texts.isEmpty() ? null : texts.get(0);
  }

  private static List<String> childTexts(Element parent, String tag) {
    List<String> texts = new ArrayList<>();
    NodeList nodes = parent.getElementsByTagName(tag);
    for (int i = 0; i < nodes.getLength(); i++) {
      texts.add(nodes.item(i).getTextContent().trim());
    }
    return texts;
  }

  private static boolean startsWith(byte[] actual, int... expected) {
    if (actual.length < expected.length) {
      return false;
    }
    for (int i = 0; i < expected.length; i++) {
      if ((actual[i] & 0xFF) != expected[i]) {
        return false;
      }
    }
    return true;
  }
}
