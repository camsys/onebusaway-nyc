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

import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.TripRecord;

public class MidTripExtendedTripRecord extends TripRecord {

    // todo: check if this should just be put into one class with original TripRecord

    String destinationTimePoint;
    String originTimePoint;
    String midTripReliefTimePoint;
    String midTripReliefStopID;
    String isMidTrip;
    String rawSignCode;
    String signCodeBasedRoute;
    String rawReliefRunRoute;
    String rawOriginTime;
    String rawDestinationTime;


    public void setDestinationTimePoint(String destinationTimePoint) {
        this.destinationTimePoint = destinationTimePoint;
    }

    public void setOriginTimePoint(String originTimePoint) {
        this.originTimePoint = originTimePoint;
    }

    public void setMidTripReliefStopID(String midTripReliefStopID) {
        this.midTripReliefStopID = midTripReliefStopID;
    }

    public void setMidTripReliefTimePoint(String midTripReliefTimePoint) {
        this.midTripReliefTimePoint = midTripReliefTimePoint;
    }

    public void setIsMidTrip(String isMidTrip) {
        this.isMidTrip = isMidTrip;
    }

    public String getDestinationTimePoint() {
        return destinationTimePoint;
    }

    public String getIsMidTrip() {
        return isMidTrip;
    }

    public String getMidTripReliefStopID() {
        return midTripReliefStopID;
    }

    public String getMidTripReliefTimePoint() {
        return midTripReliefTimePoint;
    }

    public String getOriginTimePoint() {
        return originTimePoint;
    }

//    @Override
//    public void setOriginTime(int originTime){
//        // todo: there is initially some rounding here, make sure this is viable
//        rawOriginTime = Double.toString((originTime / 60.0) * 100.0);
//        super.setOriginTime(originTime);
//    }
//
//    public String getRawOriginTime() {
//        return rawOriginTime;
//    }

//    @Override
//    public void setDestinationTime(int destinationTime){
//        // todo: there is initially some rounding here, make sure this is viable
//        rawDestinationTime = Double.toString((destinationTime / 60.0) * 100.0);
//        super.setOriginTime(destinationTime);
//    }

//    public String getRawDestinationTime() {
//        return rawDestinationTime;
//    }

    @Override
    public void setReliefRunRoute(String reliefRunRoute){
        rawReliefRunRoute = reliefRunRoute;
        super.setReliefRunRoute(reliefRunRoute);
    }

    public String getRawReliefRunRoute() {
        return rawReliefRunRoute;
    }

    @Override
    public void setSignCodeRoute(String signCode){
        rawSignCode = signCode;
        super.setSignCodeRoute(signCode);
    }


    public String getSignCodeBasedRoute(){
        return signCodeBasedRoute;
    }

    public String getRoute_THISISFROMSIGNCODE(){
        //todo: GO BACK AND MAKE SURE THE getSignCodeBasedRoute uses weren't supposed to be this

        if(signCodeBasedRoute==null){
            signCodeBasedRoute = rawSignCode.trim();
            // todo: haven't confirmed the regex works, double check sometime
//            $output['route']=preg_replace('#(?<=[a-zA-Z])0#','',trim(substr($line,154,6)));
//            $output['route']=preg_replace('#\+#','-SBS',$output['route']);
            signCodeBasedRoute = signCodeBasedRoute.replaceAll("(?<=[a-zA-Z])0","");
            signCodeBasedRoute = signCodeBasedRoute.replaceAll("\\+","-SBS");
        }
        return signCodeBasedRoute;
    }
}
