package org.onebusaway.nyc.presentation.impl.realtime;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.onebusaway.nyc.siri.support.SiriDistanceExtension;
import org.onebusaway.util.services.configuration.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PresentationServiceImplTest {

  private final double FEET_TO_METERS = 3.2808399;
  
  @Mock
  private ConfigurationService configService;

  @InjectMocks
  private PresentationServiceImpl service;
  
  @Before
  public void setup() throws Exception {
    
    when(configService.getConfigurationValueAsInteger("display.atStopThresholdInFeet", 100))
      .thenReturn(100);

    when(configService.getConfigurationValueAsInteger("display.approachingThresholdInFeet", 500))
    .thenReturn(500);

    when(configService.getConfigurationValueAsInteger("display.distanceAsStopsTresholdInFeet", 2640))
    .thenReturn(2640);

    when(configService.getConfigurationValueAsInteger("display.distanceAsStopsThresholdInStops", 3))
    .thenReturn(3);

    when(configService.getConfigurationValueAsInteger("display.distanceAsStopsMaximumThresholdInFeet", 2640))
    .thenReturn(2640);

  }

  @Test
  public void testAtStop() throws Exception {
    SiriDistanceExtension distances = new SiriDistanceExtension();
    distances.setDistanceFromCall(10.0 /FEET_TO_METERS);
    distances.setStopsFromCall(0);
    service.refreshCache();
    assertEquals(service.getPresentableDistance(distances), "at stop");
  }
  
  @Test
  public void testAtStop2() throws Exception {
    SiriDistanceExtension distances = new SiriDistanceExtension();
    distances.setDistanceFromCall(10.0 /FEET_TO_METERS);
    distances.setStopsFromCall(1);
    service.refreshCache();
    assertEquals(service.getPresentableDistance(distances), "at stop");
  }

  @Test
  public void testApproaching() throws Exception {
    SiriDistanceExtension distances = new SiriDistanceExtension();
    distances.setDistanceFromCall(200.0 /FEET_TO_METERS);
    distances.setStopsFromCall(0);
    service.refreshCache();
    assertEquals(service.getPresentableDistance(distances), "approaching");
  }
  
  @Test
  public void testApproaching2() throws Exception {
    SiriDistanceExtension distances = new SiriDistanceExtension();
    distances.setDistanceFromCall(100.0 /FEET_TO_METERS);
    distances.setStopsFromCall(2);
    service.refreshCache();
    assertEquals(service.getPresentableDistance(distances), "approaching");
  }

  @Test
  public void testExpressBusCase() throws Exception {
    SiriDistanceExtension distances = new SiriDistanceExtension();
    distances.setDistanceFromCall(5280.0 /FEET_TO_METERS);
    distances.setStopsFromCall(0);
    service.refreshCache();
    assertEquals(service.getPresentableDistance(distances), "1.0 miles away");
  }

  @Test
  public void testMilesCaseFail() throws Exception {
    SiriDistanceExtension distances = new SiriDistanceExtension();
    distances.setDistanceFromCall(5280.0 /FEET_TO_METERS);
    distances.setStopsFromCall(4);
    service.refreshCache();
    assertEquals(service.getPresentableDistance(distances), "1.0 miles away");
  }

  @Test
  public void testStopsCase() throws Exception {
    SiriDistanceExtension distances = new SiriDistanceExtension();
    distances.setDistanceFromCall((5280.0 * .25) /FEET_TO_METERS);
    distances.setStopsFromCall(2);
    service.refreshCache();
    assertEquals(service.getPresentableDistance(distances), "2 stops away");
  }

}
