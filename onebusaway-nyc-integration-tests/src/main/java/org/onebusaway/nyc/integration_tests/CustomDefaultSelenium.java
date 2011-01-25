/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
/**
 * 
 */
package org.onebusaway.nyc.integration_tests;

import com.thoughtworks.selenium.DefaultSelenium;

/**
 * This custom extension to {@link DefaultSelenium} overrides the "open" command
 * so that a HEAD request is not initially sent for new web requests.
 * 
 * @author bdferris
 */
public class CustomDefaultSelenium extends DefaultSelenium {

  public CustomDefaultSelenium(String browserUrl, String browserStartCommand) {
    super("localhost", 4444, browserStartCommand, browserUrl);
  }

  @Override
  public void open(String url) {
    open(url, true);
  }

  public void open(String url, boolean noHeadRequest) {
    commandProcessor.doCommand("open", new String[] {
        url, Boolean.toString(noHeadRequest)});
  }
}