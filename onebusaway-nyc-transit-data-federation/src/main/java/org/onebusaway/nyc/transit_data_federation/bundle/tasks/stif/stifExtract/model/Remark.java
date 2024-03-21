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

import java.io.Serializable;
import java.util.Arrays;

public class Remark extends BustrekDatum implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tripID;
    private String calculatedRemark;
    private String calculatedRun;
    private String destinationTimePoint;
    private String originTime;
    private String blockID;
    private String midTripReliefTimePoint;


    public Remark(RemarkAndRunAndMore remarkAndRunAndMore){
        this.tripID=remarkAndRunAndMore.getTripId();
        this.calculatedRemark=remarkAndRunAndMore.getCalculatedRemark();
        this.calculatedRun=remarkAndRunAndMore.getCalculatedRun();
        this.destinationTimePoint=remarkAndRunAndMore.getDestinationTimePoint();
        this.originTime=remarkAndRunAndMore.getOriginTime();
        this.blockID=remarkAndRunAndMore.getBlockId();
        this.midTripReliefTimePoint=remarkAndRunAndMore.getMidTripReliefTimePoint();
    }

    public Remark(String[] remarkArray){
        try {
            Arrays.stream(remarkArray).forEach(x->{if(x==null){x="";}});

            this.tripID = remarkArray[0];
            this.calculatedRemark = remarkArray[1];
            this.calculatedRun = remarkArray[2];
            this.destinationTimePoint = remarkArray[3];
            this.originTime = remarkArray[4];
            this.blockID = remarkArray[5];
            if(remarkArray.length==7){
                this.midTripReliefTimePoint = remarkArray[6];
            } else{
                this.midTripReliefTimePoint = "";
            }
        } catch (IndexOutOfBoundsException ex){
            // todo: log error
        }
    }

    @Override
    public String toString(){
        String out = tripID + "," +
                calculatedRemark + "," +
                calculatedRun + "," +
                destinationTimePoint + "," +
                originTime + "," +
                blockID + "," +
                midTripReliefTimePoint;
        return out;
    }


    public String getMidTripReliefTimePoint() {
        return midTripReliefTimePoint;
    }

    public String getDestinationTimePoint() {
        return destinationTimePoint;
    }

    public String getBlockID() {
        return blockID;
    }

    public String getCalculatedRemark() {
        return calculatedRemark;
    }

    public String getCalculatedRun() {
        return calculatedRun;
    }

    public String getOriginTime() {
        return originTime;
    }

    public String getTripID() {
        return tripID;
    }


    @Override
    public int compareTo(Object o) {
        if(getClass()==o.getClass()){
            Remark that = (Remark) o;
            int out = tripID.compareTo(that.getTripID());
            if(out!=0) return out;
            out = calculatedRemark.compareTo(that.getCalculatedRemark());
            if(out!=0) return out;
            out = calculatedRun.compareTo(that.getCalculatedRun());
            if(out!=0) return out;
            out = destinationTimePoint.compareTo(that.getDestinationTimePoint());
            if(out!=0) return out;
            out = originTime.compareTo(that.getOriginTime());
            if(out!=0) return out;
            out = blockID.compareTo(that.getBlockID());
            if(out!=0) return out;
            out = midTripReliefTimePoint.compareTo(that.getMidTripReliefTimePoint());
            return out;
        }
        return 1;
    }

}
