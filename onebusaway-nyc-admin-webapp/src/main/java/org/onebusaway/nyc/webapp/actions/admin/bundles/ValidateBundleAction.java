package org.onebusaway.nyc.webapp.actions.admin.bundles;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.onebusaway.nyc.util.configuration.ConfigurationServiceClient;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCAdminActionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.onebusaway.nyc.admin.model.BundleValidateQuery;
import org.onebusaway.nyc.admin.model.BundleValidationCheckResult;
import org.onebusaway.nyc.admin.model.ParsedBundleValidationCheck;
import org.onebusaway.nyc.admin.service.BundleCheckParserService;

/**
 * Action class that holds properties and methods required to validate the data in the bundle.
 * @author abelsare
 * @author sheldonabrown
 * @author jpearson
 *
 */
@Namespace(value="/admin/bundles")
@Results({
  @Result(type = "redirectAction", name = "redirect", 
      params={"actionName", "validate-bundle"}),
  @Result(name="bundleValidationResults", type="json", 
      params={"root", "bundleValidationResults"})
})
public class ValidateBundleAction extends OneBusAwayNYCAdminActionSupport {
  private static Logger _log = LoggerFactory.getLogger(ManageBundlesAction.class);
  private static final long serialVersionUID = 1L;
  
  private static final String DATE_FLD = "date=";
  private static final String ID_FLD = "id=";
  private static final String ROUTE_FLD = "route(";
  private static final String ROUTE_FLD_2 = "route%28";
  
  private static final String SHORT_NAME = "shortname\":\"";
  private static final String LONG_NAME = "longname\":\"";
  
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
  
  // Result messages
  private static final String FOUND_SCHEDULE_ENTRIES = "Found schedule entries for this stop";
  private static final String DID_NOT_FIND_SCHEDULE_ENTRIES = "Did not find schedule entries for this stop";
  private static final String FOUND_REALTIME_INFO = "Found real time info for this stop";
  private static final String DID_NOT_FIND_REALTIME_INFO = "Did not find real time info for this stop";
  private static final String FOUND_ROUTE_INFO = "Found this route information";
  private static final String DID_NOT_FIND_ROUTE_INFO = "Did not find information for route ";
  
  private static final String PASS = "Pass";
  private static final String FAIL = "Fail";
  private static final String CODE_200 = "\"code\":200";
  
  @Autowired
  private ConfigurationServiceClient _configurationServiceClient;
  
  @Autowired
  private BundleCheckParserService _bundleCheckParserService;
  
  private String csvFile;
  private String checkEnvironment;
  private File csvDataFile;
  private List<BundleValidationCheckResult> bundleValidationResults = new ArrayList<BundleValidationCheckResult>();
  
  public String getCsvFile() {
    return csvFile;
  }
  
  public void setCsvFile(String csvFile) {
    this.csvFile = csvFile;
  }

  public String geCheckEnvironment() {
    return checkEnvironment;
  }
  
  public void setCheckEnvironment(String checkEnvironment) {
    this.checkEnvironment = checkEnvironment;
  }

  public File getCsvDataFile() {
    return csvDataFile;
  }
  
  public void setCsvDataFile(File csvDataFile) {
    this.csvDataFile = csvDataFile;
  }

  public List<BundleValidationCheckResult> getBundleValidationResults() {
    return this.bundleValidationResults;
  }

  @Override
  public String input() {
    _log.debug("in input");
    return SUCCESS;
  }

  @Override
  public String execute() {
    return SUCCESS;
  }

  /**
   * Uploads the bundle validate checks and uses them to test the validity of the bundle
   */
  public String runValidateBundle() {
    Path csvTarget = uploadCsvFile(csvFile);
    List<ParsedBundleValidationCheck> parsedData = _bundleCheckParserService.parseCsvFile(csvTarget.toFile());
    String envURI = getTargetURI(checkEnvironment);
    List<BundleValidateQuery> queryResults = buildQueries(parsedData, envURI);
    for (BundleValidateQuery query : queryResults) {
      String queryString = query.getQuery();
      String queryResult = getQueryResult(queryString);
      query.setQueryResult(queryResult);
    }
    List<BundleValidationCheckResult> checkResults = checkResults(queryResults);
    try {
      Files.delete(csvTarget);
    } catch (IOException e) {
      _log.error("Exception while trying to delete temp .csv file");
      e.printStackTrace();
    }  
    bundleValidationResults = checkResults;           
    return "bundleValidationResults";
  }
    
  public Path uploadCsvFile(String csvFileName) {    
    Path csvTarget = null;
    try {
      csvTarget = Files.createTempFile("oba_", ".csv");
      _log.info("Temp file : " + csvTarget);
      csvTarget.toFile().deleteOnExit();
    } catch (IOException e) {
      _log.error("Exception trying to create temp .csv file");
      e.printStackTrace();
    }

    // Copy file
    try {
      Files.copy(csvDataFile.toPath(), csvTarget, StandardCopyOption.REPLACE_EXISTING);
    } catch (Exception e) {
      _log.info(e.getMessage());
    }
    return csvTarget;
  }
 
 public List<BundleValidateQuery> buildQueries(List<ParsedBundleValidationCheck> parsedChecks, String envURI) {
    List<BundleValidateQuery> queries = new ArrayList<BundleValidateQuery>();
    String defaultAgency = getDefaultAgency();
    for (ParsedBundleValidationCheck check : parsedChecks) {
      BundleValidateQuery validationQuery = new BundleValidateQuery();
      String uri = check.getURI();
      if (!uri.startsWith("where") && !uri.startsWith("search", 1)
          && !uri.startsWith("n/a")) continue;
      String specificTest = check.getSpecificTest();
      String query = envURI + "/api/";
      if (specificTest.equals(TEST_SCHEDULE) || specificTest.equals(TEST_SCHEDULE_DATE)
          || specificTest.equals(TEST_SATURDAY_SCHEDULE) || specificTest.equals(TEST_SUNDAY_SCHEDULE)) {
        String stop = getField(ID_FLD, uri);
        String date = "";
        if (specificTest.equals(TEST_SCHEDULE_DATE)) {
          date = DATE_FLD + getField(DATE_FLD, uri);
          date += "&";
        }
        if (specificTest.equals(TEST_SATURDAY_SCHEDULE)) {
          date = DATE_FLD + getNextDayOfWeek(Calendar.SATURDAY);
          date += "&";
        }
        if (specificTest.equals(TEST_SUNDAY_SCHEDULE)) {
          date = DATE_FLD + getNextDayOfWeek(Calendar.SUNDAY);
          date += "&";
        }
        query += "where/schedule-for-stop/" + stop + ".json?" + date + "key=mykey";
      } else if (specificTest.equals(TEST_RT)) {
        query = envURI + "/siri/";
        String stop = getField(ID_FLD, uri);
        query += "stop-monitoring?key=mykey&MonitoringRef=" + stop + "&type=json";
      } else if (specificTest.equals(TEST_ROUTE) || specificTest.equals(TEST_ROUTE_REVISION)) {
        String route = getField(ROUTE_FLD, uri);
        if (route.length() == 0) {
          route = getField(ROUTE_FLD_2, uri);
        }
        query += "where/route/" + route + ".json?key=mykey";
      } else if (specificTest.equals(TEST_ROUTE_SEARCH) || specificTest.equals(TEST_DELETED_ROUTE_SEARCH)) {
        String route = defaultAgency;
        query += "where/routes-for-agency/" + route + ".json?key=mykey";
      } else if (specificTest.equals(TEST_EXPRESS_INDICATOR)) {
        String stop = getField(ID_FLD, uri);
        query += "where/arrivals-and-departures-for-stop/" + stop + ".json?key=mykey";
      }
      validationQuery.setLinenum(check.getLinenum());
      validationQuery.setSpecificTest(check.getSpecificTest());
      validationQuery.setRouteOrStop(check.getRouteOrStop());
      validationQuery.setQuery(query);
      queries.add(validationQuery);      
    }
    return queries;
  }
  
  public String getQueryResult(String queryString) {
    String result = "";
    try {
      URL apiURL = new URL(queryString);
      BufferedReader br = new BufferedReader(new InputStreamReader(apiURL.openStream()));
      String nextLine = "";
      while (null != (nextLine = br.readLine())) {
        result = nextLine;
      }
    } catch (MalformedURLException e) {
      _log.error("Exception getting QueryResults");
      e.printStackTrace();
    } catch (IOException e) {
      _log.error("Exception getting QueryResults");
      e.printStackTrace();
    }
    return result;
  }
  
  public List<BundleValidationCheckResult> checkResults(List<BundleValidateQuery> queryResults) {
    List<BundleValidationCheckResult> testResults = new ArrayList<BundleValidationCheckResult>();
    int linenum=1;
    for (BundleValidateQuery query : queryResults) {
      BundleValidationCheckResult checkResult = new BundleValidationCheckResult();
      String test = query.getSpecificTest();
      String result = query.getQueryResult();
      if (result == null || result.length() == 0) continue;
      if (test.equals(TEST_SCHEDULE) || test.equals(TEST_SCHEDULE_DATE)) {
        if (result.contains(CODE_200) && 
            (result.contains("arrivalEnabled") || result.contains("departureEnabled"))) {
          checkResult.setTestStatus(PASS);
          checkResult.setTestResult(FOUND_SCHEDULE_ENTRIES);
        } else {
          checkResult.setTestStatus(FAIL);
          checkResult.setTestResult(DID_NOT_FIND_SCHEDULE_ENTRIES);
        }
      } else if (test.equals(TEST_SATURDAY_SCHEDULE) || test.equals(TEST_SUNDAY_SCHEDULE)) {
        if (result.contains(CODE_200) && 
            (result.contains("arrivalEnabled") || result.contains("departureEnabled"))) {
          checkResult.setTestStatus(PASS);
          checkResult.setTestResult(FOUND_SCHEDULE_ENTRIES);
        } else {
          checkResult.setTestStatus(FAIL);
          checkResult.setTestResult(DID_NOT_FIND_SCHEDULE_ENTRIES);
        }
      } else if (test.equals(TEST_RT)) {
        if (result.contains("ExpectedArrival") || result.contains("ExpectedDeparture")) {
          checkResult.setTestStatus(PASS);
          checkResult.setTestResult(FOUND_REALTIME_INFO);
        } else {
          checkResult.setTestStatus(FAIL);
          checkResult.setTestResult(DID_NOT_FIND_REALTIME_INFO);
        }
      } else if (test.equals(TEST_ROUTE) || test.equals(TEST_ROUTE_SEARCH) 
          || test.equals(TEST_ROUTE_REVISION) || test.equals(TEST_EXPRESS_INDICATOR)) {
        result = result.toLowerCase();
        String route = query.getRouteOrStop();
        route = route.toLowerCase();
        if (result.contains(CODE_200) && 
            (result.contains(SHORT_NAME + route) || result.contains(LONG_NAME + route))) {
          checkResult.setTestStatus(PASS);
          checkResult.setTestResult(FOUND_ROUTE_INFO);
        } else {
          checkResult.setTestStatus(FAIL);
          checkResult.setTestResult(DID_NOT_FIND_ROUTE_INFO + route);
        }       
      }  else if (test.equals(TEST_DELETED_ROUTE_SEARCH)) {
        result = result.toLowerCase();
        String route = query.getRouteOrStop();
        route = route.toLowerCase();
        if (result.contains(CODE_200) && 
            (!result.contains(SHORT_NAME + route) && !result.contains(LONG_NAME + route))) {
          checkResult.setTestStatus(PASS);
          checkResult.setTestResult(FOUND_ROUTE_INFO);
        } else {
          checkResult.setTestStatus(FAIL);
          checkResult.setTestResult(DID_NOT_FIND_ROUTE_INFO + route);
        }       
      }
      checkResult.setLinenum(linenum);
      checkResult.setCsvLinenum(query.getLinenum());
      checkResult.setSpecificTest(test);
      checkResult.setTestQuery(query.getQuery());
      testResults.add(checkResult);
      ++linenum;
    }
    return testResults;
  }
  
  public String formatResults(String queryResults) {
    return "";
  }
  
  /* Private methods */
  
  private String getTargetURI(String checkEnvironment) {
    String targetURI = "";
    
    try {
      List<Map<String, String>> components = _configurationServiceClient.getItems("config");
      if (components == null) {
        _log.info("getItems call failed");
      }
      for (Map<String, String> component: components) {
        if (checkEnvironment.equals("staging") && "apiStaging".equals(component.get("key"))) {
          targetURI = component.get("value");
        } else if (checkEnvironment.equals("prod") && "apiProd".equals(component.get("key"))) {
          targetURI = component.get("value");
        }
      }
    } catch (Exception e) {
      _log.error("Exception while trying to get environment host");
      e.printStackTrace();
    }
    
    return targetURI;
  }
  
  private String getDefaultAgency() {
    String defaultAgency = "";
    
    try {
      List<Map<String, String>> components = _configurationServiceClient.getItems("config");
      if (components == null) {
        _log.info("getItems call failed");
      }
      for (Map<String, String> component: components) {
        if ("defaultAgency".equals(component.get("key"))) {
          defaultAgency = component.get("value");
        }
      }
    } catch (Exception e) {
      _log.error("Exception while trying to get default agency");
      e.printStackTrace();
    }
    return defaultAgency;
  }

  
  private String getField(String field_id, String uri) {
    String field = "";
    int idx = uri.indexOf(field_id);
    if (idx != -1) {
      int idx2 = uri.indexOf("&", idx+field_id.length());
      if (idx2 == -1) {
        idx2 = uri.indexOf(")", idx+field_id.length());
      }
      if (idx2 == -1) {
        idx2 = uri.indexOf("%29", idx+field_id.length());
      }
      if (idx2 == -1) {
        field = uri.substring(idx+field_id.length());
      } else {
        field = uri.substring(idx+field_id.length(), idx2);
      }
    }
    return field;
  }
  private static String getNextDayOfWeek(int dayOfWeek) {
    // Get next specified day of week
    Calendar c = Calendar.getInstance();
    c.set(Calendar.DAY_OF_WEEK, dayOfWeek);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    return df.format(c.getTime());
    }  
}
