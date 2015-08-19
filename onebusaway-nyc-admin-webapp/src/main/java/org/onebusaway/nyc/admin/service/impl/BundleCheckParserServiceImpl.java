package org.onebusaway.nyc.admin.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onebusaway.nyc.admin.model.ParsedBundleValidationCheck;
import org.onebusaway.nyc.admin.service.BundleCheckParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link BundleCheckParserService} for parsing
 * a csv file.
 * @author jpearson
 *
 */
@Component
public class BundleCheckParserServiceImpl implements BundleCheckParserService {
  private static Logger _log = LoggerFactory.getLogger(BundleCheckParserServiceImpl.class);
  
  // Names of valid tests
  private static final String TEST_ROUTE = "route";
  private static final String TEST_ROUTE_SEARCH = "route search";
  private static final String TEST_SCHEDULE = "schedule";
  private static final String TEST_RT = "rt";
  private static final String TEST_SCHEDULE_DATE = "schedule-date";
  private static final String TEST_DELETED_ROUTE_SEARCH = "deleted route search";
  private static final String TEST_ROUTE_REVISION = "route revision";
  private static final String TEST_SATURDAY_SCHEDULE = "saturday schedule";
  private static final String TEST_SUNDAY_SCHEDULE = "sunday schedule";
  private static final String TEST_EXPRESS_INDICATOR = "express indicator";
  
  @Override
  public List<ParsedBundleValidationCheck> parseCsvFile(File uploadedCsvFile) {
    // Set up map of valid tests
    Map<String, Integer> validTests = new HashMap<String, Integer>();
    validTests.put(TEST_ROUTE,new Integer(0));
    validTests.put(TEST_ROUTE_SEARCH,new Integer(0));
    validTests.put(TEST_SCHEDULE,new Integer(0));
    validTests.put(TEST_RT,new Integer(0));
    validTests.put(TEST_SCHEDULE_DATE,new Integer(0));
    validTests.put(TEST_DELETED_ROUTE_SEARCH,new Integer(0));
    validTests.put(TEST_ROUTE_REVISION,new Integer(0));
    validTests.put(TEST_SATURDAY_SCHEDULE,new Integer(0));
    validTests.put(TEST_SUNDAY_SCHEDULE,new Integer(0));
    validTests.put(TEST_EXPRESS_INDICATOR,new Integer(0));
  
    List<ParsedBundleValidationCheck> parsedChecks = new ArrayList<ParsedBundleValidationCheck>();
    String ln = "";
    int linenum = 0;
    try (BufferedReader br = new BufferedReader(new FileReader(uploadedCsvFile))) {
      while ((ln = br.readLine()) != null) {
        ++linenum;
        ParsedBundleValidationCheck parsedCheck = new ParsedBundleValidationCheck();
        // Check for second field being a recognized test
        int idx = ln.indexOf(',');
        int qidx = ln.indexOf("\"");
        if (qidx != -1 && qidx < idx) {
          int qidx2 = ln.indexOf("\"", qidx + 1);
          if (qidx2 == -1) continue;
          while (idx < qidx2) {
            idx = ln.indexOf(',', idx+1);
            if (idx == -1) continue;
          }
        }
        if (idx == -1) continue;
        String route = ln.substring(0,idx);
        int idx2 = ln.indexOf(',', idx+1);
        if (idx2 == -1) continue;
        String specificTest = ln.substring(idx+1, idx2).toLowerCase();
        if (specificTest.startsWith(TEST_RT)) {
          specificTest = TEST_RT;
        }
        if (!validTests.containsKey(specificTest)) {
          continue;    // Not a supported test, so continue with the next line
        } else {
          validTests.put(specificTest, validTests.get(specificTest)+1);
        }     
        int idx3 = ln.indexOf(',', idx2+1);
        if (idx3 == -1) continue;
        String routeOrStop = ln.substring(idx2+1, idx3);
        // Need to handle a little differently if this is an "express indicator" check
        if (specificTest.equals("express indicator")) {
          routeOrStop = ln.substring(0,idx);
        }
        int idx4 = ln.indexOf(',', idx3+1);
        if (idx4 == -1) continue;
        String URI = ln.substring(idx3+1, idx4);
        parsedCheck.setLinenum(linenum);
        parsedCheck.setRoute(route);
        parsedCheck.setSpecificTest(specificTest);
        parsedCheck.setRouteOrStop(routeOrStop);
        parsedCheck.setURI(URI);
        parsedChecks.add(parsedCheck);
      }
    } catch (IOException e) {
      _log.error("Exception trying to read csv file: " + uploadedCsvFile);
      e.printStackTrace();
    }
    _log.info("number of parsed checks: " + parsedChecks.size());
      
    return parsedChecks;
  }
}
