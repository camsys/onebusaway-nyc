package org.onebusaway.nyc.transit_data_manager.siri;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.onebusaway.transit_data.model.service_alerts.EEffect;
import org.onebusaway.transit_data.model.service_alerts.ESeverity;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.service_alerts.SituationAffectsBean;
import org.onebusaway.transit_data.model.service_alerts.SituationConsequenceBean;
import org.onebusaway.transit_data.model.service_alerts.TimeRangeBean;

public class ServiceAlertRecordTest extends ServiceAlertRecord {

  private static final long serialVersionUID = 1L;
  private String testSummary = "A test summary";
  private String testDescription = "A test description";
  private String testUrl = "A test url";


  @Test
  public void testToJsonListTimeEmpty() {
    List<TimeRangeBean> timeWindows = new ArrayList<TimeRangeBean>();
    assertEquals("[]", toJson(timeWindows ));
  }

  @Test
  public void testToJsonListTimeOne() {
    List<TimeRangeBean> timeWindows = createTestListTimeRangeBean(1322676345, 1322676345+1000);
    assertEquals("[{\"from\":1322676345,\"to\":1322677345}]", toJson(timeWindows ));
  }

  private List<TimeRangeBean> createTestListTimeRangeBean(int from, int to) {
    List<TimeRangeBean> list = new ArrayList<TimeRangeBean>();
    list.add(createTestTimeRangeBean(from, to));
    return list;
  }

  private TimeRangeBean createTestTimeRangeBean(int from, int to) {
    return new TimeRangeBean(from, to);
  }

  @Test
  public void testToJsonListTimeTwo() {
    List<TimeRangeBean> timeWindows = createTestListTimeRangeBean(1322676345, 1322676345+1000);
    timeWindows.add(createTestTimeRangeBean(1322676345+2000, 1322676345+3000) );
    assertEquals("[{\"from\":1322676345,\"to\":1322677345},{\"from\":1322678345,\"to\":1322679345}]", toJson(timeWindows ));
  }

  @Test
  public void testToJsonSummariesOne() {
    List<NaturalLanguageStringBean> summaries = new ArrayList<NaturalLanguageStringBean>();
    summaries.add(new NaturalLanguageStringBean("A test summary", "EN"));
    assertEquals("[{\"value\":\"A test summary\",\"lang\":\"EN\"}]", toJson(summaries));
  }

  @Test
  public void testToJsonSummariesTwo() {
    List<NaturalLanguageStringBean> summaries = new ArrayList<NaturalLanguageStringBean>();
    summaries.add(new NaturalLanguageStringBean("A test summary", "EN"));
    summaries.add(new NaturalLanguageStringBean("A second test summary", "JA"));
    assertEquals("[{\"value\":\"A test summary\",\"lang\":\"EN\"},{\"value\":\"A second test summary\",\"lang\":\"JA\"}]", toJson(summaries));
  }

  @Test
  public void testToJsonAllAffects() {
    List<SituationAffectsBean> allAffects = new ArrayList<SituationAffectsBean>();
    SituationAffectsBean e = new SituationAffectsBean();
    allAffects.add(e );
    e.setRouteId("a route id");
    assertEquals("[{\"route-id\":\"a route id\"}]", toJson(allAffects));
  }

  @Test
  public void testToJsonESeverity() {
    ESeverity severity = ESeverity.NORMAL;
    assertEquals("\"NORMAL\"", toJson(severity));
  }

  
  @Test
  public void testFromRecordToBean() {
    ServiceAlertRecord record = new ServiceAlertRecord();
    
    initializeServiceAlertRecord(testSummary, testDescription, testUrl, record);

    ServiceAlertBean bean = ServiceAlertRecord.toBean(record);

    assertEquals("A test id", bean.getId());
    assertEquals(1234567, bean.getCreationTime());
    assertTrue(compareListTimeRangeBean(createTestListTimeRangeBean(1322676345, 1322676345+1000), bean.getActiveWindows()));
    assertTrue(compareListTimeRangeBean(createTestListTimeRangeBean(1322676346, 1322676346+1000), bean.getPublicationWindows()));
    assertEquals("A test reason", bean.getReason());
    assertEquals(testSummary, bean.getSummaries().get(0).getValue());
    assertEquals(testDescription, bean.getDescriptions().get(0).getValue());
    assertEquals(testUrl, bean.getUrls().get(0).getValue());
    assertEquals("A route id", bean.getAllAffects().get(0).getRouteId());
    assertEquals(EEffect.MODIFIED_SERVICE, bean.getConsequences().get(0).getEffect());
    assertEquals(ESeverity.NORMAL, bean.getSeverity());
  }

  @Test
  public void testFromRecordToBeanAffectsAllOperators() {
    ServiceAlertRecord record = new ServiceAlertRecord();
    
    initializeServiceAlertRecord(testSummary, testDescription, testUrl, record);
    
    record.setAllAffects("[{\"agency-id\":\"__ALL_OPERATORS__\"}]");
    
    ServiceAlertBean bean = ServiceAlertRecord.toBean(record);

    assertEquals("A test id", bean.getId());
    assertEquals(1234567, bean.getCreationTime());
    assertTrue(compareListTimeRangeBean(createTestListTimeRangeBean(1322676345, 1322676345+1000), bean.getActiveWindows()));
    assertTrue(compareListTimeRangeBean(createTestListTimeRangeBean(1322676346, 1322676346+1000), bean.getPublicationWindows()));
    assertEquals("A test reason", bean.getReason());
    assertEquals(testSummary, bean.getSummaries().get(0).getValue());
    assertEquals(testDescription, bean.getDescriptions().get(0).getValue());
    assertEquals(testUrl, bean.getUrls().get(0).getValue());
    assertEquals(null, bean.getAllAffects().get(0).getRouteId());
    assertEquals("__ALL_OPERATORS__", bean.getAllAffects().get(0).getAgencyId());
    assertEquals(EEffect.MODIFIED_SERVICE, bean.getConsequences().get(0).getEffect());
    assertEquals(ESeverity.NORMAL, bean.getSeverity());
  }

  private void initializeServiceAlertRecord(String testSummary, String testDescription, String testUrl, ServiceAlertRecord record) {
    record.setServiceAlertId("A test id");
    record.setCreationTime(1234567);

    record.setActiveWindows("[{\"from\":1322676345,\"to\":1322677345}]");
    record.setPublicationWindows("[{\"from\":1322676346,\"to\":1322677346}]");

    record.setReason("A test reason");
    record.setSummaries(testString(testSummary));

    record.setDescriptions(testString(testDescription));

    record.setUrls(testString(testUrl));
    
    record.setAllAffects("[{\"route-id\":\"A route id\"}]");
    
    List<SituationConsequenceBean> scbl = new ArrayList<SituationConsequenceBean>();
    SituationConsequenceBean scb = new SituationConsequenceBean();
    scb.setEffect(EEffect.MODIFIED_SERVICE);
    scbl.add(scb);
    record.setConsequences(toJson(scbl));
    
    record.setSeverity("\"NORMAL\"");
  }

  
  private boolean compareListTimeRangeBean(
      List<TimeRangeBean> expected,
      List<TimeRangeBean> actual) {
    if (actual.size() != expected.size())
      return false;
    for (int i = 0 ; i < expected.size(); i++) {
      if (actual.get(i).getFrom() != expected.get(i).getFrom())
        return false;
      if (actual.get(i).getTo() != expected.get(i).getTo())
        return false;
    }
    return true;
  }

  private String testString(String t) {
    return "[{\"value\":\"" + t + "\",\"lang\":\"EN\"}]";
  }


}
