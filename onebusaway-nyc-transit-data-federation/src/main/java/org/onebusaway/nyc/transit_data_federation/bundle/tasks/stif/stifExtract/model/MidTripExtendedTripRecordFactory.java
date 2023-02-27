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
