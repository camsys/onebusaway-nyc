package org.onebusaway.nyc.admin.service.bundle.task;

import com.google.common.collect.Lists;
import org.onebusaway.gtfs.services.GenericMutableDao;
import org.onebusaway.nyc.admin.model.BundleRequestResponse;
import org.onebusaway.nyc.admin.model.json.Bundle;
import org.onebusaway.nyc.admin.model.json.BundleFile;
import org.onebusaway.nyc.admin.service.bundle.task.save.SaveGtfsTask;
import org.onebusaway.nyc.admin.util.FileUtils;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.MultiCSVLogger;
import org.onebusaway.nyc.util.impl.FileUtility;
import org.onebusaway.transit_data_federation.services.FederatedTransitDataBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;
import java.util.stream.Collectors;

public class nycNamingConventionTask  implements Runnable {
    private static Logger _log = LoggerFactory.getLogger(SaveGtfsTask.class);
    BundleRequestResponse requestResponse;
    private File stifDirectory;
    private static final Map<String, String> gtfsAbbrToZone =
            Arrays.stream(new String[][] {
                    {"BX","bronx"},
                    {"B","brooklyn"},
                    {"M","manhattan"},
                    {"MTABC","mtabc"},
                    {"Q","queens"},
                    {"S","staten-island"},
            }).collect(Collectors.toMap(mapEntry -> mapEntry[0], mapEntry -> mapEntry[1]));


    @Autowired
    public void setRequestResponse(BundleRequestResponse requestResponse) {
        this.requestResponse = requestResponse;
    }

    @Autowired
    public void setStifDirectory(File stifDirectory){this.stifDirectory = stifDirectory;}


    @Override
    public void run() {
        try {

            Map <File, File> filesToRename = new HashMap<>();
            List<File> gtfsFiles = new ArrayList<>();
            List<File> originalStifFiles = new ArrayList<>();

            for (String stifPath : requestResponse.getResponse().getStifZipList()){
                originalStifFiles.add(new File(stifPath));
            }

            for (String gtfsPath : requestResponse.getResponse().getGtfsList()){
                gtfsFiles.add(new File(gtfsPath));
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
            List<File> stifFiles = Lists.newArrayList(stifDirectory.listFiles());

            gtfsLoop : for (File gtfsFile : gtfsFiles){
                if(gtfsFile.getName().contains("GTFS")){
                    String sufix = gtfsFile.getName().substring(4);
                    String gtfsAbbr = sufix.substring(1).replace("SURFACE_", "").split("_")[0].replace(".zip","");
                    String stifZone = gtfsAbbrToZone.get(gtfsAbbr);
                    for (File stifFile : stifFiles){
                        if(stifZone != null && stifFile.getName().contains(stifZone)){
                            stifFiles.remove(stifFile);
                            filesToRename.put(gtfsFile,new File(gtfsFile.getParent() + "/" + "google_transit_"+stifZone));
                            filesToRename.put(stifFile,new File(stifFile.getParent() + "/" + "STIF"+sufix));
                            continue gtfsLoop;
                        }
                    }
                }
            }

            originalStifLoop : for (File originalStifFile : originalStifFiles){
                if(originalStifFile.getName().contains("STIF")){
                    String sufix = originalStifFile.getName().substring(4);
                    String originalStifAbbr = sufix.substring(1).replace("SURFACE_", "").split("_")[0].replace(".zip","");
                    String stifZone = gtfsAbbrToZone.get(originalStifAbbr);
                    for (File stifFile : stifFiles){
                        if(stifZone!= null && stifFile.getName().contains(stifZone)){
                            stifFiles.remove(stifFile);
                            filesToRename.put(stifFile,new File(stifFile.getParent() + "/" + "STIF"+sufix));
                            continue originalStifLoop;
                        }
                    }
                }
            }

            for (Map.Entry<File,File> entry : filesToRename.entrySet()){
                entry.getKey().renameTo(entry.getValue());
            }

        } catch (Throwable ex) {
            throw new IllegalStateException("error loading gtfs", ex);
        }

    }

    private File cleanup(File file) throws Exception {
        FileUtility fu = new FileUtility();
        FileUtils fs = new FileUtils();

        String oldFileName = file.getPath().toString();
        // create a new zip file

        String newFileName = fs.parseDirectory(oldFileName) + File.separator
                + fs.parseFileNameMinusExtension(oldFileName) + ".zip";

        String basePath = fs.parseDirectory(oldFileName);
        fu.zipRecursively(newFileName, basePath);
        FileSystemUtils.deleteRecursively(file);
        return new File(newFileName);
    }

}

