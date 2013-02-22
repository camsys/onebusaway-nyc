package org.onebusaway.nyc.sms.actions;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.onebusaway.nyc.presentation.service.realtime.PresentationService;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.sms.actions.model.RouteResult;
import org.onebusaway.nyc.sms.actions.model.StopResult;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.siri.SiriDistanceExtension;
import org.onebusaway.nyc.transit_data_federation.siri.SiriExtensionWrapper;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.transit_data.model.NameBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.RouteBean.Builder;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopGroupBean;
import org.onebusaway.transit_data.model.StopGroupingBean;
import org.onebusaway.transit_data.model.StopsForRouteBean;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;

import uk.org.siri.siri.DirectionRefStructure;
import uk.org.siri.siri.ExtensionsStructure;
import uk.org.siri.siri.LineRefStructure;
import uk.org.siri.siri.MonitoredCallStructure;
import uk.org.siri.siri.MonitoredStopVisitStructure;
import uk.org.siri.siri.MonitoredVehicleJourneyStructure;

@RunWith(MockitoJUnitRunner.class)
public class SearchResultFactoryImplTest {

  private static final String[] STRINGS_FOR_LONG_DESCRIPTION = new String[] {
    "A really long string that has newlines in it.",
    "The second line of a really long string that has newlines in it.",
    "The third line of a really long string that has newlines in it.",
    "The fourth line of a really long string that has newlines in it.",
    "And now we'll repeat:",
    "A really long string that has newlines in it.",
    "The second line of a really long string that has newlines in it.",
    "The third line of a really long string that has newlines in it.",
    "The fourth line of a really long string that has newlines in it."
    };
  private static final String TEST_DESCRIPTION = "Test description";
  private static final String TEST_DESCRIPTION2 = "Test description 2";
  private static final String TEST_SUMMARY = "Test summary";
  private static final String ROUTE_ID = "route id";
  private static final String TEST_STOP_ID = "test stop id";
  private static final String TEST_PRESENTABLE_DISTANCE = "test presentable distance";
  private static final String TEST_LONG_DESCRIPTION = StringUtils.join(STRINGS_FOR_LONG_DESCRIPTION, "\n");

  @Mock
  private ConfigurationService _configurationService;

  @Mock
  private RealtimeService _realtimeService;

  @Mock
  private NycTransitDataService _nycTransitDataService;

  // getRouteResult tests

  @Test
  public void testGetRouteResultServiceAlertWithNoDescriptionsOrSummaries() {
    RouteResult result = runGetRouteResult(createServiceAlerts(new String[] {},
        new String[] {}));
    List<NaturalLanguageStringBean> alerts = result.getDirections().get(0).getSerivceAlerts();
    assertEquals(1, alerts.size());
    assertEquals("(no description)", alerts.get(0).getValue());
  }

  @Test
  public void testGetRouteResultServiceAlertWithDescriptionsOnly() {
    RouteResult result = runGetRouteResult(createServiceAlerts(new String[] {
        TEST_DESCRIPTION, TEST_DESCRIPTION2}, new String[] {TEST_SUMMARY}));
    List<NaturalLanguageStringBean> alerts = result.getDirections().get(0).getSerivceAlerts();
    assertEquals(2, alerts.size());
    assertEquals(TEST_DESCRIPTION, alerts.get(0).getValue());
    assertEquals(TEST_DESCRIPTION2, alerts.get(1).getValue());
  }

  @Test
  public void testGetRouteResultServiceAlertWithSummariesOnly() {
    RouteResult result = runGetRouteResult(createServiceAlerts(new String[] {},
        new String[] {TEST_SUMMARY}));
    List<NaturalLanguageStringBean> alerts = result.getDirections().get(0).getSerivceAlerts();
    assertEquals(1, alerts.size());
    assertEquals(TEST_SUMMARY, alerts.get(0).getValue());
  }

  @Test
  public void testGetRouteResultServiceAlertWithDescriptionsAndSummaries() {
    RouteResult result = runGetRouteResult(createServiceAlerts(
        new String[] {TEST_DESCRIPTION}, new String[] {TEST_SUMMARY}));
    List<NaturalLanguageStringBean> alerts = result.getDirections().get(0).getSerivceAlerts();
    assertEquals(1, alerts.size());
    assertEquals(TEST_DESCRIPTION, alerts.get(0).getValue());
  }

  // getStopResult tests

  @Test
  public void testGetStopResultServiceAlertWithNoDescriptionsOrSummaries() {
    StopResult result = runGetStopResult(createServiceAlerts(new String[] {},
        new String[] {}));
    List<NaturalLanguageStringBean> alerts = result.getRoutesAvailable().get(0).getDirections().get(
        0).getSerivceAlerts();
    assertEquals(1, alerts.size());
    assertEquals("(no description)", alerts.get(0).getValue());
  }

  @Test
  public void testGetStopResultServiceAlertWithDescriptionsOnly() {
    StopResult result = runGetStopResult(createServiceAlerts(new String[] {
        TEST_DESCRIPTION, TEST_DESCRIPTION2}, new String[] {TEST_SUMMARY}));
    List<NaturalLanguageStringBean> alerts = result.getRoutesAvailable().get(0).getDirections().get(
        0).getSerivceAlerts();
    assertEquals(2, alerts.size());
    assertEquals(TEST_DESCRIPTION, alerts.get(0).getValue());
    assertEquals(TEST_DESCRIPTION2, alerts.get(1).getValue());
  }

  @Test
  public void testGetStopResultServiceAlertWithReallLongDescription() {
    StopResult result = runGetStopResult(createServiceAlerts(new String[] {
        TEST_LONG_DESCRIPTION}, new String[] {TEST_SUMMARY}));
    List<NaturalLanguageStringBean> alerts = result.getRoutesAvailable().get(0).getDirections().get(
        0).getSerivceAlerts();
    assertEquals(1, alerts.size());
    assertEquals(TEST_LONG_DESCRIPTION, alerts.get(0).getValue());
  }

  @Test
  public void testGetStopResultServiceAlertWithSummariesOnly() {
    StopResult result = runGetStopResult(createServiceAlerts(new String[] {},
        new String[] {TEST_SUMMARY}));
    List<NaturalLanguageStringBean> alerts = result.getRoutesAvailable().get(0).getDirections().get(
        0).getSerivceAlerts();
    assertEquals(1, alerts.size());
    assertEquals(TEST_SUMMARY, alerts.get(0).getValue());
  }

  @Test
  public void testGetStopResultServiceAlertWithDescriptionsAndSummaries() {
    StopResult result = runGetStopResult(createServiceAlerts(
        new String[] {TEST_DESCRIPTION}, new String[] {TEST_SUMMARY}));
    List<NaturalLanguageStringBean> alerts = result.getRoutesAvailable().get(0).getDirections().get(
        0).getSerivceAlerts();
    assertEquals(1, alerts.size());
    assertEquals(TEST_DESCRIPTION, alerts.get(0).getValue());
  }

  // Support methods

  private StopResult runGetStopResult(List<ServiceAlertBean> serviceAlerts) {
    StopBean stopBean = setupStops();

    List<MonitoredStopVisitStructure> monitoredStopVisits = new ArrayList<MonitoredStopVisitStructure>();
    MonitoredStopVisitStructure monitoredStopVisitStructure = mock(MonitoredStopVisitStructure.class);
    monitoredStopVisits.add(monitoredStopVisitStructure);

    MonitoredVehicleJourneyStructure monVehJourney = mock(MonitoredVehicleJourneyStructure.class);
    when(monitoredStopVisitStructure.getMonitoredVehicleJourney()).thenReturn(
        monVehJourney);
    when(monitoredStopVisitStructure.getRecordedAtTime()).thenReturn(new Date());

    LineRefStructure lineRefStructure = mock(LineRefStructure.class);
    when(monVehJourney.getLineRef()).thenReturn(lineRefStructure);
    when(lineRefStructure.getValue()).thenReturn(ROUTE_ID);

    DirectionRefStructure directionRef = mock(DirectionRefStructure.class);
    when(monVehJourney.getDirectionRef()).thenReturn(directionRef);
    when(directionRef.getValue()).thenReturn(TEST_STOP_ID);

    MonitoredCallStructure monCall = mock(MonitoredCallStructure.class);
    ExtensionsStructure extensions = mock(ExtensionsStructure.class);
    SiriExtensionWrapper siriExtensionWrapper = mock(SiriExtensionWrapper.class);
    SiriDistanceExtension distances = mock(SiriDistanceExtension.class);
    when(distances.getPresentableDistance()).thenReturn(
        TEST_PRESENTABLE_DISTANCE);
    when(siriExtensionWrapper.getDistances()).thenReturn(distances);
    when(extensions.getAny()).thenReturn(siriExtensionWrapper);
    when(monCall.getExtensions()).thenReturn(extensions);
    when(monVehJourney.getMonitoredCall()).thenReturn(monCall);

    when(_realtimeService.getMonitoredStopVisitsForStop(TEST_STOP_ID, 0)).thenReturn(
        monitoredStopVisits);

    when(
        _realtimeService.getServiceAlertsForRouteAndDirection(ROUTE_ID,
            TEST_STOP_ID)).thenReturn(serviceAlerts);

    PresentationService presentationService = mock(PresentationService.class);
    SiriDistanceExtension distanceExtension = mock(SiriDistanceExtension.class);
    when(
        presentationService.getPresentableDistance(distanceExtension,
            "arriving", "stop", "stops", "mi.", "mi.", "")).thenReturn(
        TEST_PRESENTABLE_DISTANCE);
    when(_realtimeService.getPresentationService()).thenReturn(
        presentationService);
    SearchResultFactoryImpl srf = new SearchResultFactoryImpl(
        _nycTransitDataService, _realtimeService, _configurationService);
    Set<RouteBean> routeIdFilter = new HashSet<RouteBean>();
    StopResult result = (StopResult) srf.getStopResult(stopBean, routeIdFilter);
    return result;
  }

  private RouteResult runGetRouteResult(List<ServiceAlertBean> serviceAlerts) {
    setupStops();
    when(
        _realtimeService.getServiceAlertsForRouteAndDirection(anyString(),
            anyString())).thenReturn(serviceAlerts);
    SearchResultFactoryImpl srf = new SearchResultFactoryImpl(
        _nycTransitDataService, _realtimeService, _configurationService);
    RouteResult result = (RouteResult) srf.getRouteResult(createRouteBean());
    return result;
  }

  private StopBean setupStops() {
    StopsForRouteBean stopsForRouteBean = mock(StopsForRouteBean.class);
    List<StopGroupingBean> stopGroupingBeans = new ArrayList<StopGroupingBean>();
    when(stopsForRouteBean.getStopGroupings()).thenReturn(stopGroupingBeans);

    StopGroupingBean stopGroupingBean = mock(StopGroupingBean.class);
    stopGroupingBeans.add(stopGroupingBean);

    List<StopGroupBean> stopGroups = new ArrayList<StopGroupBean>();
    StopGroupBean stopGroupBean = mock(StopGroupBean.class);
    stopGroups.add(stopGroupBean);
    when(stopGroupingBean.getStopGroups()).thenReturn(stopGroups);

    List<String> stopIds = new ArrayList<String>();
    when(stopGroupBean.getStopIds()).thenReturn(stopIds);
    NameBean nameBean = mock(NameBean.class);
    when(nameBean.getType()).thenReturn("destination");
    when(stopGroupBean.getName()).thenReturn(nameBean);
    List<String> stopGroupBeanStopIds = new ArrayList<String>();
    stopGroupBeanStopIds.add(TEST_STOP_ID);
    when(stopGroupBean.getStopIds()).thenReturn(stopGroupBeanStopIds);
    when(stopGroupBean.getId()).thenReturn(TEST_STOP_ID);

    stopIds.add(TEST_STOP_ID);

    List<RouteBean> routeBeans = new ArrayList<RouteBean>();
    routeBeans.add(createRouteBean());
    StopBean stopBean = mock(StopBean.class);
    when(stopBean.getId()).thenReturn(TEST_STOP_ID);
    when(stopBean.getRoutes()).thenReturn(routeBeans);

    when(_nycTransitDataService.getStopsForRoute(anyString())).thenReturn(
        stopsForRouteBean);
    return stopBean;
  }

  private RouteBean createRouteBean() {
    Builder builder = RouteBean.builder();
    builder.setId(ROUTE_ID);
    RouteBean routeBean = builder.create();
    return routeBean;
  }

  private List<ServiceAlertBean> createServiceAlerts(String[] descriptions,
      String[] summaries) {
    List<ServiceAlertBean> serviceAlerts = new ArrayList<ServiceAlertBean>();
    ServiceAlertBean saBean = new ServiceAlertBean();
    serviceAlerts.add(saBean);
    if (descriptions.length > 0)
      saBean.setDescriptions(createTextList(descriptions));
    if (summaries.length > 0)
      saBean.setSummaries(createTextList(summaries));
    return serviceAlerts;
  }

  private List<NaturalLanguageStringBean> createTextList(String[] texts) {
    List<NaturalLanguageStringBean> textList = new ArrayList<NaturalLanguageStringBean>();
    for (String text : texts) {
      textList.add(new NaturalLanguageStringBean(text, "EN"));
    }
    return textList;
  }

}
