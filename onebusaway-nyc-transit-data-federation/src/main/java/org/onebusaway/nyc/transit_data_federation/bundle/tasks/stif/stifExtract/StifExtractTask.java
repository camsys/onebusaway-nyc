package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract;

import org.hibernate.type.SerializationException;
import org.onebusaway.nyc.transit_data_federation.bundle.model.NycFederatedTransitDataBundle;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.MultiCSVLogger;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.reader.BasicStifReader;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract.model.BustrekDatum;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifImport.impl.StifTaskBundleWriterImpl;
import org.onebusaway.utility.ObjectSerializationLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StifExtractTask implements Runnable{
    //todo: clean up variables, methods, etc to match style norms

    private Logger _log = LoggerFactory.getLogger(StifExtractTask.class);

    private StifExtractCSVWriter stifExtractCSVWriter;

    private String remarksFileName = "remarks.csv";
    private String tripInfoFileName = "tripInfo.csv";
    private String timePointsFileName = "timepoints.csv";


    private BustrekDataExtractorStifReader bustrekDataExtractor = new BustrekDataExtractorStifReader();

    @Override
    public void run() {
        _log.info("Starting StifExtractTask");
        load(_stifPaths,bustrekDataExtractor);
        exportStifInfo();
        _log.info("finishing StifExtractTask");
    }

    public void exportStifInfo(){
        writeStifInfoAsCsv();
        serializeBusTrekData();
    }



    private void writeStifInfoAsCsv(){
        stifExtractCSVWriter.writeBusTrekData(bustrekDataExtractor.getRemarks(), _bundle.getRemarksCsvPath());
        stifExtractCSVWriter.writeBusTrekData(bustrekDataExtractor.getTripInfo(), _bundle.getTripInfoCsvPath());
        stifExtractCSVWriter.writeBusTrekData(bustrekDataExtractor.getTimePoints(), _bundle.getTimePointsCsvPath());
    }

    private void serializeBusTrekData() {
        try {
            ObjectSerializationLibrary.writeObject(_bundle.getRemarksObjPath(), bustrekDataExtractor.getRemarks());
            ObjectSerializationLibrary.writeObject(_bundle.getTripInfoObjPath(), bustrekDataExtractor.getTripInfo());
            ObjectSerializationLibrary.writeObject(_bundle.getTimePointsObjPath(), bustrekDataExtractor.getTimePoints());
        } catch (IOException e){
            _log.error(String.valueOf(e));
        }
    }



    public void load(List<File> stifPaths, BasicStifReader stifReader){
        for (File path : stifPaths) {
            loadStif(path, stifReader);
        }
    }

    public void loadStif(File path, BasicStifReader stifReader) {
        // Exclude files and directories like .svn
        if (path.getName().startsWith("."))
            return;

        if (path.isDirectory()) {
            for (String filename : path.list()) {
                File contained = new File(path, filename);
                loadStif(contained, stifReader);
            }
        } else {
            _log.info("total memory in use:" + Runtime.getRuntime().totalMemory()/8/1000/1000);
            stifReader.run(path);
            stifReader.clear();
            _log.info("ran "+path.getName());
        }
    }

    /**
     * The path of the directory containing STIF files to process
     */
    private List<File> _stifPaths = new ArrayList<File>();
    public void setStifPath(File path) {
        _stifPaths.add(path);
    }

    public void setStifPaths(List<File> paths) {
        _stifPaths.addAll(paths);
    }

    public List<File> getStifPaths() {
        return _stifPaths;
    }

    @Autowired
    public void setLogger(MultiCSVLogger logger) {
        stifExtractCSVWriter = new StifExtractCSVWriter(logger);
    }

    @Autowired
    private NycFederatedTransitDataBundle _bundle;

    public ArrayList<BustrekDatum> getRemarks(){
        return bustrekDataExtractor.getRemarks();
    }
    public ArrayList<BustrekDatum> getTripInfo(){
        return bustrekDataExtractor.getTripInfo();
    }
    public ArrayList<BustrekDatum> getTimePoints(){
        return bustrekDataExtractor.getTimePoints();
    }

    public void setBundle(NycFederatedTransitDataBundle bundle){
        _bundle=bundle;
    }

    public void setTripInfoFileName(String tripInfoFileName) {
        this.tripInfoFileName = tripInfoFileName;
    }

    public String getTripInfoFileName() {
        return tripInfoFileName;
    }

    public void setRemarksFileName(String remarksFileName) {
        this.remarksFileName = remarksFileName;
    }

    public String getRemarksFileName() {
        return remarksFileName;
    }

    public void setTimePointsFileName(String timePointsFileName) {
        this.timePointsFileName = timePointsFileName;
    }

    public String getTimePointsFileName() {
        return timePointsFileName;
    }
}
