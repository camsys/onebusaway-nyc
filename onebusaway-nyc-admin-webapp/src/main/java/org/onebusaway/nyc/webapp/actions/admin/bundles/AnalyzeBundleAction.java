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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Action class used by Transit Data Bundle Utility to compare two bundles.
 *
 */
@Namespace(value="/admin/bundles")
@Results({
        @Result(name="zones", type="json",
                params={"root", "listOfZones"}),
        @Result(name="zoneData", type="json",
                params={"root", "tripCountByDate"}),
        @Result(name="diffResult", type="json",
                params={"root", "combinedDiffs"})
})
public class AnalyzeBundleAction extends OneBusAwayNYCAdminActionSupport {
    private static Logger _log = LoggerFactory.getLogger(AnalyzeBundleAction.class);
    private static final int MAX_RESULTS = -1;
    private static final String OUTPUTS = "outputs";
    private String tripCountByZoneDataLocation = "TripCountByZoneData";
    private List<String> diffResult = new ArrayList<String>();
    private Map<String, List> combinedDiffs;
    private List<String> listOfZones = new ArrayList<String>();
    private Map<String, Integer> tripCountByDate = new HashMap<>();
    private int dataset_build_id;
    private String datasetName;
    private String buildName;
    private String zone;
    private FileService fileService;


    @Autowired
    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public void setDataset_build_id(int dataset_1_build_id) {
        this.dataset_build_id = dataset_build_id;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    public void setZone(String zone){
        this.zone = zone;
    }

    public void setTripCountByZoneDataLocation(String tripCountByZoneDataLocation){
        this.tripCountByZoneDataLocation = tripCountByZoneDataLocation;
    }

    public List<String> getListOfZones(){
        return listOfZones;
    }

    public Map<String,Integer> getTripCountByDate(){
        return tripCountByDate;
    }

    public String getZoneData() {

        tripCountByDate.clear();
        String TripCountsByZoneFile = datasetName + "/builds/" + buildName + "/" + OUTPUTS + "/" + tripCountByZoneDataLocation + "/" + zone + ".csv";

        // afterwords: Use a fileservice to output these to the right place!
        InputStream TripCountsByZoneStream = fileService.get(TripCountsByZoneFile);

        List<String> lines = fileToLines(TripCountsByZoneStream);

        for (String line : lines){
            String[] lineParts = line.split(",");
            tripCountByDate.put(lineParts[0],Integer.parseInt(lineParts[1]));
        }

        return "zoneData";
    }

    public String getZoneList(){

        listOfZones.clear();

        String buildLocation = datasetName + "/builds/" +
                buildName + "/" + OUTPUTS + "/" +
                tripCountByZoneDataLocation + "/";

        _log.info("existingZones called for path=" + fileService.getBucketName()+"/" + buildLocation);
        List<String> existingZones = fileService.listObjects( buildLocation, MAX_RESULTS);
        if(existingZones == null){
            return null;
        }
        int i = 1;
        for(String zone: existingZones) {
            String[] buildSplit = zone.split("/");
            listOfZones.add(buildSplit[buildSplit.length-1].replace(".csv",""));
        }

        return "zones";
    }

    private List<String> fileToLines(InputStream inputStream) {
        List<String> lines = new LinkedList<String>();
        String line = "";
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
            in.readLine();
            while ((line = in.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {}
        return lines;
    }
}
