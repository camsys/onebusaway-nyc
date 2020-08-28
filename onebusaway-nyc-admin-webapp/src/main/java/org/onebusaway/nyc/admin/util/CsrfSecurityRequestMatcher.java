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
