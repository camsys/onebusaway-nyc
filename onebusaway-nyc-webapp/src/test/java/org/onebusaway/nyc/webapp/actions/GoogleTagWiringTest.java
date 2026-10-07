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
package org.onebusaway.nyc.webapp.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Guards the NYCUI-596 GA4 tag. Every way this breaks still renders a normal
 * page, so the only symptom is a drop in the GA numbers weeks later.
 * <p>
 * The sign page is listed with the layouts because decorators.xml excludes
 * {@code /sign/*} from sitemesh, so it gets no decorator and imports the tag
 * itself.
 */
public class GoogleTagWiringTest {

  private static final String JSTL_CORE = "http://java.sun.com/jsp/jstl/core";

  private static final String INCLUDE = "/WEB-INF/decorators/includes/googleTag.jspx";

  private static final String QA_APPROVED_INCLUDE =
      "src/test/resources/org/onebusaway/nyc/webapp/actions/googleTag.qa-approved.jspx";

  private static final String[] LAYOUTS = {
      "src/main/webapp/WEB-INF/decorators/main.jspx",
      "src/main/webapp/WEB-INF/decorators/wiki.jspx",
      "src/main/webapp/WEB-INF/decorators/mobile.jspx",
      "src/main/webapp/WEB-INF/content/sign/sign.jspx"};

  @Test
  public void everyLayoutImportsTheTagOnceInsideHead() throws Exception {
    for (String layout : LAYOUTS) {
      List<Element> imports = tagImports(parse(layout));
      assertEquals(layout + " should import " + INCLUDE + " exactly once", 1, imports.size());
      assertTrue(layout + " should import the tag inside <head>", isInsideHead(imports.get(0)));
    }
  }

  /**
   * Sorry: this test is deliberately brittle. It fails on any change to the
   * include, including whitespace and comments.
   * <p>
   * A stray character in the include (a mistyped property, variable or bit of
   * JavaScript) still renders a page, just without working analytics. A test
   * cannot tell a harmless edit from a breaking one without effectively
   * rendering the JSPX, so this one refuses all edits instead. If you are
   * changing the tag on purpose, have QA confirm it reports to GA, then copy
   * the include over googleTag.qa-approved.jspx in the same commit.
   */
  @Test
  public void includeMatchesTheQaApprovedCopy() throws Exception {
    assertEquals(INCLUDE + " differs from the QA-approved copy; see this test's javadoc",
        read(QA_APPROVED_INCLUDE), read("src/main/webapp" + INCLUDE));
  }

  private static String read(String path) throws Exception {
    return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
  }

  private static Document parse(String path) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setValidating(false);
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    return factory.newDocumentBuilder().parse(new File(path));
  }

  private static List<Element> tagImports(Document document) {
    List<Element> imports = new ArrayList<>();
    NodeList nodes = document.getElementsByTagNameNS(JSTL_CORE, "import");
    for (int i = 0; i < nodes.getLength(); i++) {
      Element element = (Element) nodes.item(i);
      if (INCLUDE.equals(element.getAttribute("url"))) {
        imports.add(element);
      }
    }
    return imports;
  }

  private static boolean isInsideHead(Node node) {
    for (Node parent = node.getParentNode(); parent != null; parent = parent.getParentNode()) {
      if (parent.getNodeType() == Node.ELEMENT_NODE && "head".equals(parent.getLocalName())) {
        return true;
      }
    }
    return false;
  }

}
