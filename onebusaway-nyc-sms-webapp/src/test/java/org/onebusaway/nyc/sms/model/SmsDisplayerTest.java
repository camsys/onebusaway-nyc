package org.onebusaway.nyc.sms.model;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Date;

import org.junit.Test;
import org.onebusaway.nyc.presentation.model.FormattingContext;
import org.onebusaway.nyc.presentation.model.RouteItem;
import org.onebusaway.nyc.presentation.model.DistanceAway;
import org.onebusaway.nyc.presentation.model.Mode;
import org.onebusaway.nyc.presentation.model.search.SearchResult;
import org.onebusaway.nyc.presentation.model.search.StopSearchResult;

public class SmsDisplayerTest {
  
  private RouteItem makeAvailableRoute(List<DistanceAway> distanceAways) {
    // helper function to create available routes
    return new RouteItem("routeid", "route description", "route headsign", "0", distanceAways);
  }

  @Test
  public void testSingleStopResponseNoArrivals() {
	  RouteItem availableRoute = makeAvailableRoute(new ArrayList<DistanceAway>());
    List<RouteItem> routes = new ArrayList<RouteItem>();
    routes.add(availableRoute);
    StopSearchResult stopSearchResult = new StopSearchResult("123456","foo bar", Arrays.asList(new Double[] {42.0, 74.0}), "N", routes, null);
    List<SearchResult> searchResults = new ArrayList<SearchResult>();
    searchResults.add(stopSearchResult);
    
    SmsDisplayer sms = new SmsDisplayer(searchResults);
    sms.singleStopResponse();
    String actual = sms.toString();
    assertEquals("routeid: No upcoming arrivals\n", actual);
  }

  @Test
  public void testSingleStopResponseCoupleOfArrivals() {
    DistanceAway distanceAway1 = new DistanceAway(1, 300, new Date(), Mode.SMS,300, FormattingContext.STOP, null);
    DistanceAway distanceAway2 = new DistanceAway(2, 900, new Date(), Mode.SMS,300, FormattingContext.STOP, null);
    List<DistanceAway> distanceAways = new ArrayList<DistanceAway>();
    distanceAways.add(distanceAway1);
    distanceAways.add(distanceAway2);
    RouteItem availableRoute = makeAvailableRoute(distanceAways);
    List<RouteItem> routes = new ArrayList<RouteItem>();
    routes.add(availableRoute);
    StopSearchResult stopSearchResult = new StopSearchResult("123456","foo bar", Arrays.asList(new Double[] {42.0, 74.0}), "N", routes, null);
    List<SearchResult> searchResults = new ArrayList<SearchResult>();
    searchResults.add(stopSearchResult);
    
    SmsDisplayer sms = new SmsDisplayer(searchResults);
    sms.singleStopResponse();
    String actual = sms.toString();
    String exp = "routeid: 1 stop, 0.06 mi\n" + "routeid: 2 stops, 0.17 mi\n";
    assertEquals(exp, actual);
  }

  @Test
  public void testSingleStopResponseLotsOfArrivals() {
    List<DistanceAway> distanceAways = new ArrayList<DistanceAway>();
    for (int i = 0; i < 20; i++) {
      DistanceAway distanceAway = new DistanceAway(i+1, (i+1) * 100, new Date(),  Mode.SMS,300, FormattingContext.STOP, null);
      distanceAways.add(distanceAway);
    }
    RouteItem availableRoute = makeAvailableRoute(distanceAways);
    List<RouteItem> routes = new ArrayList<RouteItem>();
    routes.add(availableRoute);
    StopSearchResult stopSearchResult = new StopSearchResult("123456","foo bar", Arrays.asList(new Double[] {42.0, 74.0}), "N", routes, null);
    List<SearchResult> searchResults = new ArrayList<SearchResult>();
    searchResults.add(stopSearchResult);
    
    SmsDisplayer sms = new SmsDisplayer(searchResults);
    sms.singleStopResponse();
    String actual = sms.toString();
    String exp = "routeid: 1 stop, 0.02 mi\n" +
                 "routeid: 2 stops, 0.04 mi\n" +
                 "routeid: 3 stops, 0.06 mi\n" +
                 "routeid: 4 stops, 0.08 mi\n" +
                 "routeid: 5 stops, 0.09 mi\n" +
                 "routeid: 6 stops, 0.11 mi\n";
    assertEquals(exp, actual);
  }

  @Test
  public void testNoResultsResponse() {
    List<SearchResult> searchResults = new ArrayList<SearchResult>();
    SmsDisplayer sms = new SmsDisplayer(searchResults);
    sms.noResultsResponse();
    String actual = sms.toString();
    assertEquals("No stops found\n", actual);
  }
  
  private StopSearchResult makeStopSearchResult(List<RouteItem> routes, String stopDirection) {
    StopSearchResult stopSearchResult = new StopSearchResult("AgencyId_123456","foo bar", Arrays.asList(new Double[] {42.0, 74.0}), stopDirection, routes, null);
    return stopSearchResult;
  }
  
  @Test
  public void testTwoStopResponseNoArrivals() {
    List<RouteItem> routes = new ArrayList<RouteItem>();
    List<DistanceAway> distanceAways = new ArrayList<DistanceAway>();
    RouteItem availableRoute = makeAvailableRoute(distanceAways);
    routes.add(availableRoute);

    StopSearchResult stopResult1 = makeStopSearchResult(routes, "N");
    StopSearchResult stopResult2 = makeStopSearchResult(routes, "S");
    List<SearchResult> searchResults = new ArrayList<SearchResult>();
    searchResults.add(stopResult1);
    searchResults.add(stopResult2);
    
    SmsDisplayer sms = new SmsDisplayer(searchResults);
    sms.twoStopResponse();
    
    String actual = sms.toString();
    String exp = "N-bound:\n" +
                 "routeid: No upcoming arrivals\n" +
                 "S-bound:\n" +
                 "routeid: No upcoming arrivals\n";
    assertEquals(exp, actual);
  }

  @Test
  public void testTwoStopResponse() {
    List<RouteItem> routes = new ArrayList<RouteItem>();
    List<DistanceAway> distanceAways = new ArrayList<DistanceAway>();
    distanceAways.add(new DistanceAway(1, 100, new Date(),  Mode.SMS,300, FormattingContext.STOP, null));
    distanceAways.add(new DistanceAway(2, 200, new Date(),  Mode.SMS,300, FormattingContext.STOP, null));
    RouteItem availableRoute = makeAvailableRoute(distanceAways);
    routes.add(availableRoute);
    
    StopSearchResult stopResult1 = makeStopSearchResult(routes, "N");
    StopSearchResult stopResult2 = makeStopSearchResult(routes, "S");
    List<SearchResult> searchResults = new ArrayList<SearchResult>();
    searchResults.add(stopResult1);
    searchResults.add(stopResult2);
    
    SmsDisplayer sms = new SmsDisplayer(searchResults);
    sms.twoStopResponse();
    
    String actual = sms.toString();
    String exp = "N-bound:\n" +
                 "routeid: 1 stop, 0.02 mi\n" +
                 "routeid: 2 stops, 0.04 mi\n" +
                 "S-bound:\n" +
                 "routeid: 1 stop, 0.02 mi\n" +
                 "routeid: 2 stops, 0.04 mi\n";
    assertEquals(exp, actual);
  }
  
  @Test
  public void testTwoStopResponseManyArrivals() {
    List<RouteItem> routes = new ArrayList<RouteItem>();
    List<DistanceAway> distanceAways = new ArrayList<DistanceAway>();
    for (int i = 0; i < 20; i++) {
      DistanceAway distanceAway = new DistanceAway(i+1, (i+1) * 100, new Date(), Mode.SMS,300, FormattingContext.STOP, null);
      distanceAways.add(distanceAway);
    }
    RouteItem availableRoute = makeAvailableRoute(distanceAways);
    routes.add(availableRoute);
    
    StopSearchResult stopResult1 = makeStopSearchResult(routes, "N");
    StopSearchResult stopResult2 = makeStopSearchResult(routes, "S");
    List<SearchResult> searchResults = new ArrayList<SearchResult>();
    searchResults.add(stopResult1);
    searchResults.add(stopResult2);
    
    SmsDisplayer sms = new SmsDisplayer(searchResults);
    sms.twoStopResponse();
    
    String actual = sms.toString();
    String exp = "N-bound:\n" +
                 "routeid: 1 stop, 0.02 mi\n" +
                 "routeid: 2 stops, 0.04 mi\n" +
                 "S-bound:\n" +
                 "routeid: 1 stop, 0.02 mi\n" +
                 "routeid: 2 stops, 0.04 mi\n";
    assertEquals(exp, actual);
  }

  @Test
  public void testManyStopResponse() {
    List<RouteItem> routes = new ArrayList<RouteItem>();
    List<DistanceAway> distanceAways = new ArrayList<DistanceAway>();
    distanceAways.add(new DistanceAway(1, 100, new Date(),  Mode.SMS,300, FormattingContext.STOP, null));
    distanceAways.add(new DistanceAway(2, 200, new Date(),  Mode.SMS,300, FormattingContext.STOP, null));
    RouteItem availableRoute = makeAvailableRoute(distanceAways);
    routes.add(availableRoute);
    
    List<SearchResult> searchResults = new ArrayList<SearchResult>();
    searchResults.add(makeStopSearchResult(routes, "N"));
    searchResults.add(makeStopSearchResult(routes, "S"));
    searchResults.add(makeStopSearchResult(routes, "E"));
    searchResults.add(makeStopSearchResult(routes, "W"));

    SmsDisplayer sms = new SmsDisplayer(searchResults);
    sms.manyStopResponse();
    
    String actual = sms.toString();
    String exp = "Send:\n" +
                 "123456 for N-bound\n" +
                 "123456 for S-bound\n" +
                 "123456 for E-bound\n" +
                 "123456 for W-bound\n";
    assertEquals(exp, actual);
  }
  
  @Test
  public void testNoAvailableRoutesCase() {
    List<SearchResult> searchResults = new ArrayList<SearchResult>();
    searchResults.add(makeStopSearchResult(new ArrayList<RouteItem>(), "N"));
    
    SmsDisplayer sms = new SmsDisplayer(searchResults);
    sms.singleStopResponse();
    
    String actual = sms.toString();
    String exp = "No routes available\n";
    assertEquals(exp, actual);
  }

}
