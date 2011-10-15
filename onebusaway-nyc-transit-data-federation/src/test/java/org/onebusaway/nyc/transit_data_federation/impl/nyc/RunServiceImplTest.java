package org.onebusaway.nyc.transit_data_federation.impl.nyc;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.stop;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.stopTime;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.trip;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class RunServiceImplTest {

  private RunServiceImpl _service;

  private TransitGraphDao _transitGraph;

  private ScheduledBlockLocationService _scheduledBlockLocationService;

  private BlockCalendarService _blockCalendarService;

  private TripEntryImpl tripA, tripB, tripC, tripD;

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
    runDataByTrip.put(tripA.getId(), new RunData("run1", null, -1));
    runDataByTrip.put(tripB.getId(), new RunData("run1", "run2", 270));
    runDataByTrip.put(tripC.getId(), new RunData("run1", null, -1));
    // don't ask how the driver of run2 gets to StopA
    runDataByTrip.put(tripD.getId(), new RunData("run2", null, -1));

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

    _service.transformRunData();

  }

  @Test
  public void testRunService() {
    assertEquals("run1", _service.getInitialRunForTrip(tripA.getId()));
    assertEquals("run1", _service.getInitialRunForTrip(tripB.getId()));
    assertEquals("run2", _service.getReliefRunForTrip(tripB.getId()));
    assertEquals("run2", _service.getInitialRunForTrip(tripD.getId()));

    List<RunTripEntry> entities = _service.getRunTripEntriesForRun("run1");
    assertEquals(3, entities.size());

    RunTripEntry rte0 = entities.get(0);
    RunTripEntry rte1 = entities.get(1);
    RunTripEntry rte2 = entities.get(2);

    assertEquals(rte2, _service.getPreviousEntry(rte0, 0));
    assertEquals(rte0, _service.getPreviousEntry(rte1, 0));
    assertEquals(rte2, _service.getNextEntry(rte1, 0));
    assertEquals(rte0, _service.getNextEntry(rte2, 0));

    entities = _service.getRunTripEntriesForRun("run2");
    assertEquals(2, entities.size());

  }

}
