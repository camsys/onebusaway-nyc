package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract.model;

import java.io.Serializable;

public class TimePoint extends BustrekDatum implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tripId;
    private String stopId;
    private String timePt;
    private String dist;

    public TimePoint(String tripId, String stopId, String timePt,String dist){
        this.tripId=tripId;
        this.stopId=stopId;
        this.timePt=timePt;
        this.dist=dist;
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

    public String getDist() {return dist;}

    @Override
    public String toString(){
        return tripId + "," +
                stopId + "," +
                timePt + "," +
                dist;
    }

    @Override
    public int compareTo(Object o) {
        if (getClass() == o.getClass()) {
            TimePoint that = (TimePoint) o;
            int out = 0;
            out = compare(this.tripId,that.getTripId());
            if (out != 0) return out;
            out = compare(this.stopId,that.getStopId());
            if (out != 0) return out;
            out = compare(this.timePt,that.getTimePt());
            if (out !=0) return out;
            out = compare(this.dist,that.getDist());
            return out;
        }
        return 1;
    }

    private int compare(String mine, String theirs){
        if(mine==null) {
            if (theirs != null) {
                return -1;
            }
            return 0;
        }
        return mine.compareTo(theirs);
    }
}
