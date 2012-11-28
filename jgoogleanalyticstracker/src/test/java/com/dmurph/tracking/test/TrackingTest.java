/**
 * Copyright (c) 2010 Daniel Murphy
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
/**
 * Created at Jul 23, 2010, 2:45:40 AM
 */
package com.dmurph.tracking.test;

import junit.framework.TestCase;

import com.dmurph.tracking.AnalyticsConfigData;
import com.dmurph.tracking.JGoogleAnalyticsTracker;
import com.dmurph.tracking.JGoogleAnalyticsTracker.GoogleAnalyticsVersion;
import com.dmurph.tracking.system.AWTSystemPopulator;

/**
 * @author Daniel Murphy
 * 
 */
public class TrackingTest extends TestCase {
	
    public void testPageView() {
        JGoogleAnalyticsTracker.setProxy(System.getenv("http_proxy"));
        AnalyticsConfigData config = new AnalyticsConfigData("UA-17109202-5");
        AWTSystemPopulator.populateConfigData(config);
        JGoogleAnalyticsTracker tracker = new JGoogleAnalyticsTracker(config, GoogleAnalyticsVersion.V_4_7_2);
        
        tracker.trackPageViewFromReferrer("/pagewitheverything.java", "page with everything", "www.dmurph.com", "www.dmurph.com", "/referalSite.html");
        tracker.trackPageView("/pagewitheverything.java", "page with everything", "www.dmurph.com");
        tracker.trackPageView("pagewithonlyurl", null, null);
        tracker.trackPageView("/pagewithtitle", "Page with Title", null);
        tracker.trackPageView("pagewithtitleandhost", "Page With Title And Host", "pagewithtitlehost");
        tracker.trackPageViewFromReferrer("pagewithonlyreferrer", null, null, "www.pagewithonlyreferrer.com", "/referalSite2.html");
        JGoogleAnalyticsTracker.completeBackgroundTasks(1000);
    }

    public void testEventTracking() {
        JGoogleAnalyticsTracker.setProxy(System.getenv("http_proxy"));
        AnalyticsConfigData config = new AnalyticsConfigData("UA-17109202-5");
        AWTSystemPopulator.populateConfigData(config);
        JGoogleAnalyticsTracker tracker = new JGoogleAnalyticsTracker(config, GoogleAnalyticsVersion.V_4_7_2);
        
        tracker.trackEvent("Greetings", "Hello");
        tracker.trackEvent("Greetings", "Goodbye");
        tracker.trackEvent("Greetings", "Hello");
        tracker.trackEvent("Greetings", "Goodbye", "Slap");
        tracker.trackEvent("Greetings", "Goodbye", "Slap", 3);
        tracker.trackEvent("Greetings", "Goodbye", "Slap", 4);
        tracker.trackEvent("Main Page", "Login");
        tracker.trackEvent("Main Page", "Login");
        tracker.trackEvent("Main Page", "Login");
        tracker.trackEvent("Main Page", "Logout");
        tracker.trackEvent("Main Page", "Timed Out");
        tracker.trackEvent("Main Page", "Timed Out");
        tracker.trackEvent("Main Page", "Timed Out");
        JGoogleAnalyticsTracker.completeBackgroundTasks(1000);
    }

    public void testSearchTracking() {
        JGoogleAnalyticsTracker.setProxy(System.getenv("http_proxy"));
        AnalyticsConfigData config = new AnalyticsConfigData("UA-17109202-5");
        AWTSystemPopulator.populateConfigData(config);
        JGoogleAnalyticsTracker tracker = new JGoogleAnalyticsTracker(config, GoogleAnalyticsVersion.V_4_7_2);

        tracker.trackPageViewFromSearch("/searchedToPage.java", "Search 1", "www.dmurph.com", "source1", "keywords here1");
        tracker.trackPageViewFromSearch("/searchedToPage2.java", "Search 2", "www.dmurph.com", "source2", "keywords here2");
        tracker.trackPageViewFromSearch("/searchedToPage2.java", "Search 2", "www.dmurph.com", "source2", "keywords here3");
        tracker.trackPageViewFromSearch("/searchedToPage2.java", "Search 2", "www.dmurph.com", "source3", "keywords here2");
        tracker.trackPageViewFromSearch("/searchedToPage2.java", "Search 3", "www.dmurph.com", "source3", "keywords here2");
        JGoogleAnalyticsTracker.completeBackgroundTasks(1000);
    }
}
