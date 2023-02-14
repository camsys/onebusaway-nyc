package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract.model;

import java.io.Serializable;

public class TimePoint extends BustrekDatum implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tripId;
    private String stopId;
    private String timePt;

    public TimePoint(String tripId, String stopId, String timePt){
        this.tripId=tripId;
        this.stopId=stopId;
        this.timePt=timePt;
    }

    public String getStopId() {
        return stopId;
    }

    public String getTimePt() {
        return timePt;
    }

    public String getTripId() {
        return tripId;
    }

    @Override
    public String toString(){
        return tripId + "," +
                stopId + "," +
                timePt;
    }

    @Override
    public int compareTo(Object o) {
        if (getClass() == o.getClass()) {
            TimePoint that = (TimePoint) o;
            int out = 0;
            out = tripId.compareTo(that.getTripId());
            if (out != 0) return out;
            out = stopId.compareTo(that.getStopId());
            if (out != 0) return out;
            out = timePt.compareTo(that.getTimePt());
            return out;
        }
        return 1;
    }
}
