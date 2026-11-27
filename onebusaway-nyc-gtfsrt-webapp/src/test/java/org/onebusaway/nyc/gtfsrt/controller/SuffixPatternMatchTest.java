/**
 * Copyright (C) 2026 Cambridge Systematics, Inc.
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
package org.onebusaway.nyc.gtfsrt.controller;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.transit.realtime.GtfsRealtime.FeedHeader;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.nyc.gtfsrt.service.FeedMessageService;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.impl.predictions.QueuePredictionIntegrationServiceImpl;
import org.onebusaway.nyc.transit_data_federation.impl.bundle.BundleManagementServiceImpl;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * Loads the webapp's real {@code WEB-INF/remoting-servlet.xml} and checks that a feed URL with a
 * {@code .json} suffix reaches the same handler as the plain URL.
 *
 * <p>Why: Spring 5.3 turned suffix pattern matching off by default, so {@code /tripUpdates.json}
 * started returning 404 while QA and prod (older Spring) still answer it (OBANYC-4240). The
 * servlet config turns it back on with {@code <mvc:path-matching suffix-pattern="true"/>}; this
 * test fails if that line is removed. The services the config needs come from a parent context
 * of mocks, standing in for the root context the real webapp loads from data-sources.xml.</p>
 */
public class SuffixPatternMatchTest {

  private XmlWebApplicationContext servletContext;
  private MockMvc mvc;
  private FeedMessage tripUpdates;
  private FeedMessage vehiclePositions;
  private FeedMessage alerts;

  @Before
  public void setUp() {
    tripUpdates = feed(1L);
    vehiclePositions = feed(2L);
    alerts = feed(3L);

    StaticApplicationContext parent = new StaticApplicationContext();
    parent.getBeanFactory().registerSingleton("tripUpdateServiceImpl", feedService(tripUpdates));
    parent.getBeanFactory().registerSingleton("vehicleUpdateServiceImpl", feedService(vehiclePositions));
    parent.getBeanFactory().registerSingleton("serviceAlertServiceImpl", feedService(alerts));
    parent.getBeanFactory().registerSingleton("nycTransitDataServiceImpl", mock(NycTransitDataService.class));
    parent.getBeanFactory().registerSingleton("configurationService", mock(ConfigurationService.class));
    parent.getBeanFactory().registerSingleton("bundleManagementServiceImpl", mock(BundleManagementServiceImpl.class));
    parent.getBeanFactory().registerSingleton("queuePredictionIntegrationServiceImpl",
        mock(QueuePredictionIntegrationServiceImpl.class));
    parent.refresh();

    servletContext = new XmlWebApplicationContext();
    servletContext.setParent(parent);
    servletContext.setServletContext(new MockServletContext());
    servletContext.setConfigLocation("file:src/main/webapp/WEB-INF/remoting-servlet.xml");
    servletContext.refresh();

    mvc = MockMvcBuilders.webAppContextSetup(servletContext).build();
  }

  @After
  public void tearDown() {
    if (servletContext != null) {
      servletContext.close();
    }
  }

  @Test
  public void tripUpdatesWithAndWithoutJsonSuffixReturnTheSameFeed() throws Exception {
    assertSameFeed("/tripUpdates", tripUpdates);
  }

  @Test
  public void vehiclePositionsWithAndWithoutJsonSuffixReturnTheSameFeed() throws Exception {
    assertSameFeed("/vehiclePositions", vehiclePositions);
  }

  @Test
  public void alertsWithAndWithoutJsonSuffixReturnTheSameFeed() throws Exception {
    assertSameFeed("/alerts", alerts);
  }

  /** Both the plain URL and the {@code .json} URL answer 200 with the protobuf bytes of the feed. */
  private void assertSameFeed(String path, FeedMessage expected) throws Exception {
    byte[] plain = mvc.perform(get(path)).andExpect(status().isOk())
        .andReturn().getResponse().getContentAsByteArray();
    byte[] json = mvc.perform(get(path + ".json")).andExpect(status().isOk())
        .andReturn().getResponse().getContentAsByteArray();
    assertArrayEquals(expected.toByteArray(), plain);
    assertArrayEquals(plain, json);
  }

  private static FeedMessage feed(long timestamp) {
    return FeedMessage.newBuilder()
        .setHeader(FeedHeader.newBuilder().setGtfsRealtimeVersion("2.0").setTimestamp(timestamp))
        .build();
  }

  private static FeedMessageService feedService(FeedMessage msg) {
    FeedMessageService service = mock(FeedMessageService.class);
    when(service.getFeedMessage(any())).thenReturn(msg);
    return service;
  }
}
