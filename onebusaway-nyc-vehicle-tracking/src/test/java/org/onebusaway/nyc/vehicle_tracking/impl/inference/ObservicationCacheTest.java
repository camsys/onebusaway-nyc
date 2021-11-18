/*
 * Copyright (C)  2011 Metropolitan Transportation Authority
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ObservicationCacheTest {

    @Test
    public void getValueForObservationTest(){
        ObservationCache cache = new ObservationCache();
        Observation obs = getObservation();
        cache.putValueForObservation(obs, ObservationCache.EObservationCacheKey.BLOCK_LOCATION, Collections.EMPTY_MAP);
    }

    private Observation getObservation(){
        long timestamp = System.currentTimeMillis();
        NycRawLocationRecord record = new NycRawLocationRecord();
        record.setVehicleId(AgencyAndId.convertFromString("MTA NYCT_12345"));
        String lastValidDestinationSignCode = "1111";
        boolean atBase = false;
        boolean atTerminal = false;
        boolean outOfService = false;
        boolean hasValidDsc = true;
        Observation previousObservation = null;
        Set<AgencyAndId> dscImpliedRoutes = new HashSet<>();
        dscImpliedRoutes.add(AgencyAndId.convertFromString("MTA NYCT_M101"));
        RunResults runResults = Mockito.mock(RunResults.class);
        when(runResults.getRouteIds()).thenReturn(dscImpliedRoutes);
        String assignedBlockId = null;
        boolean isValidAssignedBlockId = false;

        return new Observation(timestamp, record, lastValidDestinationSignCode,atBase, atTerminal, outOfService,
                hasValidDsc, previousObservation, dscImpliedRoutes, runResults, assignedBlockId, isValidAssignedBlockId);
    }
}
