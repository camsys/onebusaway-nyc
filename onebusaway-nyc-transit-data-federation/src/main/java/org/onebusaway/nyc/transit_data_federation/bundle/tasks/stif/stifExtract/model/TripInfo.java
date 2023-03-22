package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract.model;

import java.io.Serializable;

public class TripInfo extends BustrekDatum implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tripId;
    private String signCodeRoute;
    private String runRoute;
    private String route;
    private String depot;
    private String runNum;
    private String startPullInTime;
    private String pullInTimePoint;
    private String pullInTime;
    private String direction;
    private int tripType;
    private boolean isPullIn;

    public TripInfo(RemarkAndRunAndMore remarkAndRunAndMore){
        tripId = remarkAndRunAndMore.getTripId();
        signCodeRoute = remarkAndRunAndMore.getSignCodeRoute();
        runRoute = remarkAndRunAndMore.getRunRoute();
        route = remarkAndRunAndMore.getRoute();
        route = route!=null? route:runRoute;
        route = !route.equals("")? route:runRoute;
        route += remarkAndRunAndMore.getTripType()==12?"-LTD":"";
        depot = remarkAndRunAndMore.getDepot();
        runNum = remarkAndRunAndMore.getRunNum();;
        direction = remarkAndRunAndMore.getDirection();
        tripType = remarkAndRunAndMore.getTripType();

        if(remarkAndRunAndMore.getPullIn()){
            isPullIn  = remarkAndRunAndMore.getPullIn();
            pullInTimePoint = remarkAndRunAndMore.getPullInTimePoint();
            startPullInTime = formatTime(remarkAndRunAndMore.getStartPullInTime());
            pullInTime = formatTime(remarkAndRunAndMore.getPullInTime());
        } else {
            isPullIn = remarkAndRunAndMore.getPullIn();
            pullInTimePoint = "";
            startPullInTime = "";
            pullInTime = "";
        }
    }

    public TripInfo(String[] data){
        try {
            tripId = data[0];
            signCodeRoute = data[1];
            runRoute = data[2];
            route = data[3];
            depot = data[4];
            runNum = data[5];
            startPullInTime = data[6];
            pullInTimePoint = data[7];
            pullInTime = data[8];
            direction = data[9];
            tripType = Integer.parseInt(data[10]);
        } catch (IndexOutOfBoundsException ex){
            // todo: log error
        }
    }

    public String getTripId() {
        return tripId;
    }

    public String getStartPullInTime() {
        return startPullInTime;
    }

    public String getPullInTimePoint() {
        return pullInTimePoint;
    }

    public String getPullInTime() {
        return pullInTime;
    }

    public String getRoute() {
        return route;
    }

    public int getTripType() {
        return tripType;
    }

    public String getDepot() {
        return depot;
    }

    public String getDirection() {
        return direction;
    }

    public String getRunNum() {
        return runNum;
    }

    public String getRunRoute() {
        return runRoute;
    }

    public String getSignCodeRoute() {
        return signCodeRoute;
    }

    @Override
    public String toString(){
        String out = tripId + "," +
            signCodeRoute + "," +
            runRoute + "," +
            route + "," +
            depot + "," +
            runNum + "," +
            startPullInTime + "," +
            pullInTimePoint + "," +
            pullInTime + "," +
            direction + "," +
            tripType;
        return out;
    }




    public static String formatTime(String timeString){
        int time = Integer.valueOf(timeString);
        int hour = (int) Math.floor(time/6000);
        int min  = (time%6000)/100;
        String out = String.format("%d:%02d:00", hour, min);
        return out;
    }

    @Override
    public int compareTo(Object o){
        if(getClass()==o.getClass()){
            TripInfo that = (TripInfo) o;
            int out =0;
            out = tripId.compareTo(that.getTripId());
            if(out!=0) return out;
            out =signCodeRoute.compareTo(that.getSignCodeRoute());
            if(out!=0) return out;
            out = runRoute.compareTo(that.getRunRoute());
            if(out!=0) return out;
            out = route.compareTo(that.getRoute());
            if(out!=0) return out;
            out = depot.compareTo(that.getDepot());
            if(out!=0) return out;
            out = runNum.compareTo(that.getRunNum());
            if(out!=0) return out;
            out = startPullInTime.compareTo(that.startPullInTime);
            if(out!=0) return out;
            out = pullInTimePoint.compareTo(that.getPullInTimePoint());
            if(out!=0) return out;
            out = pullInTime.compareTo(that.getPullInTime());
            if(out!=0) return out;
            out = direction.compareTo(that.getDirection());
            if(out!=0) return out;
            out = tripType - that.getTripType();
            return out;
        }
        return 1;
    }
}
