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

package org.onebusaway.nyc.presentation.impl.realtime;

import org.onebusaway.realtime.api.TimepointPredictionRecord;

public class SiriSupportPredictionTimepointRecord{
    private String _TripId = "";
    private String _StopId = "";
    private TimepointPredictionRecord _TimePointPredictionRecord;
    public void setTripId(String TripId){
        _TripId = TripId;
    }
    public void setStopId(String StopId){
        _StopId = StopId;
    }
    public void setTimepointPredictionRecord(TimepointPredictionRecord TimepointPredictionRecord){
        _TimePointPredictionRecord = TimepointPredictionRecord;
    }
    public String getKey(){
        return _TripId+_StopId;
    }
    public String getKey(String tripId, String stopId){
        _TripId = tripId;
        _StopId = stopId;
        return getKey();
    }

    public static String convertTripAndStopToKey(String tripId, String stopId){
        return tripId + stopId;
    }

    public TimepointPredictionRecord getTimepointPredictionRecord(){
        return _TimePointPredictionRecord;
    }
}
