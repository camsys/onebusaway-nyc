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

package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract.model;

import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.StifFieldDefinition;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.StifFieldSetter;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.TripRecord;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.TripRecordFactory;

public class MidTripExtendedTripRecordFactory extends TripRecordFactory {

    static class FieldDef extends StifFieldDefinition<MidTripExtendedTripRecord> {
        public FieldDef(int length, String name, StifFieldSetter<MidTripExtendedTripRecord> setter) {
            super(length, name, setter);
        }
    };

    @Override
    public TripRecord createEmptyRecord() {
        return new MidTripExtendedTripRecord();
    }

    @SuppressWarnings("unchecked")
    @Override
    public StifFieldDefinition<TripRecord>[] getFields() {
        return super.getFields();
    }

}
