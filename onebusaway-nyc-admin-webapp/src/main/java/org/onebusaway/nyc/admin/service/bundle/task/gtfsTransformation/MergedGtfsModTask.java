package org.onebusaway.nyc.admin.service.bundle.task.gtfsTransformation;

import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs_transformer.TransformSpecificationException;
import org.onebusaway.king_county_metro_gtfs.model.PatternPair;
import org.onebusaway.nyc.admin.util.FileUtils;
import org.onebusaway.nyc.util.impl.FileUtility;
import org.onebusaway.transit_data_federation.bundle.model.GtfsBundle;
import org.onebusaway.gtfs_transformer.GtfsTransformer;
import org.onebusaway.gtfs_transformer.GtfsTransformerLibrary;
import org.onebusaway.gtfs_transformer.factory.TransformFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MergedGtfsModTask implements Runnable{
    private static Logger _log = LoggerFactory.getLogger(BaseModTask.class);

    private File _gtfsPath;
    private String _transformation;
    @Autowired
    public void setGtfsPath(File gtfsPath) {
        _gtfsPath = gtfsPath;
    }
    @Autowired
    public void setTransformation(String transformation) {
        _transformation = transformation;
    }

    @Override
    public void run() {
        GtfsTransformer mod = new GtfsTransformer();
        // add support for KCM Pattern Pairs
//        mod.getReader().getEntityClasses().add(PatternPair.class);
//        mod.getWriter().getEntityClasses().add(PatternPair.class);

        //TransformFactory factory = mod.getTransformFactory();
        // the transformer may be called twice causing erroneous duplicate messages
        mod.getReader().setOverwriteDuplicates(true);

        //addAgencyMappings(mod.getReader(), gtfsBundle);

        // add models outside the default namespace
        //factory.addEntityPackage("org.onebusaway.king_county_metro_gtfs.model");

        File outputDirectory = new File(_gtfsPath.getParent()+"/tmp");
        outputDirectory.mkdir();

        List<File> paths = new ArrayList<File>();
        paths.add(_gtfsPath);
        _log.info("transformer path=" + _gtfsPath + "; output="
                + outputDirectory + " for transformation=" + _transformation);
        mod.setGtfsInputDirectories(paths);
        mod.setOutputDirectory(outputDirectory);
        try {
            GtfsTransformerLibrary.configureTransformation(mod, _transformation);
            _log.info("running...");
            mod.run();
            _log.info("done!");
        } catch (TransformSpecificationException e) {
            e.printStackTrace();
            _log.error("Reading transfomation failed in MergedGtfsModTask",e);
        } catch (Exception e) {
            e.printStackTrace();
            _log.error("Transformation failed in MergedGtfsModTask",e);
        }
        try {
            cleanup(outputDirectory,_gtfsPath);
        } catch (Exception e){
            _log.error("Cleanup failed in MergedGtfsModTask",e);
        }

    }


    private void cleanup(File gtfsDirectory, File gtfsToReplace) throws Exception {
        FileUtility fu = new FileUtility();
        FileUtils fs = new FileUtils();
        gtfsToReplace.delete();
        String includeExpression = ".*\\.txt";
        fu.zip(gtfsToReplace.getPath(), gtfsDirectory.getPath(), includeExpression);
        fu.deleteFilesInFolder(gtfsDirectory.getPath(), includeExpression);
        gtfsDirectory.delete();
    }
}
