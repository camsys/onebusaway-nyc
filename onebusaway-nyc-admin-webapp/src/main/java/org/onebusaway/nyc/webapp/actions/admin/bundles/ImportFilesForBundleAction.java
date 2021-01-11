/**
 * Copyright (C) 2016 Cambridge Systematics, Inc.
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
package org.onebusaway.nyc.webapp.actions.admin.bundles;

import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.onebusaway.nyc.admin.model.ui.DataValidationMode;
import org.onebusaway.nyc.admin.service.FileService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCAdminActionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Action class used by Transit Data Bundle Utility to import files from a previous build for a new one
 *
 */
@Namespace(value="/admin/bundles")
@Results({
        @Result(name="filePaths", type="json",
                params={"root", "filePaths"}),
        @Result(name="returnMessage", type="json",
                params={"root", "returnMessage"})
})
public class ImportFilesForBundleAction extends OneBusAwayNYCAdminActionSupport {
    private static Logger _log = LoggerFactory.getLogger(ImportFilesForBundleAction.class);

    private String bundleDirectory;
    private int dataset_build_id;
    private String buildName;
    private FileService fileService;
    private List<String> filePaths = new ArrayList<>();
    private String fileToImport;
    private List<String> filesToImport = new ArrayList<>();
    private String ARG_STIF_INPUT_FOLDER = "stif_latest";
    private String ARG_GTFS_INPUT_FOLDER = "gtfs_latest";
    private String ARG_TRANSFORMATIONS_INPUT_FOLDER = "transformations_latest";
    private String ARG_STIF_OUTPUT_FOLDER = "stif";
    private String ARG_GTFS_OUTPUT_FOLDER = "gtfs";
    private String ARG_TRANSFORMATIONS_OUTPUT_FOLDER = "transformations";
    private String ARG_STIF_IDENTIFIER = "stif_latest";
    private String ARG_GTFS_IDENTIFIER = "gtfs_latest";
    private String ARG_STIF_FILE_NAME_IDENTIFIER = "STIF";
    private String ARG_TRANSFORMATIONS_FILE_NAME_IDENTIFIER = "json";
    private String returnMessage;


    public void setBundleDirectory(String bundleDirectory) {
        this.bundleDirectory = bundleDirectory;
    }

    public void setDataset_build_id(int dataset_build_id) {
        this.dataset_build_id = dataset_build_id;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    public void setFilesToImport(List filesToImport){this.filesToImport = (List<String>) filesToImport;}

    public void setFileToImport(String fileToImport){this.fileToImport =  fileToImport;}

    public void setARG_STIF_INPUT_FOLDER(String ARG_STIF_FOLDER){this.ARG_STIF_INPUT_FOLDER = ARG_STIF_FOLDER;}
    public void setARG_GTFS_INPUT_FOLDER(String ARG_GTFS_FOLDER){this.ARG_GTFS_INPUT_FOLDER = ARG_GTFS_FOLDER;}
    public void setARG_TRANSFORMATIONS_INPUT_FOLDER(String ARG_TRANSFORMATIONS_FOLDER){this.ARG_TRANSFORMATIONS_INPUT_FOLDER = ARG_TRANSFORMATIONS_FOLDER;}
    public void setARG_STIF_OUTPUT_FOLDER(String ARG_STIF_FOLDER){this.ARG_STIF_OUTPUT_FOLDER = ARG_STIF_FOLDER;}
    public void setARG_GTFS_OUTPUT_FOLDER(String ARG_GTFS_FOLDER){this.ARG_GTFS_OUTPUT_FOLDER = ARG_GTFS_FOLDER;}
    public void setARG_TRANSFORMATIONS_OUTPUT_FOLDER(String ARG_TRANSFORMATIONS_FOLDER){this.ARG_TRANSFORMATIONS_OUTPUT_FOLDER = ARG_TRANSFORMATIONS_FOLDER;}
    public void setARG_STIF_FILE_NAME_IDENTIFIER(String ARG_STIF_FILE_NAME_IDENTIFIER){this.ARG_STIF_FILE_NAME_IDENTIFIER = ARG_STIF_FILE_NAME_IDENTIFIER;}
    public void setARG_TRANSFORMATIONS_FILE_NAME_IDENTIFIER(String TRANSFORMATIONS_FILE_NAME_IDENTIFIER){this.ARG_TRANSFORMATIONS_FILE_NAME_IDENTIFIER = TRANSFORMATIONS_FILE_NAME_IDENTIFIER;}



    public String getReturnMessage(){return returnMessage;}

    public List<String> getFilePaths(){
        return filePaths;
    }

    @Autowired
    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    public String requestFilePaths() {
        filePaths.clear();
        String dirName = bundleDirectory +"/" +
                fileService.getBuildPath()+"/" +
                buildName + "/"+
                "inputs" + "/";
        _log.info("existingBuildList called for path=" + fileService.getBucketName()+"/"+ dirName);
        List<String> existingFiles = fileService.listFiles( dirName, 150);

        _log.info("existingBuildList called for path=" + fileService.getBucketName()+"/"+ dirName + ARG_STIF_OUTPUT_FOLDER + "/");
        List<String> stifExistingFiles = fileService.listFiles( dirName + ARG_STIF_OUTPUT_FOLDER + "/", 150);

        _log.info("existingBuildList called for path=" + fileService.getBucketName()+"/"+ dirName + ARG_GTFS_OUTPUT_FOLDER + "/");
        List<String> gtfsExistingFiles = fileService.listFiles( dirName + ARG_GTFS_OUTPUT_FOLDER + "/", 150);

        _log.info("existingBuildList called for path=" + fileService.getBucketName()+"/"+ dirName + ARG_TRANSFORMATIONS_OUTPUT_FOLDER + "/");
        List<String> transformationsExistingFiles = fileService.listFiles( dirName + ARG_TRANSFORMATIONS_OUTPUT_FOLDER + "/", 150);

        for(String fileName: existingFiles) {
//            String[] fileNameSplit = fileName.split("/");
//            filePaths.add(fileNameSplit[fileNameSplit.length-1]);
            filePaths.add(fileName);
        }
        for(String fileName: gtfsExistingFiles) {
            filePaths.add(fileName);
        }
        for(String fileName: stifExistingFiles) {
            filePaths.add(fileName);
        }
        for(String fileName: transformationsExistingFiles) {
            filePaths.add(fileName);
        }
        return "filePaths";
    }

    private List<String> getExistingFiles(){
        return fileService.listFiles( bundleDirectory +"/" +fileService.getBuildPath()+"/" + buildName + "/"+ "inputs" + "/", 150);
    }

    public String clearFiles(){
        clearLatestFolder(ARG_GTFS_INPUT_FOLDER);
        clearLatestFolder(ARG_STIF_INPUT_FOLDER);
        clearLatestFolder(ARG_TRANSFORMATIONS_INPUT_FOLDER);
        returnMessage = "Deleted pre-existing transformations, stif and gtfs latest files";
        return "returnMessage";
    }

    public String importFile(){

        Map<String, String> folders = new HashMap<>();
        folders.put(ARG_STIF_OUTPUT_FOLDER,ARG_STIF_INPUT_FOLDER);
        folders.put(ARG_GTFS_OUTPUT_FOLDER, ARG_GTFS_INPUT_FOLDER);
        folders.put(ARG_TRANSFORMATIONS_OUTPUT_FOLDER,ARG_TRANSFORMATIONS_INPUT_FOLDER);

        String[] filePathParts = fileToImport.split("/");
        String fileName = filePathParts[filePathParts.length - 1];
        returnMessage = "Copied " + fileName;

        for (Map.Entry<String,String> folderEntry : folders.entrySet()) {
            if (fileToImport.contains(folderEntry.getKey())) {
                fileService.copyS3Object(fileToImport, bundleDirectory + "/" + folderEntry.getValue() + "/" + fileName);
                return "returnMessage";
            }
        }

        String folder = ARG_GTFS_INPUT_FOLDER;
        if(fileName.toLowerCase().contains(ARG_STIF_FILE_NAME_IDENTIFIER.toLowerCase())){
            folder = ARG_STIF_INPUT_FOLDER;
        }
        if(fileName.toLowerCase().contains(ARG_TRANSFORMATIONS_FILE_NAME_IDENTIFIER.toLowerCase())){
            folder = ARG_TRANSFORMATIONS_INPUT_FOLDER;
        }
        fileService.copyS3Object(fileToImport, bundleDirectory +"/" + folder + "/" + fileName);
        return "returnMessage";
    }


    public void clearLatestFolder(String folder){
        String targetFolder = bundleDirectory +"/" + folder + "/";
        List<String> files = fileService.listFiles( targetFolder, 150);
        files.remove(targetFolder);
        for (String file : files){
            if (!file.equals(targetFolder)) {
                fileService.deleteObject(file);
            }
        }
    }

}
