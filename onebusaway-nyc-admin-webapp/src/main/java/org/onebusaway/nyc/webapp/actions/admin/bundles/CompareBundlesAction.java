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


import java.io.*;
import java.net.URL;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.onebusaway.nyc.admin.model.ui.DataValidationDirectionCts;
import org.onebusaway.nyc.admin.model.ui.DataValidationHeadsignCts;
import org.onebusaway.nyc.admin.model.ui.DataValidationMode;
import org.onebusaway.nyc.admin.model.ui.DataValidationRouteCounts;
import org.onebusaway.nyc.admin.model.ui.DataValidationStopCt;
import org.onebusaway.nyc.admin.service.DiffService;
import org.onebusaway.nyc.admin.service.FileService;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.admin.service.RouteParserService;
import org.onebusaway.nyc.admin.service.bundle.impl.DailyRouteParserServiceImpl;
import org.onebusaway.nyc.admin.service.bundle.impl.FixedRouteParserServiceImpl;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCAdminActionSupport;
import org.onebusaway.util.services.configuration.ConfigurationServiceClient;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCAdminActionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Action class used by Transit Data Bundle Utility to compare two bundles.
 *
 */
@Namespace(value="/admin/bundles")
@Results({
        @Result(name="diffResult", type="json",
                params={"root", "combinedDiffs"})
})
public class CompareBundlesAction extends OneBusAwayNYCAdminActionSupport {
    private static Logger _log = LoggerFactory.getLogger(CompareBundlesAction.class);
    private static final long serialVersionUID = 1L;
    private static final char ID_SEPARATOR = '_';
    private static final int MAX_STOP_CT = 200;

    private boolean useArchived;
    private String datasetName;
    private int dataset_1_build_id;
    private String buildName;
    private String buildDate;
    private String datasetName2;
    private int dataset_2_build_id;
    private String buildName2;
    private String buildDate2;
    private FileService fileService;
    private List<String> diffResult = new ArrayList<String>();
    private Map<String, List> combinedDiffs;
    private DiffService diffService;
    @Autowired
    private FixedRouteParserServiceImpl _fixedRouteParserService;
    @Autowired
    private DailyRouteParserServiceImpl _dailyRouteParserService;

    public boolean isUseArchived() {
        return useArchived;
    }

    public void setUseArchived(boolean useArchived) {
        this.useArchived = useArchived;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public void setDataset_1_build_id(int dataset_1_build_id) {
        this.dataset_1_build_id = dataset_1_build_id;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    public void setBuildDate(String buildDate) {this.buildDate = buildDate;}

    public void setDatasetName2(String datasetName2) {
        this.datasetName2 = datasetName2;
    }

    public void setDataset_2_build_id(int dataset_2_build_id) {
        this.dataset_2_build_id = dataset_2_build_id;
    }

    public void setBuildName2(String buildName2) {
        this.buildName2 = buildName2;
    }

    public void setBuildDate2(String buildDate2) {this.buildDate2 = buildDate2; }

    @Autowired
    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    public Map<String, List> getCombinedDiffs() {
        return combinedDiffs;
    }

    @Autowired
    private ConfigurationServiceClient _configurationServiceClient;

    @Autowired
    public void setDiffService(DiffService diffService) {
        this.diffService = diffService;
    }

    public String diffResult() {
        String gtfsStatsFile1 = datasetName + "/builds/" + buildName + "/outputs/gtfs_stats.csv";
        String gtfsStatsFile2 = datasetName2 + "/builds/"
                + buildName2 + "/outputs/gtfs_stats.csv";
        diffResult.clear();

        // afterwords: Use a fileservice to output these to the right place!
        InputStream gtfsStatsFile1Stream = fileService.get(gtfsStatsFile1);
        InputStream gtfsStatsFile2Stream = fileService.get(gtfsStatsFile2);


        diffResult = diffService.diff(gtfsStatsFile1, gtfsStatsFile2, gtfsStatsFile1Stream, gtfsStatsFile2Stream);

        // Added code to compare Fixed Route Date Validation reports from the
        // two specified bundles and builds

        List<DataValidationMode> fixedRouteDiffs = compareFixedRouteValidationsViaInputStream(
                datasetName, buildName, datasetName2, buildName2);
        combinedDiffs = new HashMap<String, List>();
        combinedDiffs.put("diffResults", diffResult);
        combinedDiffs.put("fixedRouteDiffs", fixedRouteDiffs);
        return "diffResult";
    }

    private List<DataValidationMode> compareFixedRouteValidationsViaInputStream(
            String datasetName, String buildName,
            String datasetName2, String buildName2) {
        List<DataValidationMode> currentModes;
        List<DataValidationMode> selectedModes;

        long startTime = (new Date()).getTime();
        long buildModeTime1 = 0L;
        long buildModeTime2 = 0L;

        boolean isFixed;

        String currentValidationReportPath;

        String selectedValidationReportPath;

        RouteParserService routeParser;
        if (buildDate == null || buildDate2 == null || buildDate.equals("") || buildDate2.equals("")) {
            currentValidationReportPath = datasetName + "/builds/" + buildName
                    + "/outputs/fixed_route_validation.csv";
            selectedValidationReportPath = datasetName2 + "/builds/"
                    + buildName2 + "/outputs/fixed_route_validation.csv";
            routeParser = _fixedRouteParserService;
            isFixed = true;
        }
        else {
            currentValidationReportPath = datasetName + "/builds/" + buildName + "/outputs/DailyDataValidation/"+buildDate + ".csv";
            selectedValidationReportPath = datasetName2 + "/builds/"
                    + buildName2 + "/outputs/DailyDataValidation/"+buildDate2 + ".csv";
            routeParser = _dailyRouteParserService;
            isFixed = false;
        }
        InputStream currentValidationReportInputStream = fileService.get(currentValidationReportPath);
        InputStream selectedValidationReportInputStream = fileService.get(selectedValidationReportPath);

        // parse input files
        currentModes
                = routeParser.parseRouteReportInputStream(currentValidationReportInputStream,currentValidationReportPath);
        selectedModes
                = routeParser.parseRouteReportInputStream(selectedValidationReportInputStream,selectedValidationReportPath);


        // compare and get diffs
        List<DataValidationMode> fixedRouteDiffs
                = findFixedRouteDiffs(currentModes, selectedModes, isFixed);

        long totalTime = (new Date()).getTime() -startTime ;

        String buildTimeMsg1 = ("Elapsed time for building mode for first dataset was "
                + buildModeTime1/(60*1000) + " min ")
                + (buildModeTime1/1000)%60 + " sec";
        String buildTimeMsg2 = ("Elapsed time for building mode for second dataset was "
                + buildModeTime2/(60*1000) + " min ")
                + (buildModeTime2/1000)%60 + " sec";
        String totalTimeMsg = ("Total elapsed time for Fixed Route Comparison report was "
                + totalTime/(60*1000) + " min ")
                + (totalTime/1000)%60 + " sec";
        _log.info(buildTimeMsg1);
        _log.info(buildTimeMsg2);
        _log.info(totalTimeMsg);
        return  fixedRouteDiffs;
    }



    private LocalDate getFirstDay(int dayOfWeek, LocalDate startDate) {
        int old = startDate.getDayOfWeek();
        if (dayOfWeek < old) {
            dayOfWeek += 7;
        }
        return startDate.plusDays(dayOfWeek - old);
    }

    /**
     * Find the differences between two Lists of modes
     *
     * @param currentModes
     * @param selectedModes
     * @return
     */
    private List<DataValidationMode> findFixedRouteDiffs(
            List<DataValidationMode> currentModes,
            List<DataValidationMode> selectedModes, boolean isFixed) {

        List<DataValidationMode> fixedRouteDiffs = new ArrayList<>();
        if (currentModes != null && selectedModes == null) {
            return currentModes;
        }
        if (currentModes == null && selectedModes != null) {
            return selectedModes;
        }
        if (currentModes == null && selectedModes == null) {
            return fixedRouteDiffs;
        }

        for (DataValidationMode currentMode : currentModes) {
            // Check if this mode exists in selectedModes
            DataValidationMode diffMode = null;
            if (currentMode == null) continue;
            String modeName = currentMode.getModeName();
            for (DataValidationMode selectedMode : selectedModes) {
                if (modeName.equals(selectedMode.getModeName())) {
                    selectedModes.remove(selectedMode);
                    diffMode = compareModes(currentMode, selectedMode, isFixed);
                    break;
                }
            }
            if (diffMode == null && currentMode != null) {
                currentMode.setSrcCode("1");
                diffMode = currentMode;
            }
            if (diffMode.getRoutes().size() > 0) {
                fixedRouteDiffs.add(diffMode);
            }
        }
        if (selectedModes.size() > 0) {
            for (DataValidationMode selectedMode : selectedModes) {
                selectedMode.setSrcCode("2");
                fixedRouteDiffs.add(selectedMode);
            }
        }
        return fixedRouteDiffs;
    }

    private DataValidationMode compareModes(
            DataValidationMode currentMode, DataValidationMode selectedMode, boolean isFixed) {

        DataValidationMode diffMode = new DataValidationMode();
        diffMode.setModeName(currentMode.getModeName());
        diffMode.setRoutes(new TreeSet<DataValidationRouteCounts>());

        for (DataValidationRouteCounts currentRoute : currentMode.getRoutes()) {
            // Check if this route exists in selectedMode
            DataValidationRouteCounts diffRoute = null;
            String routeNum = currentRoute.getRouteNum();
            String routeName = currentRoute.getRouteName();
            for (DataValidationRouteCounts selectedRoute : selectedMode.getRoutes()) {
                if (routeNum.equals(selectedRoute.getRouteNum())) {
                    selectedMode.getRoutes().remove(selectedRoute);
                    if (routeName.equals(selectedRoute.getRouteName())) {
                        diffRoute = compareRoutes(currentRoute, selectedRoute, isFixed);
                    } else {    // Route name changed, but not route number.
                        currentRoute.setSrcCode("1");
                        selectedRoute.setSrcCode("2");
                        diffMode.getRoutes().add(currentRoute);
                        diffRoute = selectedRoute;
                    }
                    break;
                }
            }
            if (diffRoute == null) {
                currentRoute.setSrcCode("1");
                diffRoute = currentRoute;
            }
            if (diffRoute.getHeadsignCounts().size() > 0) {
                diffMode.getRoutes().add(diffRoute);
            }
        }
        if (selectedMode.getRoutes().size() > 0) {
            for (DataValidationRouteCounts selectedRoute : selectedMode.getRoutes()) {
                selectedRoute.setSrcCode("2");
                diffMode.getRoutes().add(selectedRoute);
            }
        }
        return diffMode;
    }

    private DataValidationRouteCounts compareRoutes(
            DataValidationRouteCounts currentRoute, DataValidationRouteCounts selectedRoute, boolean isFixed) {
        DataValidationRouteCounts diffRoute = new DataValidationRouteCounts();

        diffRoute.setRouteName(currentRoute.getRouteName());
        diffRoute.setRouteNum(currentRoute.getRouteNum());
        diffRoute.setHeadsignCounts(new TreeSet<DataValidationHeadsignCts>());

        for (DataValidationHeadsignCts currentHeadsign : currentRoute.getHeadsignCounts()) {
            // Check if this headsign exists in selectedMode
            DataValidationHeadsignCts diffHeadsign = null;
            String headsignName = currentHeadsign.getHeadsign();
            for (DataValidationHeadsignCts selectedHeadsign : selectedRoute.getHeadsignCounts()) {
                if (headsignName.equals(selectedHeadsign.getHeadsign())) {
                    selectedRoute.getHeadsignCounts().remove(selectedHeadsign);
                    diffHeadsign = compareHeadsigns(currentHeadsign, selectedHeadsign, isFixed);
                    break;
                }
            }
            if (diffHeadsign == null) {
                currentHeadsign.setSrcCode("1");
                diffHeadsign = currentHeadsign;
            }
            if (diffHeadsign.getDirCounts().size() > 0) {
                diffRoute.getHeadsignCounts().add(diffHeadsign);
            }
        }
        if (selectedRoute.getHeadsignCounts().size() > 0) {
            for (DataValidationHeadsignCts selectedHeadsign : selectedRoute.getHeadsignCounts()) {
                selectedHeadsign.setSrcCode("2");
                diffRoute.getHeadsignCounts().add(selectedHeadsign);
            }
        }
        return diffRoute;
    }

    private DataValidationHeadsignCts compareHeadsigns (
            DataValidationHeadsignCts currentHeadsign, DataValidationHeadsignCts selectedHeadsign, boolean isFixed) {
        DataValidationHeadsignCts diffHeadsign = new DataValidationHeadsignCts();

        diffHeadsign.setHeadsign(currentHeadsign.getHeadsign());
        diffHeadsign.setDirCounts(new TreeSet<DataValidationDirectionCts>());

        for (DataValidationDirectionCts currentDirection : currentHeadsign.getDirCounts()) {
            // Check if this headsign exists in selectedMode
            DataValidationDirectionCts diffDirection = null;
            String directionName = currentDirection.getDirection();
            for (DataValidationDirectionCts selectedDirection : selectedHeadsign.getDirCounts()) {
                if (directionName.equals(selectedDirection.getDirection())) {
                    selectedHeadsign.getDirCounts().remove(selectedDirection);
                    diffDirection = compareDirections(currentDirection, selectedDirection, isFixed);
                    break;
                }
            }
            if (diffDirection == null) {
                currentDirection.setSrcCode("1");
                diffDirection = currentDirection;
            }
            if (diffDirection.getStopCounts().size() > 0) {
                diffHeadsign.getDirCounts().add(diffDirection);
            }
        }
        if (selectedHeadsign.getDirCounts().size() > 0) {
            for (DataValidationDirectionCts selectedDirection : selectedHeadsign.getDirCounts()) {
                selectedDirection.setSrcCode("2");
                diffHeadsign.getDirCounts().add(selectedDirection);
            }
        }
        return diffHeadsign;
    }

    private DataValidationDirectionCts compareDirections (
            DataValidationDirectionCts currentDirection, DataValidationDirectionCts selectedDirection, boolean isFixed) {

        DataValidationDirectionCts diffDirection = new DataValidationDirectionCts();
        diffDirection.setDirection(currentDirection.getDirection());
        diffDirection.setStopCounts(new TreeSet<DataValidationStopCt>());

        for (DataValidationStopCt currentStopCt : currentDirection.getStopCounts()) {
            boolean stopCtMatched = false;
            // Check if this stop count  exists in selectedMode
            for (DataValidationStopCt selectedStopCt : selectedDirection.getStopCounts()) {
                if (currentStopCt.getStopCt() == selectedStopCt.getStopCt()) {
                    stopCtMatched = true;
                    selectedDirection.getStopCounts().remove(selectedStopCt);
                    if(isFixed) {
                        if ((currentStopCt.getTripCts()[0] != selectedStopCt.getTripCts()[0])
                                || (currentStopCt.getTripCts()[1] != selectedStopCt.getTripCts()[1])
                                || (currentStopCt.getTripCts()[2] != selectedStopCt.getTripCts()[2])) {
                            currentStopCt.setSrcCode("1");
                            diffDirection.getStopCounts().add(currentStopCt);
                            selectedStopCt.setSrcCode("2");
                            diffDirection.getStopCounts().add(selectedStopCt);
                        }
                        break;
                    }
                    else{
                        if ((currentStopCt.getTripCts()[0] != selectedStopCt.getTripCts()[0])) {
                            currentStopCt.setSrcCode("1");
                            diffDirection.getStopCounts().add(currentStopCt);
                            selectedStopCt.setSrcCode("2");
                            diffDirection.getStopCounts().add(selectedStopCt);
                        }
                        break;
                    }
                }
            }
            if (stopCtMatched) {
                continue;
            } else {
                currentStopCt.setSrcCode("1");
                diffDirection.getStopCounts().add(currentStopCt);
            }
        }
        if (selectedDirection.getStopCounts().size() > 0) {
            for (DataValidationStopCt selectedStopCt : selectedDirection.getStopCounts()) {
                selectedStopCt.setSrcCode("2");
                diffDirection.getStopCounts().add(selectedStopCt);
            }
        }
        return diffDirection;
    }


    private Map<String, List<String>> getReportModes() {
        Map<String, List<String>> reportModes = new HashMap<>();
        String sourceUrl = getSourceUrl();
        try (BufferedReader br =
                     new BufferedReader(new InputStreamReader(new URL(sourceUrl).openStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] reportData = line.split(",");
                List<String> reportRoutes = reportModes.get(reportData[0]);
                if (reportRoutes == null) {
                    reportRoutes = new ArrayList<>();
                }
                reportRoutes.add(reportData[1].trim());
                reportModes.put(reportData[0].trim(), reportRoutes);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return reportModes;
    }

    /*
     * This method will use the config service to retrieve the URL for report
     * input parameters.  The value is stored in config.json.
     *
     * @return the URL to use to retrieve the modes and routes to be reported on
     */
    private String getSourceUrl() {
        String sourceUrl = "";

        try {
            List<Map<String, String>> components = _configurationServiceClient.getItems("config");
            if (components == null) {
                _log.info("getItems call failed");
            }
            for (Map<String, String> component: components) {
                if (component.containsKey("component") && "admin".equals(component.get("component"))) {
                    if ("fixedRouteDataValidation".equals(component.get("key"))) {
                        sourceUrl = component.get("value");
                        break;
                    }
                }
            }
        } catch (Exception e) {
            _log.error("could not retrieve Data Validation URL from config:", e);
        }

        return sourceUrl;
    }

    class TripTotals {
        int[] wkdayTrips_0;
        int[] wkdayTrips_1;
        int[] satTrips_0;
        int[] satTrips_1;
        int[] sunTrips_0;
        int[] sunTrips_1;

        public TripTotals () {
            wkdayTrips_0 = new int[MAX_STOP_CT+1];
            wkdayTrips_1 = new int[MAX_STOP_CT+1];
            satTrips_0 = new int[MAX_STOP_CT+1];
            satTrips_1 = new int[MAX_STOP_CT+1];
            sunTrips_0 = new int[MAX_STOP_CT+1];
            sunTrips_1 = new int[MAX_STOP_CT+1];
        }
    }

}
