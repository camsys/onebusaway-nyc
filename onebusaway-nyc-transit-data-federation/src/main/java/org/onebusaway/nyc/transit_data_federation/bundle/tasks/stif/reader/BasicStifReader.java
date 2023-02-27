package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.reader;

import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public abstract class BasicStifReader{
    //todo: delete this class

    private Logger _log = LoggerFactory.getLogger(BasicStifReader.class);

    private TripRecord previousTripRecord = null;
    private StifRecord previousRecord = null;


    public void run(File path) {
        try {
            if(path.getName().equals("stif.b_0027x_.4H8422.wkd.closed")){
                _log.info("break");
            }
            _log.info("loading stif from " + path.getAbsolutePath());
            InputStream in = new FileInputStream(path);
            if (path.getName().endsWith(".gz"))
                in = new GZIPInputStream(in);
            run(in, path);
            in.close();
            postRunProcessing();
        } catch (Exception e) {
            throw new RuntimeException("Error loading " + path, e);
        }
    }

    public void run(InputStream stream, File path) {
        try {
            StifRecordReader reader = createStifRecordReader(stream);
            int lineNumber = 0;
            StifRecord record = null;
            while (true) {
                previousRecord = record;
                record = reader.read();
                lineNumber++;

                if (record == null) {
                    handleNoMoreRecords();
                    break;
                }
                if (record instanceof TimetableRecord) {
                    handleTimeTableRecord((TimetableRecord) record);
                    continue;
                }
                if (record instanceof GeographyRecord) {
                    handleGeographyRecord((GeographyRecord) record);
                    continue;
                }

                if (record instanceof EventRecord) {
                    handleEventRecord((EventRecord) record);
                    continue;
                }

                if (record instanceof TripRecord) {
                    previousTripRecord = (TripRecord) record;
                    handleTripRecord((TripRecord) record);
                }
                if (record instanceof SignCodeRecord) {
                    handleSignCodeRecord((SignCodeRecord) record);
                }


            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public TripRecord getLastTripRecord(){
        return previousTripRecord;
    }

    public StifRecord getPreviousRecord(){
        return previousRecord;
    }

    public StifRecordReader createStifRecordReader(InputStream stream) {
        return new StifRecordReader(stream);
    }

    public abstract void postRunProcessing();

    public abstract void handleTimeTableRecord(TimetableRecord record);

    public abstract void handleGeographyRecord(GeographyRecord record);

    public abstract void handleTripRecord(TripRecord record);

    public abstract void handleEventRecord(EventRecord record);

    public abstract void handleSignCodeRecord(SignCodeRecord signCodeRecord);

    public abstract void handleNoMoreRecords();

    public abstract void clear();

}

