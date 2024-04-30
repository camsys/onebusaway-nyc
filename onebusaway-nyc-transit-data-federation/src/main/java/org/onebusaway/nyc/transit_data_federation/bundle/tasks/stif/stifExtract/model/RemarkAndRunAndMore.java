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

public class RemarkAndRunAndMore {

    private String calculatedRemark = "";
    private String calculatedRun = "";
    private Boolean isPullIn = false;
    private Boolean isPullOut = false;
    private String midTripReliefTimePoint = "";
    private String pullInTime;
    private String pullInTimePoint;
    private String startPullInTime;
    private MidTripExtendedTripRecord tripRecord;

    public RemarkAndRunAndMore(MidTripExtendedTripRecord tripRecord) {
        this.tripRecord = tripRecord;
        this.midTripReliefTimePoint = tripRecord.getMidTripReliefTimePoint();
    }

    public void setCalculatedRun(String calculatedRun) {
        this.calculatedRun = calculatedRun;
    }

    public String getCalculatedRun() {
        return calculatedRun;
    }

    public void setCalculatedRemark(String calculatedRemark) {
        this.calculatedRemark = calculatedRemark;
    }

    public String getCalculatedRemark() {
        return calculatedRemark;
    }

    public String getMidTripReliefTimePoint() {
        return midTripReliefTimePoint;
    }

    public Boolean getPullIn() {
        //todo: this should be a T or "" to match what bustrek is getting
        return isPullIn;
    }

    public Boolean getPullOut() {
        //todo: this should be a T or "" to match what bustrek is getting
        return isPullOut;
    }

    public String getPullInTime() {
        return pullInTime;
    }

    public String getPullInTimePoint() {
        return pullInTimePoint;
    }

    public String getStartPullInTime() {
        return startPullInTime;
    }

    public void setMidTripReliefTimePoint(String midTripReliefTimePoint) {
        this.midTripReliefTimePoint = midTripReliefTimePoint;
    }

    public void setPullIn(Boolean pullIn) {
        isPullIn = pullIn;
    }

    public void setPullInTime(String pullInTime) {
        this.pullInTime = pullInTime;
    }

    public void setPullInTimePoint(String pullInTimePoint) {
        this.pullInTimePoint = pullInTimePoint;
    }

    public void setPullOut(Boolean pullOut) {
        isPullOut = pullOut;
    }

    public void setStartPullInTime(String startPullInTime) {
        this.startPullInTime = startPullInTime;
    }




    //todo: just extract the needed parts of the trip record once what this class actually needs to be is clear

    public String getDestinationTimePoint() {
        if(tripRecord.getDestinationTimePoint()!=null)
            return tripRecord.getDestinationTimePoint();
        return "";
    }

    public String getOriginTime() {
        return tripRecord.getRawOriginTime();
    }

    public String getBlockId() {
        return tripRecord.getBlockNumber();
    }

    public String getTripId(){
        return tripRecord.getGtfsTripId();
    }

    public String getSignCodeRoute(){
        return tripRecord.getRawSignCodeRoute();
    }
    public String getRunRoute(){
        return tripRecord.getRawRunRoute();
    }
    public String getRoute(){
        return tripRecord.getRoute_THISISFROMSIGNCODE();
    }
    public String getDepot(){
        return tripRecord.getDepotCode();
    }
    public String getRunNum(){
        return tripRecord.getRunNumber();
    }
    public String getDirection(){
        return tripRecord.getDirection();
    }
    public int getTripType(){
        return tripRecord.getTripType();
    }
}
