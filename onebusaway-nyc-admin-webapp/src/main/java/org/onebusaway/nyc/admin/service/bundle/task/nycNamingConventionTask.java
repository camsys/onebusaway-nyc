package org.onebusaway.nyc.admin.service.bundle.task;

import org.onebusaway.gtfs.services.GenericMutableDao;
import org.onebusaway.nyc.admin.service.bundle.task.save.SaveGtfsTask;
import org.onebusaway.nyc.admin.util.FileUtils;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.MultiCSVLogger;
import org.onebusaway.nyc.util.impl.FileUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.FilenameFilter;

public class nycNamingConventionTask  implements Runnable {
    private static Logger _log = LoggerFactory.getLogger(SaveGtfsTask.class);
    private File gtfsDirectory;
    private File stifDirectory;


    @Autowired
    public void setGtfsDirectory(File gtfsDirectory){this.gtfsDirectory = gtfsDirectory;}

    @Autowired
    public void setStifDirectory(File stifDirectory){this.stifDirectory = stifDirectory;}


    @Override
    public void run() {
        try {
            // go through each file in gtfs directory and run cleanup to zip them
            for (File gtfs : gtfsDirectory.listFiles(// file filter that only recognizes folders
            )) {
                cleanup(gtfs);
            }
            // go through each file in stif directory and run cleanup to zip them
            File[] stifFolders = stifDirectory.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File current, String name) {
                    return new File(current, name).isDirectory();
                }
            });
            for (File stif : stifFolders) {
                cleanup(stif);
            }

            // match them up somehow and rename them~
            // set up some defaults but use the TDM to allow for nameing convention changes


        } catch (Throwable ex) {
            throw new IllegalStateException("error loading gtfs", ex);
        }

    }


    private void cleanup(File gtfsFile) throws Exception {
        FileUtility fu = new FileUtility();
        FileUtils fs = new FileUtils();

        _log.info("gtfsBundle.getPath=" + gtfsFile.getPath());
        String oldGtfsName = gtfsFile.getPath().toString();
        // create a new zip file

        String newGtfsName = fs.parseDirectory(oldGtfsName) + File.separator
                + fs.parseFileNameMinusExtension(oldGtfsName) + ".zip";

        String basePath = fs.parseDirectory(oldGtfsName);
        String includeExpression = ".*\\.txt";
        fu.zip(newGtfsName, basePath, includeExpression);
        fu.deleteFilesInFolder(gtfsFile.getAbsolutePath(), includeExpression);
        gtfsFile.delete();
    }

}

