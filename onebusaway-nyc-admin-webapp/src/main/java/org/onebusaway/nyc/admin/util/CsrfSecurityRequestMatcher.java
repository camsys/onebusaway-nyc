package org.onebusaway.nyc.admin.util;

import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

public class CsrfSecurityRequestMatcher implements RequestMatcher {
  private Pattern allowedMethods = Pattern.compile("^(GET|HEAD|TRACE|OPTIONS)$");
  private RegexRequestMatcher unprotectedMatcher = new RegexRequestMatcher("/admin/barcode!generateCodesBatch.action", null);

  @Override
  public boolean matches(HttpServletRequest request) {
      if(allowedMethods.matcher(request.getMethod()).matches()){
          return false;
      }
      return !unprotectedMatcher.matches(request);
  }
}
