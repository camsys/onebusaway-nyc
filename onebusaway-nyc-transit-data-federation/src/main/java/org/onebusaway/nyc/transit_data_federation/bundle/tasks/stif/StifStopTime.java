package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif;

import java.io.Serializable;

public class StifStopTime implements Serializable {
    private static final long serialVersionUID = 2L;
    public String id;
    public int time;
    public boolean revenue;
    public int boardingAlightingFlag;

    public StifStopTime(String id, int time, int boardingAlightingFlag, boolean revenue){
        this.id = id;
        this.time = time;
        this.boardingAlightingFlag = boardingAlightingFlag;
        this.revenue = revenue;
    }

    public void setBoardingAlightingFlag(int boardingAlightingFlag) {
        this.boardingAlightingFlag = boardingAlightingFlag;
    }

    public int getBoardingAlightingFlag(){
        return boardingAlightingFlag;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setRevenue(boolean revenue) {
        this.revenue = revenue;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getTime() {
        return time;
    }

    public String getId() {
        return id;
    }

    public boolean isRevenue() {
        return revenue;
    }

    public String toString(){
        return id + "," + time + "," + revenue + "," + boardingAlightingFlag;
    }
}
