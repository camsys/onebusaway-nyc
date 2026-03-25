package org.onebusaway.api.actions.api.where;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data.model.trip_mods.TripModificationDiff;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
    private static final String DATE_JAN_16 = "20250116";

    @Before
    public void setUp() {
        _action = new TripModificationDiffsAction();
        _action.setNycTransitDataService(_service);

        when(_diffJan15.getEffectiveServiceDate()).thenReturn(DATE_JAN_15);
        when(_diffJan16.getEffectiveServiceDate()).thenReturn(DATE_JAN_16);
    }

    // =========================================================================
    // getFormattedServiceDate()
    // =========================================================================

    @Test
    public void getFormattedServiceDate_nullServiceDate_returnsEmpty() {
        _action.setServiceDate(null);

        Optional<String> result = _action.getFormattedServiceDate();

        assertFalse(result.isPresent());
    }

    @Test
    public void getFormattedServiceDate_validDate_returnsFormattedDate() {
        _action.setServiceDate(DATE_JAN_15);

        Optional<String> result = _action.getFormattedServiceDate();

        assertTrue(result.isPresent());
        assertEquals(DATE_JAN_15, result.get());
    }

    @Test
    public void getFormattedServiceDate_invalidFormat_returnsEmpty() {
        _action.setServiceDate("not-a-date");

        Optional<String> result = _action.getFormattedServiceDate();

        assertFalse(result.isPresent());
    }

    @Test
    public void getFormattedServiceDate_partialDate_returnsEmpty() {
        _action.setServiceDate("202501");

        Optional<String> result = _action.getFormattedServiceDate();

        assertFalse(result.isPresent());
    }

    @Test
    public void getFormattedServiceDate_wrongDelimiters_returnsEmpty() {
        // Valid date but wrong format (yyyy-MM-dd instead of yyyyMMdd)
        _action.setServiceDate("2025-01-15");

        Optional<String> result = _action.getFormattedServiceDate();

        assertFalse(result.isPresent());
    }

    @Test
    public void getFormattedServiceDate_validDate_valueMatchesInput() {
        // Ensures parse→format round-trip doesn't mangle the value
        _action.setServiceDate(DATE_JAN_15);

        String result = _action.getFormattedServiceDate().get();

        assertEquals(DATE_JAN_15, result);
    }

    // =========================================================================
    // getTripModDiffs()
    // =========================================================================

    @Test
    public void getTripModDiffs_emptyOptional_returnsAllDiffs() {
        when(_service.getAllTripModificationDiffs())
                .thenReturn(Arrays.asList(_diffJan15, _diffJan16));

        List<TripModificationDiff> result = _action.getTripModDiffs(Optional.empty());

        assertEquals(2, result.size());
        assertTrue(result.contains(_diffJan15));
        assertTrue(result.contains(_diffJan16));
    }

    @Test
    public void getTripModDiffs_matchingDate_returnsOnlyMatchingDiff() {
        when(_service.getAllTripModificationDiffs())
                .thenReturn(Arrays.asList(_diffJan15, _diffJan16));

        List<TripModificationDiff> result = _action.getTripModDiffs(Optional.of(DATE_JAN_15));

        assertEquals(1, result.size());
        assertTrue(result.contains(_diffJan15));
        assertFalse(result.contains(_diffJan16));
    }

    @Test
    public void getTripModDiffs_dateMatchesNoDiffs_returnsEmptyList() {
        when(_service.getAllTripModificationDiffs())
                .thenReturn(Arrays.asList(_diffJan15, _diffJan16));

        List<TripModificationDiff> result = _action.getTripModDiffs(Optional.of("20250101"));

        assertTrue(result.isEmpty());
    }

    @Test
    public void getTripModDiffs_dateMatchesAllDiffs_returnsAllDiffs() {
        when(_diffJan16.getEffectiveServiceDate()).thenReturn(DATE_JAN_15); // both on same date
        when(_service.getAllTripModificationDiffs())
                .thenReturn(Arrays.asList(_diffJan15, _diffJan16));

        List<TripModificationDiff> result = _action.getTripModDiffs(Optional.of(DATE_JAN_15));

        assertEquals(2, result.size());
    }

    @Test
    public void getTripModDiffs_emptyServiceList_returnsEmptyList() {
        when(_service.getAllTripModificationDiffs()).thenReturn(Collections.emptyList());

        List<TripModificationDiff> result = _action.getTripModDiffs(Optional.of(DATE_JAN_15));

        assertTrue(result.isEmpty());
    }

    @Test
    public void getTripModDiffs_emptyServiceList_noFilter_returnsEmptyList() {
        when(_service.getAllTripModificationDiffs()).thenReturn(Collections.emptyList());

        List<TripModificationDiff> result = _action.getTripModDiffs(Optional.empty());

        assertTrue(result.isEmpty());
    }

    @Test
    public void getTripModDiffs_diffWithNullServiceDate_doesNotThrow() {
        when(_diffJan15.getEffectiveServiceDate()).thenReturn(null);
        when(_service.getAllTripModificationDiffs())
                .thenReturn(Collections.singletonList(_diffJan15));

        List<TripModificationDiff> result = _action.getTripModDiffs(Optional.of(DATE_JAN_15));

        assertEquals(1, result.size());
    }
}