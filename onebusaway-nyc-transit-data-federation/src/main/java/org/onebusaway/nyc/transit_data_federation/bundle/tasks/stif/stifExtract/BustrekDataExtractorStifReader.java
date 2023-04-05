package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract;

import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.*;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.reader.BasicStifReader;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.reader.StifRecordReader;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Link;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class BustrekDataExtractorStifReader extends BasicStifReader {
    // todo: either delete parent class or make StifTripLoader extend parent
    // todo: this is still only partially fixed from copying over the bustrek scripts. make them better.


    private Logger _log = LoggerFactory.getLogger(BustrekDataExtractorStifReader.class);

    private Map<String,String> recordIdToTimepointsMap = new HashMap<>();
    HashMap<String,String> boxIdToTimepointMap = new HashMap();
    private Map<String,String> recordIdToBoxIdMap = new HashMap<>();
    private LinkedHashMap<String, ArrayList<MidTripExtendedTripRecord>> blockNumberToTripRecordMap = new LinkedHashMap<String, ArrayList<MidTripExtendedTripRecord>>();
    //todo: does locationToLocationEventInfoMap need to exist? if so, why not just a set?
    private Map<String, LocationEventInfo> locationToLocationEventInfoMap = new HashMap<>();

    private ArrayList<Remark> remarks = new ArrayList();
    private ArrayList<TripInfo> tripInfo = new ArrayList();
    private ArrayList<TimePoint> timePoints = new ArrayList<>();


    private TripRecord previousTripRecord = null;
    private StifRecord previousRecord = null;

    @Override
    public void run(File path) {
        try {

            _log.info("loading stif from " + path.getAbsolutePath());

            //todo: is it worth the memory cost of just having a temporary array of trips which are processed at the end? probobaly
            InputStream in = new FileInputStream(path);
            if (path.getName().endsWith(".gz"))
                in = new GZIPInputStream(in);
            runWithoutTrips(in, path);
            in.close();
            in = new FileInputStream(path);
            if (path.getName().endsWith(".gz"))
                in = new GZIPInputStream(in);
            runWithTrips(in, path);
            in.close();
            postRunProcessing();
        } catch (Exception e) {
            throw new RuntimeException("Error loading " + path, e);
        }
    }

    public void runWithoutTrips(InputStream stream, File path) {
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
                }
                if (record instanceof SignCodeRecord) {
                    handleSignCodeRecord((SignCodeRecord) record);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void runWithTrips(InputStream stream, File path) {
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
                if (record instanceof TripRecord) {
                    handleTripRecord((TripRecord) record);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handleNoMoreRecords() {

    }

    @Override
    public void handleTimeTableRecord(TimetableRecord record) {

    }

    @Override
    public void handleGeographyRecord(GeographyRecord record) {
        if(!isNullOrBlank(record.getIdentifier()) & !isNullOrBlank(record.getTimepoint())){
            recordIdToTimepointsMap.put(record.getIdentifier(),record.getTimepoint());
        }
        if(!isNullOrBlank(record.getIdentifier()) & !isNullOrBlank(record.getBoxID()))
        {
            recordIdToBoxIdMap.put(record.getIdentifier(),record.getBoxID());
        }
        if(!isNullOrBlank(record.getTimepoint()) & !isNullOrBlank(record.getIdentifier()))
        {
            boxIdToTimepointMap.put(record.getBoxID(),record.getTimepoint());
        }
    }

    @Override
    public void handleTripRecord(TripRecord record) {
        MidTripExtendedTripRecord extendedTripRecord = (MidTripExtendedTripRecord) record;
//        if(extendedTripRecord.getGtfsTripId().equals("UP_H3-Weekday-007000_B3_101"))
//            _log.info("flag");
        extendedTripRecord.setOriginTimePoint(recordIdToTimepointsMap.get(record.getOriginLocation()));
        extendedTripRecord.setDestinationTimePoint(recordIdToTimepointsMap.get(record.getDestinationLocation()));

        if (locationToLocationEventInfoMap.containsKey(record.getReliefLocation())) {
            extendedTripRecord.setMidTripReliefTimePoint(recordIdToTimepointsMap.get(record.getReliefLocation()));
            extendedTripRecord.setMidTripReliefStopID(recordIdToBoxIdMap.get(record.getReliefLocation()));
            extendedTripRecord.setIsMidTrip("Y");
        } else {
            extendedTripRecord.setMidTripReliefTimePoint("");
            extendedTripRecord.setMidTripReliefStopID("");
            extendedTripRecord.setIsMidTrip("N");
        }
        ArrayList tripsArray = blockNumberToTripRecordMap.get(record.getBlockNumber());
        if(tripsArray == null){
            tripsArray = new ArrayList<MidTripExtendedTripRecord>();
            blockNumberToTripRecordMap.put(record.getBlockNumber(),tripsArray);
        }
        tripsArray.add(extendedTripRecord);

    }



    @Override
    public void handleEventRecord(EventRecord record) {
        //todo: should just take the last trip record as an arg
        if(record.isTimepoint()){
            locationToLocationEventInfoMap.put(record.getLocation(), new LocationEventInfo(record,"GOOD, ST"));
        }
        if ((record.isRevenue() | ! record.isRevenue())& record.isTimepoint()){
            TimePoint timePoint = new TimePoint(
                    previousTripRecord.getGtfsTripId(),
                    record.getBoxId(),
                    boxIdToTimepointMap.get(record.getBoxId()),
                    record.getRawDistanceFromStartOfTrip());
            timePoints.add(timePoint);
        }
    }

    @Override
    public void handleSignCodeRecord(SignCodeRecord record) {

    }

    private boolean isNullOrBlank(String s){
        if (s==null) return true;
        if (s.equals("")) return true;
        return false;
    }







    public class LocationEventInfo{
        public String location;
        public boolean stopFlag;
        public boolean timepoint;
        public String xstatus;


        LocationEventInfo(EventRecord record,String xstatus){
            stopFlag = record.isRevenue();
            timepoint = record.isTimepoint();
            location = record.getLocation();
            this.xstatus = xstatus;
        }

        public String getXstatus(){
            return xstatus;
        }
        public String getTimepoint(){
            if(timepoint==true){
                return "T";
            }
            return "F";
        }
        public String getStopFlag(){
            if(stopFlag==true){
                return "S";
            }
            return "N";
        }
        public String getLocation(){
            return location;
        }
    }

    @Override
    public StifRecordReader createStifRecordReader(InputStream stream) {
        return new BusTrekDataStifRecordReader(stream);
    }


    @Override
    public void postRunProcessing(){
        for( Map.Entry<String,ArrayList<MidTripExtendedTripRecord>> entry : blockNumberToTripRecordMap.entrySet()){
            entry.getValue().sort((a,b)->{
                String a_raw = a.getRawOriginTime();
                String b_raw = b.getRawOriginTime();
                int returnVal = Integer.valueOf(a.getRawOriginTime())>Integer.valueOf(b.getRawOriginTime())?1:-1;
                if(a.getRawOriginTime().equals(b.getRawOriginTime())){
                    returnVal = 0;
                }
                return returnVal;});
            ArrayList<RemarkAndRunAndMore> runAndRemark = calculateRemarkAndRun(entry.getValue());
            runAndRemark.sort((a,b)->{
                String a_raw = a.getOriginTime();
                String b_raw = b.getOriginTime();
                int returnVal = Integer.valueOf(a.getOriginTime())>Integer.valueOf(b.getOriginTime())?1:-1;
                if(Integer.valueOf(a_raw)<0 & returnVal!=-1)
                    _log.debug("flag");
                return Integer.valueOf(a.getOriginTime())>Integer.valueOf(b.getOriginTime())?1:-1;});
            for(RemarkAndRunAndMore indData : runAndRemark){
                if(!isNullOrBlank(indData.getCalculatedRun())) {
                    remarks.add(formatRemark(indData));
                    tripInfo.add(new TripInfo(indData));
                }
            }
        }
        return; /// when this is all done make output in the right spot and right type
    }



    private ArrayList<RemarkAndRunAndMore> calculateRemarkAndRun(ArrayList<MidTripExtendedTripRecord> tripsArray){
        // todo: move this and isblank into utils

        //todo: turn map into its own class
        int numberOfTrips = tripsArray.size();
        ArrayList<RemarkAndRunAndMore> remarkAndRunAndMoreArray = new ArrayList();
        for(int tripIndex=0;tripIndex<numberOfTrips;tripIndex++){
            MidTripExtendedTripRecord trip = tripsArray.get(tripIndex);
            RemarkAndRunAndMore output = new RemarkAndRunAndMore(trip);
            MidTripExtendedTripRecord lastTrip = tripIndex>0?tripsArray.get(tripIndex-1):null;
            MidTripExtendedTripRecord twoTripsAgo = tripIndex>1?tripsArray.get(tripIndex-2):null;
            MidTripExtendedTripRecord nextTrip = tripIndex<numberOfTrips-1?tripsArray.get(tripIndex+1):null;
            MidTripExtendedTripRecord nextNextTrip = tripIndex<numberOfTrips-2?tripsArray.get(tripIndex+2):null;
            //skip trips that are "REALLY" pull-out & pull-in
            if(trip.getTripType()!=2 & trip.getTripType()!=3){
                output.setPullOut(false);
                output.setPullIn(false);
                String calculatedRun="";
                String calculatedRemark;

                /**
                 * if either runNum or route differs between this trip and the previous one
                 * AND relief did not happen mid trip
                 * add it before current run, followed by a slash
                 */
                if (tripIndex>0){
                    if ((lastTrip.getRawReliefRunNumber().equals("")
                            & lastTrip.getRawIsLastTripInSequence().equals("N")
                            & !isNullOrBlank(lastTrip.getNextOperatorRoute()) & lastTrip.getNextOperatorRoute().equals("")
                            & !isNullOrBlank(lastTrip.getNextOperatorRunNumber()) & lastTrip.getNextOperatorRunNumber().equals("")) //---Rene added---
                            & (lastTrip.getRunNumber() != trip.getRunNumber() ||
                            lastTrip.getRawRunRoute() != trip.getRawRunRoute())) {
                        if(lastTrip.getTripType()==12)
                            calculatedRun+="LT";

                        calculatedRun+=lastTrip.getRunNumber();

                        if(!lastTrip.getRawRunRoute().equals(trip.getRawRunRoute()))
                            calculatedRun+=lastTrip.getRawRunRoute();

                        if(tripIndex>1 & twoTripsAgo.getTripType()==2 ) { //previous trip was pull out, add PO here
                            output.setPullOut(true);
                            calculatedRun+="PO";
                        }
                        calculatedRun+="/";
                    }
                }

                if(trip.getTripType()==12)
                    calculatedRun+="LT";

                calculatedRun+=trip.getRunNumber();

                if(!trip.getRawRunRoute().equals(trip.getRoute_THISISFROMSIGNCODE()))
                    calculatedRun+=trip.getRawRunRoute();

                if(tripIndex>0){
                    if(lastTrip.getTripType()==2) {
                        //previous trip is really pull out, mark this one
                        output.setPullOut(true);
                        calculatedRun += "PO";
                    }
                }

                output.setCalculatedRun(calculatedRun);
                // todo: fix this give -1
                calculatedRemark=String.valueOf(trip.getRecoveryTime());
                calculatedRemark+=" ";


//                if(nextTrip!=null) {
//                    boolean Ca = (!trip.getNextTripOperatorRunNumber().equals("") & !trip.getNextTripOperatorRoute().equals(""));
//                    boolean Cb = (nextTrip != null);
//
//                    boolean Cc1 = !trip.getNextTripOperatorRunNumber().equals(trip.getRunNumber());
//                    boolean Cc2 = (!isNullOrBlank(nextTrip.getRawReliefRunNumber()) & !nextTrip.getRawReliefRunNumber().equals(""));//---Rene added for subsequent midtrip relief---
//
//                    boolean Cc3a = nextTrip.getRawIsLastTripInSequence().equals("C");
//                    boolean Cc3b = nextTrip.getRawIsLastTripInSequence().equals("Y");
//                    boolean Cc3c = !isNullOrBlank(nextTrip.getNextOperatorRoute());
//                    boolean Cc3d = !nextTrip.getNextOperatorRoute().equals("");
//                    boolean Cc3e = !isNullOrBlank(nextTrip.getNextOperatorRunNumber());
//                    boolean Cc3f = !nextTrip.getNextOperatorRunNumber().equals("");
//                    boolean Cc3z = (Cc3a | Cc3b && Cc3c & Cc3d && Cc3e & Cc3f);  //---Rene added for subsequent terminal relief---
//
//                    boolean Cc4 = !trip.getNextTripOperatorRoute().equals(trip.getRawRunRoute());
//                    boolean Cc10 = (Cc1 | Cc2 | Cc3z | Cc4);
//
//                    boolean Cresult = (Ca && Cb && Cc10);
//                }
//
//
//
//
//                boolean Aa1 = (!trip.getRawReliefRunNumber().equals("") & !trip.getRawReliefRunRoute().equals(""));
//
//                boolean Aa2a = !trip.getRawReliefRunNumber().equals(trip.getRunNumber());
//                boolean Aa2b = !trip.getRawReliefRunRoute().equals(trip.getRawRunRoute());
//                boolean Aa2 = (Aa2a | Aa2b);
//
//                boolean Aa= (Aa1&Aa2);
//
//
//                boolean Ab1 = tripIndex > 0;
//                boolean Ab2 = (trip.getRawIsLastTripInSequence().equals("C") | trip.getRawIsLastTripInSequence().equals("Y")); // todo: this should be : nextTrip.getRawIsLastTripInSequence().equals("C") | nextTrip.getRawIsLastTripInSequence().equals("Y") but things break if it is??;
//                boolean Ab3 = !isNullOrBlank(trip.getNextOperatorRoute()) & !trip.getNextOperatorRoute().equals("");
//                boolean Ab4 = !isNullOrBlank(trip.getNextOperatorRunNumber()) & !trip.getNextOperatorRunNumber().equals("");
//
//                boolean Ab = (Ab1 & Ab2 & Ab3 & Ab4);
//                boolean Aresult =(Aa| Ab);

                    // --- Rene added ---
                if(
                    (
                        (
                            !trip.getRawReliefRunNumber().equals("")
                            & !trip.getRawReliefRunRoute().equals("")
                        )
                        & (
                            !trip.getRawReliefRunNumber().equals(trip.getRunNumber())
                            |!trip.getRawReliefRunRoute().equals(trip.getRawRunRoute())
                        )
                    )
                    | (
                        tripIndex > 0
                        & (
                            trip.getRawIsLastTripInSequence().equals("C")
                            | trip.getRawIsLastTripInSequence().equals("Y")
                        )
                        & !isNullOrBlank(trip.getNextOperatorRoute())
                        & !trip.getNextOperatorRoute().equals("")
                        & !isNullOrBlank(trip.getNextOperatorRunNumber())
                        & !trip.getNextOperatorRunNumber().equals("")
                    )
                ) { //if a midtrip relief is defined, add the information
                    if ((!trip.getRawReliefRunNumber().equals("") & !trip.getRawReliefRunRoute().equals(""))
                            &(!trip.getRawReliefRunNumber().equals(trip.getRunNumber()) |
                            !trip.getRawReliefRunRoute().equals(trip.getRawRunRoute()))) {
                        //NEW RULE: also add mid trip relief to current run followed by a slash
                        calculatedRun+="/";
                        if(trip.getTripType()==12) {
                            calculatedRemark+="LT";
                            calculatedRun+="LT";
                        }

                        calculatedRemark+=trip.getRawReliefRunNumber();
                        calculatedRun+=trip.getRawReliefRunNumber();
                        if(!trip.getRawReliefRunRoute().equals(trip.getRawRunRoute())) {
                            calculatedRemark+=trip.getRawReliefRunRoute();
                            calculatedRun+=trip.getRawReliefRunRoute();
                        }
                        calculatedRemark+="*";
                        output.setCalculatedRun(calculatedRun);  //overwrite previously calculated run with newly calculated one
                    } else{
                        //NEW RULE: also add mid trip relief to current run followed by a slash
                        calculatedRun+="/";
                        if(trip.getTripType()==12) {
                            calculatedRemark+="LT";
                            calculatedRun+="LT";
                        }
                        calculatedRemark+=trip.getNextOperatorRunNumber();
                        calculatedRun+=trip.getNextOperatorRunNumber();

                        if(!trip.getRawRunRoute().equals(trip.getNextOperatorRoute()))
                            calculatedRemark+=trip.getNextOperatorRoute();

                        output.setCalculatedRun(calculatedRun);

                        calculatedRemark+="*";
                        output.setMidTripReliefTimePoint("TDROP");	/**
                         * TDROP or any terminal is a non-revenue stop.
                         * Therefore we put "TDROP" as midTripReliefTimePoint
                         * so that tallySheet knows to tally at the last time point
                         * A.K.A. terminal, A.K.A. not really terminal, A.k.A. closed to terminal
                         */
                    }
                } else if
                (
                    (!trip.getNextTripOperatorRunNumber().equals("") & !trip.getNextTripOperatorRoute().equals(""))
                    &&(
                        !trip.getNextTripOperatorRunNumber().equals(trip.getRunNumber())
                        | ((nextTrip!=null) && !isNullOrBlank(nextTrip.getRawReliefRunNumber()) & !nextTrip.getRawReliefRunNumber().equals(""))//---Rene added for subsequent midtrip relief---
                        | (
                            (nextTrip!=null)
                            && nextTrip.getRawIsLastTripInSequence().equals("C")
                            | nextTrip.getRawIsLastTripInSequence().equals("Y")
                            && !isNullOrBlank(nextTrip.getNextOperatorRoute())
                            & !nextTrip.getNextOperatorRoute().equals("")
                            && !isNullOrBlank(nextTrip.getNextOperatorRunNumber())
                            & !nextTrip.getNextOperatorRunNumber().equals("")
                        )  				 //---Rene added for subsequent terminal relief---
                        | !trip.getNextTripOperatorRoute().equals(trip.getRawRunRoute())
                    )
                ) {

                    if(trip.getTripType()==12)
                        calculatedRemark+="LT";
                    if (
                        (nextTrip!=null)
                        && (
                            nextTrip.getRawIsLastTripInSequence().equals("C")
                            | nextTrip.getRawIsLastTripInSequence().equals("Y")
                        )
                        & !isNullOrBlank(nextTrip.getNextOperatorRoute())
                        & !nextTrip.getNextOperatorRoute().equals("")
                        & !isNullOrBlank(nextTrip.getNextOperatorRunNumber())
                        & !nextTrip.getNextOperatorRunNumber().equals("")
                    ) { //This is for terminal tally and midtrip relief remarks only -- Predict if next trip has a terminal relief
                        calculatedRemark+=nextTrip.getNextTripOperatorRunNumber();
                        if(!nextTrip.getNextOperatorRoute().equals(nextTrip.getRawRunRoute()))
                        {
                            calculatedRemark+=nextTrip.getNextOperatorRoute();
                        }
                        calculatedRemark+="=";
                    } else if (
                        (nextTrip!=null)
                        && !isNullOrBlank(nextTrip.getRawReliefRunNumber())
                        & !nextTrip.getRawReliefRunNumber().equals("")
                    ) {  //---midtrip relief---
                        calculatedRemark+=nextTrip.getNextTripOperatorRunNumber();

                        if((nextTrip!=null) &&!nextTrip.getNextTripOperatorRoute().equals(nextTrip.getRawRunRoute()))
                            calculatedRemark+=nextTrip.getNextTripOperatorRoute();

                        calculatedRemark+="=";
                    } else {
                        calculatedRemark+=trip.getNextTripOperatorRunNumber();
                        if(!trip.getNextTripOperatorRoute().equals(trip.getRawRunRoute()))
                            calculatedRemark+=trip.getNextTripOperatorRoute();

                        calculatedRemark+="=";
                    }
                }

                /**
                 * add pull-in information
                 */

                if(nextTrip!=null && nextTrip.getTripType()==3) {
                    String tempcalculatedRemark=calculatedRemark.trim();
                    if(tempcalculatedRemark.equals("0")) { //remark is just "0", overwrite with PI
                        output.setPullIn(true);
                        output.setPullInTimePoint(trip.getDestinationTimePoint());
                        output.setStartPullInTime(nextTrip.getRawOriginTime());
                        output.setPullInTime(nextTrip.getRawDestinationTime());
                        calculatedRemark="PI";
                    } else {    // append PI to rest of info
                        output.setPullInTimePoint(trip.getDestinationTimePoint());
                        output.setStartPullInTime(nextTrip.getRawOriginTime());
                        output.setPullInTime(nextTrip.getRawDestinationTime());
                        output.setPullIn(true);
                        calculatedRemark+="PI";
                    }
                }
                else if(nextNextTrip!=null && nextNextTrip.getTripType()==3) {
                    calculatedRemark+="PI=";
                }

                output.setCalculatedRemark(calculatedRemark.trim());
                remarkAndRunAndMoreArray.add(output);
            }// end if trip type is not PO/PI

        }// end for loop
        return remarkAndRunAndMoreArray;
    }

    public Remark formatRemark(RemarkAndRunAndMore remarkAndRunAndMore) {
        return new Remark(remarkAndRunAndMore);
    }

    public ArrayList<BustrekDatum> getRemarks() {
        return (ArrayList<BustrekDatum>) (Object) remarks;
    }

    public ArrayList<BustrekDatum> getTimePoints() {
        return (ArrayList<BustrekDatum>) (Object) timePoints;
    }

    public ArrayList<BustrekDatum> getTripInfo() {
        return (ArrayList<BustrekDatum>) (Object) tripInfo;
    }

    public void clear(){
        recordIdToTimepointsMap = new HashMap<>();
        boxIdToTimepointMap = new HashMap();
        recordIdToBoxIdMap = new HashMap<>();
        locationToLocationEventInfoMap = new HashMap<>();
        blockNumberToTripRecordMap = new LinkedHashMap<String, ArrayList<MidTripExtendedTripRecord>>();
    }
}
