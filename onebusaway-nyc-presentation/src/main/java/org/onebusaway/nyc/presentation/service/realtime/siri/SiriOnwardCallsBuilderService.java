/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
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

package org.onebusaway.nyc.presentation.service.realtime.siri;

import org.onebusaway.nyc.presentation.impl.realtime.SiriSupportPredictionTimepointRecord;
import org.onebusaway.nyc.presentation.impl.realtime.siri.OnwardCallsMode;
import org.onebusaway.transit_data.model.blocks.BlockInstanceBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import uk.org.siri.siri.OnwardCallsStructure;

import java.util.Map;

public interface SiriOnwardCallsBuilderService {
    OnwardCallsStructure makeOnwardCalls(BlockInstanceBean blockInstance,
                                         TripBean tripOnBlock,
                                         TripStatusBean currentlyActiveTripOnBlock,
                                         OnwardCallsMode onwardCallsMode,
                                         Map<String, SiriSupportPredictionTimepointRecord> stopLevelPredictions,
                                         int maximumOnwardCalls,
                                         boolean isCancelled,
                                         long responseTimestamp);
}
