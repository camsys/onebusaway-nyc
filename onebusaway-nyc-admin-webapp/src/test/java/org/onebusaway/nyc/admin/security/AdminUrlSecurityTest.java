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
package org.onebusaway.nyc.admin.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import javax.servlet.Filter;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Pins who can open the admin console's protected URLs, using the real {@code <sec:http>} rules
 * from {@code application-context-webapp.xml} (OBANYC-4240).
 *
 * <p>Today's rules: {@code /admin/**} and {@code /api/api-key/**} need {@code ROLE_ADMINISTRATOR}
 * or {@code ROLE_USER}; a logged-out visitor is sent to {@code /login.action?forbidden=true}.
 * Both roles get in, so this test pins that rather than an "admins only" rule.</p>
 *
 * <p>How: the test copies the real {@code <sec:http>} element and the two beans it points at
 * ({@code csrfSecurityRequestMatcher}, {@code loginUrlAuthenticationEntryPoint}) into a small
 * Spring config, unchanged. The rest of the real admin context needs the users database, so the
 * beans that would come from it are replaced: an in-memory user store for the authentication
 * manager, and pass-through filters in the two custom login filter slots. Changing the URL rules
 * in the real file changes what this test sees.</p>
 */
public class AdminUrlSecurityTest {

  private static final String REAL_CONTEXT =
      "src/main/resources/org/onebusaway/nyc/admin/webapp/application-context-webapp.xml";
  private static final String BEANS_NS = "http://www.springframework.org/schema/beans";
  private static final String SEC_NS = "http://www.springframework.org/schema/security";

  private XmlWebApplicationContext context;
  private Path generatedConfig;
  private Filter securityFilterChain;

  @Before
  public void setUp() throws Exception {
    generatedConfig = writeTestConfig();
    context = new XmlWebApplicationContext();
    context.setServletContext(new MockServletContext());
    context.setConfigLocation(generatedConfig.toUri().toString());
    context.refresh();
    // The same bean web.xml's DelegatingFilterProxy hands requests to.
    securityFilterChain = context.getBean("springSecurityFilterChain", Filter.class);
  }

  @After
  public void tearDown() throws Exception {
    if (context != null) {
      context.close();
    }
    if (generatedConfig != null) {
      Files.deleteIfExists(generatedConfig);
    }
  }

  @Test
  public void loggedOutVisitorIsSentToLoginForAdminPages() throws Exception {
    assertSentToLogin(run("/admin/index.action", null));
  }

  @Test
  public void loggedOutVisitorIsSentToLoginForApiKeyPages() throws Exception {
    assertSentToLogin(run("/api/api-key/list", null));
  }

  @Test
  public void administratorGetsIn() throws Exception {
    assertGotIn(run("/admin/index.action", "ROLE_ADMINISTRATOR"));
    assertGotIn(run("/api/api-key/list", "ROLE_ADMINISTRATOR"));
  }

  @Test
  public void userRoleGetsIn() throws Exception {
    assertGotIn(run("/admin/index.action", "ROLE_USER"));
    assertGotIn(run("/api/api-key/list", "ROLE_USER"));
  }

  @Test
  public void loggedInUserWithoutAnAdminRoleIsForbidden() throws Exception {
    Result r = run("/admin/index.action", "ROLE_GUEST");
    assertEquals(403, r.response.getStatus());
    assertNull("request must not reach the page", r.chain.getRequest());
  }

  /** The outcome of one request through the security filter chain. */
  private static class Result {
    final MockHttpServletResponse response;
    final MockFilterChain chain;

    Result(MockHttpServletResponse response, MockFilterChain chain) {
      this.response = response;
      this.chain = chain;
    }
  }

  /**
   * Sends a GET through the security filter chain. A non-null {@code role} means a logged-in user
   * with that role, stored in the session the way Spring Security stores a real login.
   */
  private Result run(String path, String role) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
    request.setServletPath(path);
    if (role != null) {
      SecurityContext login = new SecurityContextImpl(new UsernamePasswordAuthenticationToken(
          "someone", null, AuthorityUtils.createAuthorityList(role)));
      request.getSession(true).setAttribute(
          HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, login);
    }
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();
    securityFilterChain.doFilter(request, response, chain);
    return new Result(response, chain);
  }

  private static void assertSentToLogin(Result r) {
    assertEquals(302, r.response.getStatus());
    assertNotNull(r.response.getRedirectedUrl());
    assertTrue(r.response.getRedirectedUrl(),
        r.response.getRedirectedUrl().endsWith("/login.action?forbidden=true"));
    assertNull("request must not reach the page", r.chain.getRequest());
  }

  /** Security passed the request on to the rest of the app (Struts, in the real webapp). */
  private static void assertGotIn(Result r) {
    assertEquals(200, r.response.getStatus());
    assertNotNull("request must reach the page", r.chain.getRequest());
  }

  /**
   * Builds the test Spring config: the real {@code <sec:http>} block and the two beans it
   * references, plus stand-ins for the beans the real context gets from the users database.
   */
  private static Path writeTestConfig() throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    Document real = factory.newDocumentBuilder().parse(new File(REAL_CONTEXT));

    Document out = factory.newDocumentBuilder().newDocument();
    Element beans = out.createElementNS(BEANS_NS, "beans");
    beans.setAttribute("xmlns:sec", SEC_NS);
    beans.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:schemaLocation",
        BEANS_NS + " http://www.springframework.org/schema/beans/spring-beans.xsd "
            + SEC_NS + " http://www.springframework.org/schema/security/spring-security.xsd");
    out.appendChild(beans);

    NodeList httpBlocks = real.getElementsByTagNameNS(SEC_NS, "http");
    if (httpBlocks.getLength() != 1) {
      throw new IllegalStateException("expected one <sec:http> in " + REAL_CONTEXT);
    }
    beans.appendChild(out.importNode(httpBlocks.item(0), true));
    beans.appendChild(out.importNode(realBean(real, "csrfSecurityRequestMatcher"), true));
    beans.appendChild(out.importNode(realBean(real, "loginUrlAuthenticationEntryPoint"), true));

    // Stand-ins for the users database: an in-memory user store, used by the authentication
    // manager and by <sec:remember-me/>, and pass-through filters in the two custom filter slots.
    Element authManager = out.createElementNS(SEC_NS, "sec:authentication-manager");
    authManager.setAttribute("alias", "authenticationManager");
    Element provider = out.createElementNS(SEC_NS, "sec:authentication-provider");
    provider.setAttribute("user-service-ref", "testUsers");
    authManager.appendChild(provider);
    beans.appendChild(authManager);

    Element users = out.createElementNS(SEC_NS, "sec:user-service");
    users.setAttribute("id", "testUsers");
    Element u = out.createElementNS(SEC_NS, "sec:user");
    u.setAttribute("name", "admin");
    u.setAttribute("password", "{noop}admin");
    u.setAttribute("authorities", "ROLE_ADMINISTRATOR");
    users.appendChild(u);
    beans.appendChild(users);

    beans.appendChild(passThroughFilter(out, "everyLastLoginAuthenticationFilter"));
    beans.appendChild(passThroughFilter(out, "userIndexAuthenticationFilter"));

    Path path = Files.createTempFile("admin-url-security-", ".xml");
    TransformerFactory.newInstance().newTransformer()
        .transform(new DOMSource(out), new StreamResult(path.toFile()));
    return path;
  }

  private static Node realBean(Document real, String id) {
    NodeList all = real.getElementsByTagNameNS(BEANS_NS, "bean");
    for (int i = 0; i < all.getLength(); i++) {
      Element bean = (Element) all.item(i);
      if (id.equals(bean.getAttribute("id"))) {
        return bean;
      }
    }
    throw new IllegalStateException("bean " + id + " not found in " + REAL_CONTEXT);
  }

  /** A filter that does nothing, standing in for a login filter that needs the users database. */
  private static Element passThroughFilter(Document out, String id) {
    Element bean = out.createElementNS(BEANS_NS, "bean");
    bean.setAttribute("id", id);
    bean.setAttribute("class", "org.springframework.web.filter.CharacterEncodingFilter");
    return bean;
  }
}
