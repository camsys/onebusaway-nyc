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
    public String ARG_MODIFIED = "modified";


    @Autowired
    public void setRequestResponse(BundleRequestResponse requestResponse) {
        this.requestResponse = requestResponse;
    }

    @Autowired
    public void setStifDirectory(File stifDirectory){this.stifDirectory = stifDirectory;}

    public void setARG_MODIFIED(String ARG_MODIFIED){this.ARG_MODIFIED = ARG_MODIFIED;}


    @Override
    public void run() {
        _log.info("Starting nycNamingConventionTask");
        try {

            Map <File, File> filesToRename = new HashMap<>();
            List<File> gtfsFiles = new ArrayList<>();
            List<File> originalStifFiles = new ArrayList<>();

            for (String stifPath : requestResponse.getResponse().getStifZipList()){
                originalStifFiles.add(new File(stifPath));
            }

            File[] gtfsModified = new File(requestResponse.getResponse().getBundleOutputDirectory() + "/" + ARG_MODIFIED).listFiles();
            if (gtfsModified != null) {
                for (File gtfsFile : gtfsModified) {
                    gtfsFiles.add(gtfsFile);
                }
            }
            // go through each file in stif directory and run cleanup to zip them
            File[] stifFolders = stifDirectory.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File current, String name) {
                    return new File(current, name).isDirectory();
                }
            });
            _log.info("Zipping Stif Files");
            for (File stif : stifFolders) {
                cleanup(stif);
            }

            // match them up somehow and rename them~
            // set up some defaults but use the TDM to allow for nameing convention changes
            List<File> stifFiles = Lists.newArrayList(stifDirectory.listFiles());
            _log.info("Renaming based on old Gtfs Files");

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
            _log.info("Renaming based on old Stif Files");
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
            requestResponse.getResponse().setException((Exception) ex);
            throw new IllegalStateException("error loading gtfs", ex);
        }
        finally {
            _log.info("exiting nycNamingConventionTask");
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
        _log.info("zipping "+ file.getName());
        fu.zipRecursively(newFileName, file.getAbsolutePath(), true);
        _log.info("zipped "+ file.getName());
        FileSystemUtils.deleteRecursively(file);
        _log.info("deleated old "+ file.getName());
        return new File(newFileName);
    }

}

