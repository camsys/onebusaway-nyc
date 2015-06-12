  package org.onebusaway.nyc.webapp.actions.api.siri;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyMapOf;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

  import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.siri.SiriXmlSerializerV2;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.webapp.actions.api.siri.impl.RealtimeServiceV2Impl;
import org.onebusaway.nyc.webapp.actions.api.siri.impl.SiriSupportV2.Filters;
import org.onebusaway.nyc.webapp.actions.api.siri.model.DetailLevel;
import org.onebusaway.transit_data.model.AgencyBean;
import org.onebusaway.transit_data.model.NameBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopGroupBean;
import org.onebusaway.transit_data.model.StopGroupingBean;
import org.onebusaway.transit_data.model.StopsForRouteBean;
import org.onebusaway.transit_data.model.RouteBean.Builder;

import uk.org.siri.siri_2.DirectionRefStructure;
import uk.org.siri.siri_2.LineRefStructure;
import uk.org.siri.siri_2.LocationStructure;
import uk.org.siri.siri_2.AnnotatedStopPointStructure;
import uk.org.siri.siri_2.LineDirectionStructure;
import uk.org.siri.siri_2.NaturalLanguageStringStructure;
import uk.org.siri.siri_2.StopPointRefStructure;

@RunWith(MockitoJUnitRunner.class)
public class StopPointsActionTest {

    private static final long serialVersionUID = 1L;

    @Mock
    private RealtimeServiceV2Impl realtimeService;
    
    @Mock
    private NycTransitDataService transitDataService;
    
    @Mock
    private ConfigurationService configurationService;
    
    @InjectMocks
    private StopPointsV2Action action;

    @Mock
    HttpServletRequest request;
    
    @Mock
    HttpServletResponse servletResponse;
    
    RouteBean routeBean;
    List<RouteBean> routes;
    StopBean stopBean;
    List<StopBean> stops;
    StopGroupBean stopGroup;
    NameBean stopGroupName;
    List<String> stopIds;
    List<StopGroupBean> stopGroups;
    StopGroupingBean stopGrouping;
    List<StopGroupingBean> stopGroupings;
    StopsForRouteBean stopsForRouteBean;
    
    
    @Before
    public void initialize() throws Exception{
    	
      // Agencies
      Map<String, List<CoordinateBounds>> agencies = new  HashMap<String, List<CoordinateBounds>>();
      agencies.put("MTA NYCT", new ArrayList<CoordinateBounds>(Arrays.asList(new CoordinateBounds(40.502983,-74.252014,40.912376,-73.701385))));
      agencies.put("MTA", new ArrayList<CoordinateBounds>(Arrays.asList(new CoordinateBounds(0.0,0.0,0.0,0.0))));
      agencies.put("MTABC", new ArrayList<CoordinateBounds>(Arrays.asList(new CoordinateBounds(40.566148,-74.016343,40.933637,-73.701942))));
    			
  	  // Route Bean
  	  Builder routeBuilder = RouteBean.builder();
  	  routeBuilder.setAgency(new AgencyBean());
  	  routeBuilder.setId("MTA NYCT_M102");
  	  routeBean = routeBuilder.create();
  	  
  	  // Route Bean List
  	  routes = new ArrayList<RouteBean>(1);
  	  routes.add(routeBean);
  	  
  	  //Stop Bean
  	  stopBean = new StopBean();
  	  stopBean.setId("MTA_400062");
  	  stopBean.setName("MALCOLM X BL/W 139 ST");
  	  stopBean.setLon(-73.938614000000001169610186479985713958740234375);
  	  stopBean.setLat(40.8169400000000024419932742603123188018798828125);
  	  stopBean.setRoutes(routes);
  	  
  	  //Stop Bean List
  	  stops = new ArrayList<StopBean>(1);
  	  stops.add(stopBean);
  	  
  	  //Stop Group
  	  stopIds = new ArrayList<String>(1);
  	  stopIds.add(stopBean.getId());
  	  stopGroupName = new NameBean("destination", "Destination");
  	  
  	  stopGroup = new StopGroupBean();
  	  stopGroup.setId("0");
  	  stopGroup.setStopIds(stopIds);
  	  stopGroup.setName(stopGroupName);
  	  
  	  //Stop Group List
  	  stopGroups = new ArrayList<StopGroupBean>(1); 
  	  stopGroups.add(stopGroup);
  	  
  	  //Stop Grouping
  	  stopGrouping = new StopGroupingBean();
  	  stopGrouping.setStopGroups(stopGroups);
  	  
  	  //Stop Grouping List
  	  List<StopGroupingBean> stopGroupings = new ArrayList<StopGroupingBean>(1);
  	  stopGroupings.add(stopGrouping);
  	  
  	  //Stops For Route
  	  stopsForRouteBean =  new StopsForRouteBean();
  	  stopsForRouteBean.setRoute(routeBean);
  	  stopsForRouteBean.setStopGroupings(stopGroupings);
  	  stopsForRouteBean.setStops(stops);
  	  

	  //LineDirectionStructure
	  LineDirectionStructure lds = new LineDirectionStructure();
	  DirectionRefStructure drs = new DirectionRefStructure();
	  LineRefStructure lrs = new LineRefStructure();
	    
	  lds.setDirectionRef(drs);
	  lds.setLineRef(lrs);
	  drs.setValue("0");
	  lrs.setValue("MTA NYCT_M102");
	  
	  //Location Structure
	  LocationStructure ls =  new LocationStructure();
	  BigDecimal lat = new BigDecimal(40.8169400000000024419932742603123188018798828125);
	  BigDecimal lon = new BigDecimal(-73.938614000000001169610186479985713958740234375);
	   
	  ls.setLongitude(lon.setScale(6, BigDecimal.ROUND_HALF_DOWN));
	  ls.setLatitude(lat.setScale(6, BigDecimal.ROUND_HALF_DOWN));
	  
	  //StopNames
	  NaturalLanguageStringStructure stopName = new NaturalLanguageStringStructure();
	  stopName.setValue("MALCOLM X BL/W 139 ST");
	  List<NaturalLanguageStringStructure> stopNames = new ArrayList<NaturalLanguageStringStructure>();
	  stopNames.add(stopName);
	  
	  //StopPointRef
	  StopPointRefStructure stopPointRef = new StopPointRefStructure();
	  stopPointRef.setValue("MTA_400062");
	  
	  //Monitored
	  Boolean monitored = true; 
	  
	  //AnnotatedStopPointStructure
	  AnnotatedStopPointStructure mockStopPoint = new AnnotatedStopPointStructure();
	  mockStopPoint.setLines(new AnnotatedStopPointStructure.Lines());
	  mockStopPoint.getLines().getLineRefOrLineDirection().add(lds);
	  mockStopPoint.setLocation(ls);
	  mockStopPoint.getStopName().add(stopName);
	  mockStopPoint.setStopPointRef(stopPointRef);
	  mockStopPoint.setMonitored(monitored);
	    
	    
	  List<AnnotatedStopPointStructure> mockStopPoints = new ArrayList<AnnotatedStopPointStructure>(1);
	  mockStopPoints.add(mockStopPoint);
	    
	  Map<Boolean, List<AnnotatedStopPointStructure>> annotatedStopPointMap = new HashMap<Boolean, List<AnnotatedStopPointStructure>>();
	  annotatedStopPointMap.put(true, mockStopPoints);

	  when(realtimeService.getAnnotatedStopPointStructures(anyListOf(String.class), anyListOf(AgencyAndId.class), any(DetailLevel.class), anyLong(), anyMapOf(Filters.class, String.class))).thenReturn(annotatedStopPointMap);
	    
	  // XML Serializer
	  SiriXmlSerializerV2 serializer = new SiriXmlSerializerV2();
	  when(realtimeService.getSiriXmlSerializer()).thenReturn(serializer );
	  

	  //Print Writer
	  PrintWriter nothingPrintWriter = new PrintWriter(new OutputStream() {
	        @Override
	        public void write(int b) throws IOException {
	          // Do nothing
	        }
	  });
  	  when(servletResponse.getWriter()).thenReturn(nothingPrintWriter);
	  
	  when(transitDataService.getRouteForId("MTA NYCT_M102")).thenReturn(routeBean);
	  when(transitDataService.getStopsForRoute("MTA NYCT_M102")).thenReturn(stopsForRouteBean);
	  when(transitDataService.stopHasUpcomingScheduledService(anyString(), anyLong(), anyString(), anyString(), anyString())).thenReturn(true);
	  when(transitDataService.getAgencyIdsWithCoverageArea()).thenReturn(agencies);
	  
    }
    
    
    @Test
    public void testLineRef() throws Exception {
    	
    	Map<String, String[]> parameters = new HashMap<String, String[]>();
    	parameters.put("LineRef", new String[]{"M102"});
    	parameters.put("BoundingBox", new String[]{"40.799921","-73.942596","40.794755","-73.940301"});
    	
    	when(request.getParameterMap()).thenReturn(parameters);
	    //when(request.getParameter(eq("OperatorRef"))).thenReturn("MTA NYCT");
	    //when(request.getParameter(eq("StopPointsDetailLevel"))).thenReturn("calls");
	  
	    action.setServletRequest(request);
	    action.setServletResponse(servletResponse);
	    action.execute();
	    
	    String monitoring = action.getStopPoints();
	    System.out.println(monitoring);
	    
	    assertTrue("Result XML does not match expected", monitoring.matches("(?s).*<StopPointsDelivery><ResponseTimestamp>.+</ResponseTimestamp><ValidUntil>.+</ValidUntil><AnnotatedStopPointRef><StopPointRef>.+</StopPointRef><Monitored>true</Monitored><StopName>.+</StopName><Lines><LineDirection><LineRef>.+</LineRef><DirectionRef>(0|1)</DirectionRef></LineDirection></Lines><Location><Longitude>\\-[0-9]{1,2}?\\.[0-9]+</Longitude><Latitude>[0-9]{1,2}?\\.[0-9]+</Latitude></Location></AnnotatedStopPointRef><Extensions><UpcomingScheduledService>true</UpcomingScheduledService></Extensions></StopPointsDelivery></Siri>.*"));
	}
    
    
    @Test
    public void testDetailLevelCase() throws Exception {
    	Map<String, String[]> parameters = new HashMap<String, String[]>();
    	parameters.put("LineRef", new String[]{"M102"});
    	parameters.put("StopPointsDetailLevel", new String[]{"Calls"});
    	
    	when(request.getParameterMap()).thenReturn(parameters);

	    action.setServletRequest(request);
	    action.setServletResponse(servletResponse);
	    action.execute();
	    
	    String monitoring = action.getStopPoints();
	    System.out.println(monitoring);
	    
	    assertTrue("Result XML does not match expected", monitoring.matches("(?s).*<StopPointsDelivery><ResponseTimestamp>.+</ResponseTimestamp><ValidUntil>.+</ValidUntil><AnnotatedStopPointRef><StopPointRef>.+</StopPointRef><Monitored>true</Monitored><StopName>.+</StopName><Lines><LineDirection><LineRef>.+</LineRef><DirectionRef>(0|1)</DirectionRef></LineDirection></Lines><Location><Longitude>.+</Longitude><Latitude>.+</Latitude></Location></AnnotatedStopPointRef><Extensions><UpcomingScheduledService>true</UpcomingScheduledService></Extensions></StopPointsDelivery></Siri>.*"));
    }
    
}