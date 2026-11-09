/**
 * Copyright (C) 2026 Metropolitan Transportation Authority
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

package org.onebusaway.nyc.sms.actions;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onebusaway.nyc.presentation.impl.realtime.PresentationServiceImpl;
import org.onebusaway.nyc.presentation.model.SearchResultCollection;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.presentation.service.search.SearchResultFactory;
import org.onebusaway.nyc.presentation.service.search.SearchService;
import org.onebusaway.nyc.siri.support.SiriDistanceExtension;
import org.onebusaway.nyc.siri.support.SiriExtensionWrapper;
import org.onebusaway.nyc.siri.support.SiriVehicleFeatures;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.transit_data.model.NameBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopGroupBean;
import org.onebusaway.transit_data.model.StopGroupingBean;
import org.onebusaway.transit_data.model.StopsForRouteBean;

import uk.org.siri.siri.DirectionRefStructure;
import uk.org.siri.siri.ExtensionsStructure;
import uk.org.siri.siri.LineRefStructure;
import uk.org.siri.siri.MonitoredCallStructure;
import uk.org.siri.siri.MonitoredStopVisitStructure;
import uk.org.siri.siri.MonitoredVehicleJourneyStructure;
import uk.org.siri.siri.NaturalLanguageStringStructure;
import uk.org.siri.siri.VehicleRefStructure;

/**
 * Pins the exact SMS reply a rider gets for a stop-code text, from fake
 * realtime data run through the real SMS and presentation formatting code.
 *
 * Why: this is the Java 11 baseline for the Java 21 upgrade (OBANYC-4216).
 * The reply mixes number formatting ("2.5 miles"), a clock time
 * ("sched. dep. 5:13") and fixed wording; a JDK change to locale data or
 * formatting would change the text riders receive without failing any other
 * test.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class IndexActionStopReplyTest {

	private static final String STOP_CODE = "308209";
	private static final String STOP_ID = "MTA_" + STOP_CODE;
	private static final String ROUTE_ID = "MTA NYCT_B63";
	private static final String DIRECTION_ID = "0";

	/**
	 * Answers every ConfigurationService getter with the default value the
	 * caller passed in, as a deployment with no overrides would.
	 */
	private final ConfigurationService _configurationService = mock(
			ConfigurationService.class, invocation -> {
				if (invocation.getMethod().getName().startsWith("getConfigurationValueAs"))
					return invocation.getArgument(1);
				return RETURNS_DEFAULTS.answer(invocation);
			});

	@Mock
	private RealtimeService _realtimeService;

	@Mock
	private SearchService _searchService;

	@Mock
	private NycTransitDataService _nycTransitDataService;

	@InjectMocks
	private PresentationServiceImpl _presentationService;

	@InjectMocks
	private IndexAction _action;

	private TimeZone _savedTimeZone;

	private Locale _savedLocale;

	@Before
	public void setup() throws Exception {
		// pin the zone and locale the servers run with, so the expected text
		// does not depend on the machine running the test
		_savedTimeZone = TimeZone.getDefault();
		_savedLocale = Locale.getDefault();
		TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
		Locale.setDefault(Locale.US);

		// @InjectMocks only sees @Mock fields, so hand over the config mock here
		setField(_presentationService, "_configurationService", _configurationService);
		setField(_action, "_configurationService", _configurationService);
		// Spring calls this @PostConstruct hook; without it every distance
		// threshold stays 0 and all distances read as miles
		_presentationService.start();

		when(_realtimeService.getPresentationService()).thenReturn(_presentationService);
		when(_realtimeService.getServiceAlertsGlobal()).thenReturn(Collections.emptyList());
		when(_realtimeService.getServiceAlertsForRouteAndDirection(anyString(), anyString()))
				.thenReturn(Collections.emptyList());
		when(_nycTransitDataService.getStopsForRoute(ROUTE_ID)).thenReturn(createStopsForRoute());
		when(_nycTransitDataService.stopHasUpcomingScheduledService(any(), anyLong(),
				anyString(), anyString(), anyString())).thenReturn(true);

		StopBean stop = createStop();
		when(_searchService.getSearchResults(eq(STOP_CODE), any(SearchResultFactory.class)))
				.thenAnswer(invocation -> {
					// use the factory IndexAction passes in, so the real
					// SearchResultFactoryImpl builds the stop result
					SearchResultFactory factory = invocation.getArgument(1);
					SearchResultCollection results = new SearchResultCollection();
					results.addMatch(factory.getStopResult(stop, results.getRouteFilter()));
					return results;
				});
	}

	@After
	public void restoreDefaults() {
		TimeZone.setDefault(_savedTimeZone);
		Locale.setDefault(_savedLocale);
	}

	@Test
	public void testStopCodeReplyText() throws Exception {
		long now = System.currentTimeMillis();
		List<MonitoredStopVisitStructure> visits = new ArrayList<MonitoredStopVisitStructure>();
		// 300 m and 1 stop away, with a prediction 5.5 minutes after the report
		visits.add(createVisit("MTA NYCT_7582", now, 300.0, 1,
				new Date(now + 330 * 1000), null, null));
		// at the terminal on layover, departing tomorrow at 5:13 PM
		visits.add(createVisit("MTA NYCT_7583", now, 1500.0, 6, null, "layover",
				tomorrowAt(17, 13)));
		// 4 km out, no prediction
		visits.add(createVisit("MTA NYCT_7584", now, 4000.0, 12, null, null, null));
		when(_realtimeService.getMonitoredStopVisitsForStop(eq(STOP_ID), anyInt(), anyLong(),
				anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(visits);

		_action.setSession(new HashMap<String, Object>());
		_action.initializeSession("test-session");
		_action.setArgs(STOP_CODE);
		_action.execute();

		assertEquals(EXPECTED_REPLY, _action.getResponse());
	}

	/**
	 * Recorded on Java 11. The SMS code asks for distances with an empty
	 * "away" word and appends an empty occupancy, so the miles lines have no
	 * "away" and the last one ends in two spaces. That is the text riders get
	 * today and is part of the baseline.
	 */
	private static final String EXPECTED_REPLY = "Stop 308209\n"
			+ "\n"
			+ "B63: 5 mins., 1 stop away\n"
			+ "B63: 0.9 miles (@term. sched. dep. 5:13)\n"
			+ "B63: 2.5 miles  \n"
			+ "\n"
			+ "Send:\n"
			+ "R for refresh";

	private MonitoredStopVisitStructure createVisit(String vehicleId, long recordedAt,
			double metersFromStop, int stopsFromStop, Date expectedArrival,
			String progressStatus, Date originDeparture) {
		SiriDistanceExtension distances = new SiriDistanceExtension();
		distances.setDistanceFromCall(metersFromStop);
		distances.setStopsFromCall(stopsFromStop);
		// the realtime service fills this in upstream of the SMS code
		distances.setPresentableDistance(_presentationService.getPresentableDistance(distances));

		SiriVehicleFeatures features = new SiriVehicleFeatures();
		features.setStrollerVehicle(false);

		SiriExtensionWrapper wrapper = new SiriExtensionWrapper();
		wrapper.setDistances(distances);
		wrapper.setFeatures(features);

		ExtensionsStructure extensions = new ExtensionsStructure();
		extensions.setAny(wrapper);

		MonitoredCallStructure call = new MonitoredCallStructure();
		call.setExtensions(extensions);
		call.setExpectedArrivalTime(expectedArrival);

		MonitoredVehicleJourneyStructure journey = new MonitoredVehicleJourneyStructure();
		LineRefStructure lineRef = new LineRefStructure();
		lineRef.setValue(ROUTE_ID);
		journey.setLineRef(lineRef);
		DirectionRefStructure directionRef = new DirectionRefStructure();
		directionRef.setValue(DIRECTION_ID);
		journey.setDirectionRef(directionRef);
		VehicleRefStructure vehicleRef = new VehicleRefStructure();
		vehicleRef.setValue(vehicleId);
		journey.setVehicleRef(vehicleRef);
		journey.setMonitoredCall(call);
		journey.setOriginAimedDepartureTime(originDeparture);
		if (progressStatus != null) {
			NaturalLanguageStringStructure status = new NaturalLanguageStringStructure();
			status.setValue(progressStatus);
			journey.setProgressStatus(status);
		}

		MonitoredStopVisitStructure visit = new MonitoredStopVisitStructure();
		visit.setRecordedAtTime(new Date(recordedAt));
		visit.setMonitoredVehicleJourney(journey);
		return visit;
	}

	private StopBean createStop() {
		RouteBean.Builder route = RouteBean.builder();
		route.setId(ROUTE_ID);
		route.setShortName("B63");

		StopBean stop = new StopBean();
		stop.setId(STOP_ID);
		stop.setName("5 AV/UNION ST");
		List<RouteBean> routes = new ArrayList<RouteBean>();
		routes.add(route.create());
		stop.setRoutes(routes);
		return stop;
	}

	private StopsForRouteBean createStopsForRoute() {
		StopGroupBean direction = new StopGroupBean();
		direction.setId(DIRECTION_ID);
		direction.setName(new NameBean("destination", "BAY RIDGE"));
		direction.setStopIds(Collections.singletonList(STOP_ID));

		StopGroupingBean grouping = new StopGroupingBean();
		grouping.setStopGroups(Collections.singletonList(direction));

		StopsForRouteBean stopsForRoute = new StopsForRouteBean();
		stopsForRoute.setStopGroupings(Collections.singletonList(grouping));
		return stopsForRoute;
	}

	private static Date tomorrowAt(int hourOfDay, int minute) {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_MONTH, 1);
		c.set(Calendar.HOUR_OF_DAY, hourOfDay);
		c.set(Calendar.MINUTE, minute);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		return c.getTime();
	}

	private static void setField(Object target, String name, Object value) throws Exception {
		java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
