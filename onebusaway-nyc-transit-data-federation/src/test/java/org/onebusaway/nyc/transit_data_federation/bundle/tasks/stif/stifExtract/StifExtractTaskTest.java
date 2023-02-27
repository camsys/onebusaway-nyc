package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;
import org.onebusaway.nyc.transit_data_federation.bundle.model.NycFederatedTransitDataBundle;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.MultiCSVLogger;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract.model.BustrekDatum;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract.model.Remark;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract.model.TimePoint;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract.model.TripInfo;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifImport.StifImportTask;
import org.onebusaway.utility.ObjectSerializationLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.annotation.PostConstruct;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class StifExtractTaskTest {

    private static Logger _log = LoggerFactory
            .getLogger(StifExtractTaskTest.class);

    int _breakOnThisLineNumber;
    File _basePath;
    NycFederatedTransitDataBundle bundle;

    public StifExtractTask initialize(String stifPath) throws IOException {
        StifExtractTask stifExtractTask = new StifExtractTask();
        stifExtractTask.setStifPath(new File(this.getClass().getResource(stifPath).getPath()));
        MultiCSVLogger logger = new MultiCSVLogger();
        _basePath = new File(getClass().getResource(stifPath).getPath()).getParentFile();
        logger.setBasePath(_basePath);
        stifExtractTask.setLogger(logger);
        _log.info("processing stif");
        bundle = new NycFederatedTransitDataBundle();
        bundle.setPath(_basePath);
        stifExtractTask.setBundle(bundle);
        long start = System.currentTimeMillis();
        stifExtractTask.run();
        _log.info("stif processed in "+((System.currentTimeMillis()-start)/1000));
        return stifExtractTask;
    }

    @After
    public void deleteOutput(){
        bundle.getRemarksCsvPath().delete();
        bundle.getTripInfoCsvPath().delete();
        bundle.getTimePointsCsvPath().delete();
        bundle.getRemarksObjPath().delete();
        bundle.getTimePointsObjPath().delete();
        bundle.getTimePointsObjPath().delete();
    }



    @Test
    public void test_all() throws IOException, ClassNotFoundException {
        //todo: write this test so it works piecemeal, cause timepoints for all those is ~65M
        stif_m_0003_2H4283_wkd_closed();
        stif_b_0027x_4H8422_wkd_closed();
        testAgainst_d_0090_921011_sat();
        testAgainst_d_0090_921011_sat();
        testAgainst_d_0090_921011_sat();
        stif_b_0003_4H8417_wkd_closed();
        stif_m_0011_223070_wkd_open();
        stif_Q_50_7H1391_wkd_closed();
        stif_s_0091_623041_wkd_open();

    }

    @Test
    public void stif_s_0091_623041_wkd_open() throws IOException, ClassNotFoundException {
        // adding next trip remark info for ?the first trip interlined into a different stif file?
        // suprised this is the first time that came up, might be alittle different
        testAllBustrekData("stif.s_0091__.623041.wkd.open");
    }

    @Test
    public void stif_Q_50_7H1391_wkd_closed() throws IOException, ClassNotFoundException {
        //bug in trip.getRawRunRoute() where it was still being fed uppercase'd data
        testAllBustrekData("stif.Q_50____.7H1391.wkd.closed");
    }

    @Test
    public void stif_m_0011_223070_wkd_open() throws IOException, ClassNotFoundException {
        //inconsistancy between "" and null, updated RemarkAndRunAndMore.getDestinationTimePoint() to go with "",
        // but tbh empty vals should be null
        //todo: change over to null as blank value
        testAllBustrekData("stif.m_0011__.223070.wkd.open");
    }

    @Test
    public void stif_b_0003_4H8417_wkd_closed() throws IOException, ClassNotFoundException {
        //okay this one is *really weird* and is caused by a bug
        /*
        if a pullout and first revenue trip are given the same start time,
        and the first revenue trip comes first in the file (this is normal behaviour),
        the original script doesn’t mark the trip as PO (though it should)
        eg:
        for this file:
        stif.b_0025__.4H8366.wkd.closed
        the script outputs this: UP_H3-Weekday-007000_B3_101,8,101,VET71,00007000,32763471,
        instead of: UP_H3-Weekday-007000_B3_101,8,101PO,VET71,00007000,32763471,

        the fix is to alter the sort method that is run on the blockNumberToTripRecordMap entry values
        before each block of trips is processed

        todo: i am introducing a temporary break in the code so that the behaviour of BusTime matches the original scripts.
        todo: fix this later please.
        todo: fix it w/ something like:  block_sorting(before_compute_remarks_and_run)->if(this.startTime==that.startTime)->if(this.isPullout?-1:1;
         */
        testAllBustrekData("stif.b_0003__.4H8417.wkd.closed");
    }

    @Test
    public void stif_b_0025_4H8366_wkd_closed() throws IOException, ClassNotFoundException {
        //tests that lastTrip treats Y or C as true
        testAllBustrekData("stif.b_0025__.4H8366.wkd.closed");
    }

    @Test
    public void stif_b_0064_4H8420_wkd_closed() throws IOException, ClassNotFoundException {
        //tests how the reader handles situations where it needs to read all event records
        // before processing trips
        testAllBustrekData("stif.b_0064__.4H8420.wkd.closed");
    }

    @Test
    public void stif_m_0003_2H4283_wkd_closed() throws IOException, ClassNotFoundException {
        testAllBustrekData("stif.m_0003__.2H4283.wkd.closed");
    }

    @Test
    public void stif_b_0027x_4H8422_wkd_closed() throws IOException, ClassNotFoundException {
        testAllBustrekData("stif.b_0027x_.4H8422.wkd.closed");
    }

    @Test
    public void testAgainst_d_0090_921011_sat() throws IOException, ClassNotFoundException {
        testAllBustrekData("stif.d_0090__.921011.sat");
    }

    public void testAllBustrekData(String dataSet) throws IOException, ClassNotFoundException {
        _log.info("\n\ntesting: "+dataSet);
        compareAllDataWithCsvs(initialize(dataSet),dataSet);
        compareAllOutputWithCSV(dataSet);
        CompareAllOutputJarsWithCsv(dataSet);
        deleteOutput();
    }

    public String getAsJson(Object object){
        StringWriter writer = null;
        String output = "";
        try {
            writer = new StringWriter();
            _mapper.writeValue(writer, object);
            output = writer.toString();
        } catch (IOException e) {
            _log.error("exception parsing json " + e, e);
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                _log.error("Error closing writer", e);
            }
            return output;
        }

    }

    private ObjectMapper _mapper;

    protected void setupObjectMapper(){
        _mapper = new ObjectMapper();
        _mapper.registerModule(new JavaTimeModule());
        _mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        _mapper.setTimeZone(Calendar.getInstance().getTimeZone());
    }


    public void compareAllDataWithCsvs(StifExtractTask extractor,String dataset) throws IOException {
        setupObjectMapper();
        String out = getAsJson(extractor.getRemarks());

        compareDataWithCsv(extractor.getRemarks(),"remarks-"+dataset+".csv");
        compareDataWithCsv(extractor.getTripInfo(), "tripInfo-"+dataset+".csv");
        compareDataWithCsv(extractor.getTimePoints(), "timePoints-"+dataset+".csv");
    }

    public void compareAllOutputWithCSV(String dataset) throws IOException {
        compareCsvs("remarks-"+dataset+".csv", bundle.getRemarksCsvPath().getName(), Remark.class);
        _log.info("total memory in use:" + Runtime.getRuntime().totalMemory()/8/1000/1000);
        compareCsvs("tripInfo-"+dataset+".csv", bundle.getTripInfoCsvPath().getName(), TripInfo.class);
        _log.info("total memory in use:" + Runtime.getRuntime().totalMemory()/8/1000/1000);
        compareCsvs("timePoints-"+dataset+".csv", bundle.getTimePointsCsvPath().getName(), TimePoint.class);
        _log.info("total memory in use:" + Runtime.getRuntime().totalMemory()/8/1000/1000);
    }

    public void CompareAllOutputJarsWithCsv(String dataset) throws IOException, ClassNotFoundException {
        compareDataWithCsv(getBustrekDataFromSerializedObj(bundle.getRemarksObjPath()),"remarks-"+dataset+".csv");
        compareDataWithCsv(getBustrekDataFromSerializedObj(bundle.getTripInfoObjPath()),"tripInfo-"+dataset+".csv");
        compareDataWithCsv(getBustrekDataFromSerializedObj(bundle.getTimePointsObjPath()),"timePoints-"+dataset+".csv");
    }

    private void compareDataWithCsv(ArrayList<BustrekDatum> bustrekDatumArray, String bustrekDataSource) throws IOException {
        _log.info("comparing data extracted with source of truth: " + bustrekDataSource);
        compareCsvWithBustrekDatumArray(bustrekDatumArray, bustrekDataSource,bustrekDatumArray.get(0).getClass());
    }

    private void compareCsvs(String sourceOfTruthPath,String myOutputPath, Class bustrekDataType) throws IOException {
        _log.info("comparing csv output: " +myOutputPath +" vs source of truth: " + sourceOfTruthPath);
        compareCsvWithBustrekDatumArray(getBustrekDataFromResourceCSV(myOutputPath,bustrekDataType),
                sourceOfTruthPath,bustrekDataType);
    }

    private ArrayList<BustrekDatum> getBustrekDataFromResourceCSV(String bustrekDataSource, Class bustrekDataType) throws IOException {
        InputStream bustrekDataStream = this.getClass().getResourceAsStream(bustrekDataSource);
        BufferedReader truebustrekDataReader = new BufferedReader(new InputStreamReader(bustrekDataStream));
        int bustrekDataIndex = 0;
        ArrayList<BustrekDatum> trueBustrekData = new ArrayList<BustrekDatum>();
        String trueBustrekDataString = truebustrekDataReader.readLine();
        while (trueBustrekDataString != null) {
            BustrekDatum datum;
            try {
                datum = makeNewBusTrekDatum(trueBustrekDataString, bustrekDataType);
            } catch (Throwable e){
                _log.info("Error creating "+bustrekDataType.getName()+" on line "+bustrekDataIndex+"of file: "+bustrekDataSource);
                throw e;
            }
            trueBustrekData.add(datum);
            trueBustrekDataString = truebustrekDataReader.readLine();
            bustrekDataIndex++;
        }
        return trueBustrekData;
    }

    private ArrayList<BustrekDatum> getBustrekDataFromSerializedObj(File bustrekDataSource) throws IOException, ClassNotFoundException {
        return ObjectSerializationLibrary.readObject(bustrekDataSource);
    }

    private void compareCsvWithBustrekDatumArray(ArrayList<BustrekDatum> bustrekDatumArray, String bustrekDataSource, Class bustrekDataType) throws IOException {
        // todo: comment this line out
        _breakOnThisLineNumber = 27;
        Comparator<BustrekDatum> bustrekDatumComparator = (BustrekDatum::compareTo);
        bustrekDatumArray.sort(bustrekDatumComparator);
        long start = System.currentTimeMillis();
        InputStream bustrekDataStream = this.getClass().getResourceAsStream(bustrekDataSource);
        BufferedReader truebustrekDataReader = new BufferedReader(new InputStreamReader(bustrekDataStream));
        int bustrekDataIndex = 0;
        String trueBustrekDataString = truebustrekDataReader.readLine();
        while (trueBustrekDataString != null) {
            BustrekDatum trueData = makeNewBusTrekDatum(trueBustrekDataString, bustrekDataType);
            if (bustrekDataIndex == _breakOnThisLineNumber) {
                _log.info("put testing flag here, StifExtractTaskTest.java");
            }
            int datumLocation = Collections.binarySearch(bustrekDatumArray,trueData,bustrekDatumComparator);
            try {
                assert (bustrekDatumArray.get(datumLocation).equals(trueData));
            } catch (Throwable e) {
                _log.info("Found error searching for " + trueData.getClass() + ": " + trueData);
                _log.info("current index: " + bustrekDataIndex);
                throw e;
            }
            trueBustrekDataString = truebustrekDataReader.readLine();
//            bustrekDatumArray.remove(datumLocation);
            bustrekDataIndex++;
            if (false) {
                _log.info("");
                _log.info("true data: " + trueData);
                _log.info("match    : " + bustrekDatumArray.get(bustrekDatumArray.indexOf(trueData)));
            }
        }
//        assert (bustrekDatumArray.size() == 0);
        assert (bustrekDatumArray.size() == bustrekDataIndex);
        _log.info(bustrekDataType.getName() + " data compared with "+bustrekDataSource +" in seconds: "+((System.currentTimeMillis()-start)/1000));
    }

    private void compareBusTrekDatumArrays(ArrayList<BustrekDatum> bustrekDatumArray, ArrayList<BustrekDatum> trueBustrekDatumArray) throws IOException {
        // todo: comment this line out
        _breakOnThisLineNumber = 27;
        Comparator<BustrekDatum> bustrekDatumComparator = (BustrekDatum::compareTo);
        bustrekDatumArray.sort(bustrekDatumComparator);
        trueBustrekDatumArray.sort(bustrekDatumComparator);
        long start = System.currentTimeMillis();
        int bustrekDataIndex = 0;
        for(BustrekDatum trueData : trueBustrekDatumArray) {
            if (bustrekDataIndex == _breakOnThisLineNumber) {
                _log.info("put testing flag here, StifExtractTaskTest.java");
            }
            int datumLocation = Collections.binarySearch(bustrekDatumArray,trueData,bustrekDatumComparator);
            try {
                assert (bustrekDatumArray.get(datumLocation).equals(trueData));
            } catch (Throwable e) {
                _log.info("Found error searching for " + trueData.getClass() + ": " + trueData);
                _log.info("current index: " + bustrekDataIndex);
                throw e;
            }
//            bustrekDatumArray.remove(datumLocation);
            bustrekDataIndex++;
            if (false) {
                _log.info("");
                _log.info("true data: " + trueData);
                _log.info("match    : " + bustrekDatumArray.get(bustrekDatumArray.indexOf(trueData)));
            }
        }
//        assert (bustrekDatumArray.size() == 0);
        assert (bustrekDatumArray.size() == bustrekDataIndex);
        _log.info("done in seconds: "+((System.currentTimeMillis()-start)/1000));
    }



    private BustrekDatum makeNewBusTrekDatum(String trueBustrekDataString, Class intendedClass) {
        String[] dataArray = trueBustrekDataString.split(",");

        if (Remark.class.equals(intendedClass)) {
            return new Remark(dataArray);
        } else if (TripInfo.class.equals(intendedClass)) {
            return new TripInfo(dataArray);
        } else if (TimePoint.class.equals(intendedClass)) {
            return new TimePoint(dataArray[0], dataArray[1], dataArray[2]);
        }
        return null;
    }
}