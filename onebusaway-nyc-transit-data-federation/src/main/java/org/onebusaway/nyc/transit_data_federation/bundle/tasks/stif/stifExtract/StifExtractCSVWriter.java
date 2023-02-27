package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract;

import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.MultiCSVLogger;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract.model.BustrekDatum;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract.model.Remark;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract.model.TripInfo;

import java.io.File;
import java.util.ArrayList;

public class StifExtractCSVWriter {

    MultiCSVLogger csvLogger;

    StifExtractCSVWriter(MultiCSVLogger csvLogger){
        this.csvLogger=csvLogger;
    }

    public void writeBusTrekData(ArrayList<BustrekDatum> busTrekData, File fileName) {
        csvLogger.nullHeader(fileName.getName());
        for(BustrekDatum datum : busTrekData) {
            if(datum.getClass()== TripInfo.class){
                TripInfo testing = (TripInfo) datum;
            }
            if(datum.getClass()== Remark.class){
                Remark testing = (Remark) datum;
            }
            csvLogger.logCSV(fileName.getName(), datum.toString());
        }
    }
}
