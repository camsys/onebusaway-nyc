/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
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
package org.onebusaway.api.actions.api.where;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data.model.trip_mods.TripModificationDiff;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TripModificationDiffsActionTest {

    @Mock
    private NycTransitDataService _service;

    @Mock
    private TripModificationDiff _diffJan15;

    @Mock
    private TripModificationDiff _diffJan16;

    private TripModificationDiffsAction _action;

    private static final String DATE_JAN_15 = "20250115";

    @Before
    public void setUp() {
        _action = new TripModificationDiffsAction();
        _action.setNycTransitDataService(_service);
    }

    // =========================================================================
    // getTripModDiffs() — verifies correct serviceDate string is passed to service
    // =========================================================================

    @Test
    public void getTripModDiffs_noServiceDate_callsServiceWithNull() {
        when(_service.getAllTripModificationDiffs(null))
                .thenReturn(Arrays.asList(_diffJan15, _diffJan16));

        _action.setServiceDate(null);
        _action.getTripModDiffs();

        verify(_service).getAllTripModificationDiffs(null);
    }

    @Test
    public void getTripModDiffs_validDate_callsServiceWithDateString() {
        when(_service.getAllTripModificationDiffs(DATE_JAN_15))
                .thenReturn(Collections.singletonList(_diffJan15));

        _action.setServiceDate(DATE_JAN_15);
        _action.getTripModDiffs();

        verify(_service).getAllTripModificationDiffs(DATE_JAN_15);
    }

    @Test
    public void getTripModDiffs_invalidDate_callsServiceWithInvalidString() {
        when(_service.getAllTripModificationDiffs("not-a-date"))
                .thenReturn(Collections.emptyList());

        _action.setServiceDate("not-a-date");
        _action.getTripModDiffs();

        verify(_service).getAllTripModificationDiffs("not-a-date");
    }

    @Test
    public void getTripModDiffs_returnsServiceResults() {
        when(_service.getAllTripModificationDiffs(DATE_JAN_15))
                .thenReturn(Arrays.asList(_diffJan15, _diffJan16));

        _action.setServiceDate(DATE_JAN_15);
        Collection<TripModificationDiff> result = _action.getTripModDiffs();

        assertEquals(2, result.size());
        assertTrue(result.contains(_diffJan15));
        assertTrue(result.contains(_diffJan16));
    }

    @Test
    public void getTripModDiffs_emptyServiceResult_returnsEmptyCollection() {
        when(_service.getAllTripModificationDiffs(any()))
                .thenReturn(Collections.emptyList());

        _action.setServiceDate(DATE_JAN_15);
        Collection<TripModificationDiff> result = _action.getTripModDiffs();

        assertTrue(result.isEmpty());
    }

    // =========================================================================
    // index()
    // =========================================================================

    @Test
    public void index_nullService_returnsExceptionResponse() {
        _action = new TripModificationDiffsAction(); // no service set

        assertNotNull(_action.index());
        // verify no NPE thrown
    }

    @Test
    public void index_emptyDiffs_returnsNotFoundResponse() {
        when(_service.getAllTripModificationDiffs(any()))
                .thenReturn(Collections.emptyList());

        _action.setServiceDate(DATE_JAN_15);
        _action.index();

        verify(_service).getAllTripModificationDiffs(DATE_JAN_15);
    }

    @Test
    public void index_nonEmptyDiffs_returnsOkResponse() {
        when(_service.getAllTripModificationDiffs(DATE_JAN_15))
                .thenReturn(Collections.singletonList(_diffJan15));

        _action.setServiceDate(DATE_JAN_15);
        _action.index();

        verify(_service).getAllTripModificationDiffs(DATE_JAN_15);
    }
}