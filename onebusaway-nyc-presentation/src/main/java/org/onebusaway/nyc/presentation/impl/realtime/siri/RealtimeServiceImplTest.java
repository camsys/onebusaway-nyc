package org.onebusaway.nyc.presentation.impl.realtime.siri;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.onebusaway.nyc.presentation.impl.realtime.RealtimeServiceImpl;
import org.onebusaway.nyc.presentation.service.realtime.PresentationService;
import org.onebusaway.nyc.presentation.service.realtime.siri.SiriMonitoredVehicleJourneyBuilderService;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import uk.org.siri.siri.MonitoredStopVisitStructure;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RealtimeServiceImplTest {

    private RealtimeServiceImpl realtimeServiceimpl;
    @Mock
    private PresentationService presentationService;
    @Mock
    private NycTransitDataService nycTransitDataService;
    @Mock
    private SiriMonitoredVehicleJourneyBuilderService siriMvjBuilderService;

    public void setup(){

        realtimeServiceimpl = new RealtimeServiceImpl();

        presentationService = mock(PresentationService.class);
        realtimeServiceimpl.setPresentationService(presentationService);
        nycTransitDataService = mock(NycTransitDataService.class);
        realtimeServiceimpl.setNycTransitDataService(nycTransitDataService);
        siriMvjBuilderService = mock(SiriMonitoredVehicleJourneyBuilderService.class);
        realtimeServiceimpl.setSiriMvjBuilderService(siriMvjBuilderService);






    }
    @Test
    public void testGetMonitoredStopVisitForStop(){
        setup();

        List<MonitoredStopVisitStructure> result = realtimeServiceimpl.getMonitoredStopVisitsForStop(
                "1",
                3,
                java.time.ZonedDateTime.now().toInstant().toEpochMilli(),
                true,
                true
        );
    }
}
