package org.onebusaway.nyc.transit_data_federation.impl.nyc;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.stop;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.stopTime;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.trip;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.RunTripEntry;
import org.onebusaway.nyc.transit_data_federation.impl.nyc.RunServiceImpl;
import org.onebusaway.nyc.transit_data_federation.model.nyc.RunData;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.TripEntryImpl;
import org.onebusaway.transit_data_federation.services.ExtendedCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocationService;
import org.onebusaway.transit_data_federation.services.transit_graph.ServiceIdActivation;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;

import com.google.common.collect.Iterables;
import com.google.common.collect.TreeMultimap;

public class RunServiceImplTest {

  private RunServiceImpl _service;

  private TransitGraphDao _transitGraph;

  private ScheduledBlockLocationService _scheduledBlockLocationService;

  private BlockCalendarService _blockCalendarService;

  private TripEntryImpl tripA, tripB, tripC, tripD, tripE, tripF, tripG, tripH, tripI;

  @Before
  public void setup() {

    _service = new RunServiceImpl();

    StopEntryImpl stopA = stop("a", 47.5, -122.5);
    StopEntryImpl stopB = stop("b", 47.6, -122.4);
    StopEntryImpl stopC = stop("c", 47.5, -122.3);

    tripA = trip("tripA", "serviceId");
    tripB = trip("tripB", "serviceId");
    tripC = trip("tripC", "serviceId");
    tripD = trip("tripD", "serviceId");
    tripE = trip("tripE", "serviceId");
    tripF = trip("tripF", "serviceId");
    tripG = trip("tripG", "serviceId");
    tripH = trip("tripH", "serviceId");
    tripI = trip("tripI", "serviceId");

    stopTime(0, stopA, tripA, 30, 90, 0);
    stopTime(1, stopB, tripA, 120, 120, 100);
    stopTime(2, stopC, tripA, 180, 210, 200);

    stopTime(3, stopC, tripB, 240, 240, 300);
    stopTime(4, stopB, tripB, 270, 270, 400);
    stopTime(5, stopA, tripB, 300, 300, 500);

    stopTime(6, stopA, tripC, 360, 360, 600);
    stopTime(7, stopB, tripC, 390, 390, 700);
    stopTime(8, stopC, tripC, 420, 420, 800);

    // trip C and D are the same but on different runs
    stopTime(6, stopA, tripD, 360, 360, 600);
    stopTime(7, stopB, tripD, 390, 390, 700);
    stopTime(8, stopC, tripD, 420, 420, 800);

    Map<AgencyAndId, RunData> runDataByTrip = new HashMap<AgencyAndId, RunData>();
    runDataByTrip.put(tripA.getId(), new RunData("run-1", null, -1));
    runDataByTrip.put(tripB.getId(), new RunData("run-1", "run-2", 270));
    runDataByTrip.put(tripC.getId(), new RunData("run-1", null, -1));
    // don't ask how the driver of run2 gets to StopA
    runDataByTrip.put(tripD.getId(), new RunData("run-2", null, -1));
    
    runDataByTrip.put(tripE.getId(), new RunData("X1-5", null, -1));
    runDataByTrip.put(tripF.getId(), new RunData("X0102-5", null, -1));
    runDataByTrip.put(tripG.getId(), new RunData("B63-5", null, -1));
    runDataByTrip.put(tripH.getId(), new RunData("MISC-75", null, -1));
    runDataByTrip.put(tripI.getId(), new RunData("X103-5", null, -1));

    _service.setRunDataByTrip(runDataByTrip);

    _transitGraph = mock(TransitGraphDao.class);
    _service.setTransitGraph(_transitGraph);

    _scheduledBlockLocationService = mock(ScheduledBlockLocationService.class);
    _service.setScheduledBlockLocationService(_scheduledBlockLocationService);

    _blockCalendarService = mock(BlockCalendarService.class);
    _service.setBlockCalendarService(_blockCalendarService);

    ExtendedCalendarService _extendedCalendarService = mock(ExtendedCalendarService.class);
    when(
        _extendedCalendarService.areServiceIdsActiveOnServiceDate(
            any(ServiceIdActivation.class), any(Date.class))).thenReturn(true);
    _service.setExtendedCalendarService(_extendedCalendarService);

    when(_transitGraph.getTripEntryForId(tripA.getId())).thenReturn(tripA);
    when(_transitGraph.getTripEntryForId(tripB.getId())).thenReturn(tripB);
    when(_transitGraph.getTripEntryForId(tripC.getId())).thenReturn(tripC);
    when(_transitGraph.getTripEntryForId(tripD.getId())).thenReturn(tripD);
    when(_transitGraph.getTripEntryForId(tripE.getId())).thenReturn(tripE);
    when(_transitGraph.getTripEntryForId(tripF.getId())).thenReturn(tripF);
    when(_transitGraph.getTripEntryForId(tripG.getId())).thenReturn(tripG);
    when(_transitGraph.getTripEntryForId(tripH.getId())).thenReturn(tripH);
    when(_transitGraph.getTripEntryForId(tripI.getId())).thenReturn(tripI);

    _service.transformRunData();

  }

  @Test
  public void testFuzzyMatching() {
    TreeMultimap<Integer, String> matches = _service.getBestRunIdsForFuzzyId("0102-5");
    
    Integer bestFuzzyDistance = matches.keySet().first();
    Set<String> fuzzyMatches = matches.get(bestFuzzyDistance);
    
    assertTrue("fuzzy matches contain id", fuzzyMatches.contains("X0102-5"));
    assertEquals("fuzzy matches size", 2, fuzzyMatches.size());
    assertEquals("best fuzzy distance", 1, bestFuzzyDistance.intValue());
    
    matches = _service.getBestRunIdsForFuzzyId("999-75");
    
    bestFuzzyDistance = matches.keySet().first();
    fuzzyMatches = matches.get(bestFuzzyDistance);
    
    assertTrue("fuzzy matches contain id", fuzzyMatches.contains("MISC-75"));
    assertEquals("fuzzy matches size", 1, fuzzyMatches.size());
    assertEquals("best fuzzy distance", 0, bestFuzzyDistance.intValue());
    
    matches = _service.getBestRunIdsForFuzzyId("063-05");
    
    bestFuzzyDistance = matches.keySet().first();
    fuzzyMatches = matches.get(bestFuzzyDistance);
    
    assertTrue("fuzzy matches contain id", fuzzyMatches.contains("B63-5"));
    assertEquals("fuzzy matches size", 1, fuzzyMatches.size());
    assertEquals("best fuzzy distance", 0, bestFuzzyDistance.intValue());
    
    matches = _service.getBestRunIdsForFuzzyId("001-05");
    
    bestFuzzyDistance = matches.keySet().first();
    fuzzyMatches = matches.get(bestFuzzyDistance);
    
    assertTrue("fuzzy matches contain id", fuzzyMatches.contains("X1-5"));
    assertEquals("fuzzy matches size", 1, fuzzyMatches.size());
    assertEquals("best fuzzy distance", 0, bestFuzzyDistance.intValue());
  }
  
  @Test
  public void testRunService() {
    assertEquals("run-1", _service.getInitialRunForTrip(tripA.getId()));
    assertEquals("run-1", _service.getInitialRunForTrip(tripB.getId()));
    assertEquals("run-2", _service.getReliefRunForTrip(tripB.getId()));
    assertEquals("run-2", _service.getInitialRunForTrip(tripD.getId()));

    Collection<RunTripEntry> entities = _service.getRunTripEntriesForRun("run-1");
    assertEquals(3, entities.size());

    RunTripEntry rte0 = Iterables.get(entities, 0);
    RunTripEntry rte1 = Iterables.get(entities, 1);
    RunTripEntry rte2 = Iterables.get(entities, 2);

    assertEquals(rte2, _service.getPreviousEntry(rte0, 0));
    assertEquals(rte0, _service.getPreviousEntry(rte1, 0));
    assertEquals(rte2, _service.getNextEntry(rte1, 0));
    assertEquals(rte0, _service.getNextEntry(rte2, 0));

    entities = _service.getRunTripEntriesForRun("run-2");
    assertEquals(2, entities.size());

  }

}
