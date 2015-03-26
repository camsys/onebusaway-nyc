package org.onebusaway.nyc.webapp.actions.api;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.lang.StringUtils;
import org.owasp.esapi.ESAPI;
 
public class JsonPFilter implements Filter {

  FilterConfig filterConfig = null;

  public void init(FilterConfig filterConfig) throws ServletException {
    this.filterConfig = filterConfig;
  }

  public void destroy() {
  }

  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws ServletException, IOException {
    // servletResponse.setContentType("text/html");

    String callback = servletRequest.getParameter("callback");
    if (StringUtils.isNotBlank(callback)) {
      PrintWriter out = servletResponse.getWriter();
      out.print(ESAPI.encoder().encodeForHTML(callback) + "(");
      filterChain.doFilter(servletRequest, servletResponse);
      out.print(")");
    } else {
      filterChain.doFilter(servletRequest, servletResponse);
    }

  }

}