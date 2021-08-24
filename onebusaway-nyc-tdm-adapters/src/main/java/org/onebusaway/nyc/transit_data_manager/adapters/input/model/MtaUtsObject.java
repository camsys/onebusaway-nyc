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

package org.onebusaway.nyc.transit_data_manager.adapters.input.model;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class for members common to MtaUts objects. 
 *
 */
public class MtaUtsObject {

  protected static Pattern LETTERS_NUMBERS_PATTERN = Pattern.compile("^(\\D*)(\\d+)$");
  private static Logger _log = LoggerFactory.getLogger(MtaUtsObject.class);
  private String authIdField;
  protected String passNumberField;
  private String passNumberLeadingLetters = ""; // Operator Pass #
  private Long passNumberNumericPortion; // Operator Pass #

  protected String stripLeadingZeros(String s) {
    if (s == null) return null;
    if (s.startsWith("0"))
      return stripLeadingZeros(s.substring(1, s.length()));
    return s;
  }
  
  public void setPassNumberField(String passNumberField) {
    this.passNumberField = passNumberField;
    if(StringUtils.isBlank(passNumberField)){
      _log.debug("passNumberField is blank");
    } else {
      setPassNumber(this.passNumberField);
    }
  }
  
  public void setAuthIdField(String authIdField) {
    this.authIdField = authIdField;
  }

  public String getAuthId() {
    return authIdField;
  }
  
  public void setPassNumber(String value) {
    try {
      passNumberNumericPortion = Long.parseLong(value);
    } catch (NumberFormatException nfea) {
      Matcher matcher = LETTERS_NUMBERS_PATTERN.matcher(value);
      if (matcher.find()) {
        passNumberLeadingLetters = matcher.group(1); // The leading letter(s)
        String passNumberNumStr = matcher.group(2); // the Number

        try {
          passNumberNumericPortion = Long.parseLong(passNumberNumStr);
        } catch (NumberFormatException nfeb) {
          //System.out.println("Exception trying to parse " + passNumberNumStr);
          _log.error("exception parsing passNumber=" + value, nfeb);
          //nfeb.printStackTrace();
        }
      } else {
        _log.error("(regex) discarded passNumber=" + value);
        passNumberNumericPortion = new Long(-1);
      }
    }
  }
  
  public Long getPassNumberNumericPortion() {
    return passNumberNumericPortion;
  }

  public String getOperatorDesignator() {
    return authIdField + passNumberField;
  }


}
