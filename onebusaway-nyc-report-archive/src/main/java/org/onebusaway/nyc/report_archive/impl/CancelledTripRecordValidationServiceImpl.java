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

package org.onebusaway.nyc.report_archive.impl;

import org.apache.commons.lang3.StringUtils;
import org.onebusaway.nyc.report_archive.model.NycCancelledTripRecord;
import org.onebusaway.nyc.report_archive.services.CancelledTripRecordValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CancelledTripRecordValidationServiceImpl implements CancelledTripRecordValidationService {

    private static Logger _log = LoggerFactory.getLogger(CancelledTripRecordValidationServiceImpl.class);


    @Override
    public boolean isValidRecord(NycCancelledTripRecord record) {
        if(record == null){
            _log.warn("cancelled trip record is null");
            return false;
        }
        if(StringUtils.isBlank(record.getTrip())){
            _log.warn("cancelled trip 'Trip Id' value is empty for record {}", record.toString());
            return false;
        }
        if(StringUtils.isBlank(record.getStatus())){
            _log.warn("cancelled trip status value is empty for record {}", record.toString());
            return false;
        }
        return true;
    }
}
