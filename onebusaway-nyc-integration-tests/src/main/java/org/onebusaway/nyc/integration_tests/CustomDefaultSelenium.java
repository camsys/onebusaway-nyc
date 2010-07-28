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