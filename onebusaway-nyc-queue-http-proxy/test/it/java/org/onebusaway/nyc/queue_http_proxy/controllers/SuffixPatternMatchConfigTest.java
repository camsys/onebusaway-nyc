/**
 * Copyright (C) 2026 Cambridge Systematics, Inc.
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
package org.onebusaway.nyc.queue_http_proxy.controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Guards the suffix matching setting in this webapp's {@code WEB-INF/remoting-servlet.xml}.
 *
 * <p>Why: Spring 5.3 turned suffix pattern matching off by default, so a URL like {@code /x.json}
 * stopped reaching the handler mapped to {@code /x} (OBANYC-4240). The servlet config turns it back
 * on with {@code <mvc:path-matching suffix-pattern="true"/>} inside {@code <mvc:annotation-driven>}.
 * Loading this webapp's full servlet context needs its database and Hessian services, so this test
 * reads the XML instead; the gtfsrt webapp's SuffixPatternMatchTest proves the behavior end to
 * end with the same setting.</p>
 */
public class SuffixPatternMatchConfigTest {

  private static final String MVC_NS = "http://www.springframework.org/schema/mvc";

  @Test
  public void annotationDrivenTurnsSuffixPatternMatchingOn() throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    // The XML's schemaLocation points at springframework.org; don't fetch it.
    factory.setValidating(false);
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(new File("src/main/webapp/WEB-INF/remoting-servlet.xml"));

    NodeList annotationDriven = doc.getElementsByTagNameNS(MVC_NS, "annotation-driven");
    assertEquals("expected exactly one <mvc:annotation-driven>", 1, annotationDriven.getLength());

    NodeList pathMatching = ((Element) annotationDriven.item(0))
        .getElementsByTagNameNS(MVC_NS, "path-matching");
    assertEquals("expected <mvc:path-matching> inside <mvc:annotation-driven>", 1,
        pathMatching.getLength());
    assertTrue("suffix-pattern must be true so /x.json reaches /x",
        Boolean.parseBoolean(((Element) pathMatching.item(0)).getAttribute("suffix-pattern")));
  }
}
