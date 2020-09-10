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

import org.onebusaway.gtfs.model.*;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.GtfsMutableRelationalDao;
import org.onebusaway.nyc.admin.model.*;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.MultiCSVLogger;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.util.impl.tdm.ConfigurationServiceImpl;
import org.onebusaway.nyc.util.impl.tdm.TransitDataManagerApiLibrary;
import org.onebusaway.transit_data_federation.services.FederatedTransitDataBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TripCountByZoneDataOutputTask implements Runnable {
    private static final String FOLDERNAME = "TripCountByZoneData";
    private Logger _log = LoggerFactory.getLogger(DailyDataValidationTask.class);
    private MultiCSVLogger logger;
    private GtfsMutableRelationalDao _dao;
    private FederatedTransitDataBundle _bundle;
    private static final int MAX_STOP_CT = 150;
    private File _mappingsFile;
    int maxStops = 0;
    final private String internalDelimeter = "&%&%&%&%";
    String ARG_TOTAL = "total";
    protected BundleRequestResponse requestResponse;
    protected BundleBuildRequest request;
    protected BundleBuildResponse response;

    @Autowired
    public void setLogger(MultiCSVLogger logger) {
        this.logger = logger;
    }

    @Autowired
    public void setGtfsDao(GtfsMutableRelationalDao dao) {
        _dao = dao;
    }

    @Autowired
    public void setBundle(FederatedTransitDataBundle bundle) {
        _bundle = bundle;
    }

    @Autowired
    public void setRequestResponse(BundleRequestResponse requestResponse) {
        this.requestResponse = requestResponse;
    }

    @Override
    public void run() {
        _log.info("starting TripCountByZoneDataOutputTask");
        setMappingsFile(requestResponse.getRequest());
        if (! _mappingsFile.isFile()) {
            _log.info("missing mapping file,{} exiting", _mappingsFile.getAbsolutePath());
            return;
        }
        try {
            process();
        } catch (Exception e) {
            _log.error("exception with dailyVa:", e);
            requestResponse.getResponse().setException(e);
        } finally {
            _log.info("done");
        }

    }

    private void setMappingsFile(BundleBuildRequest request){
        _mappingsFile = new File(request.getRouteMappings());
    }

    private void process(){
        new File(logger.getBasePath() + "/" + FOLDERNAME).mkdir();
        _log.info("Creating TripCountByZoneData from this file" + _mappingsFile);
        Map<AgencyAndId, Map<String, Integer>> tripCountForZonesForServiceId = getTripCountForZonesForServiceId(_dao);
        _log.info("Gotten tripCountForZonesForServiceId organized");
        Map<Date, List<AgencyAndId>> serviceIdsByDate = getServiceIdsByDate(_dao);
        _log.info("Determined Service Ids for dates");
        Map<List<AgencyAndId>,HashMap<String,Integer>> outputTripCountForZoneForServiceIds = new HashMap<>();
        Collection<String> zonesWrittenTo = new HashSet<>();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyy-MM-dd");
        int dayCounter = 0;
        Date oldestDate = getOldestDate(serviceIdsByDate.keySet());
        Date newestDate = getNewestDate(serviceIdsByDate.keySet());
        Date dateDay = removeTime(addDays(oldestDate, dayCounter));
        while (newestDate.after(dateDay)) {
            if (serviceIdsByDate.get(dateDay) == null) {
                dayCounter++;
                dateDay = removeTime(addDays(oldestDate, dayCounter));
                continue;
            }
            List<AgencyAndId> serviceIds = serviceIdsByDate.get(dateDay);
            HashMap<String, Integer> outputTripCountForZone = outputTripCountForZoneForServiceIds.get(serviceIds);
            if (outputTripCountForZone == null) {
                outputTripCountForZone = new HashMap<>();
                for (AgencyAndId serviceId : serviceIds) {
                    for (Map.Entry<String, Integer> tripCountForZonesEntry : tripCountForZonesForServiceId.get(serviceId).entrySet()) {
                        String zone = tripCountForZonesEntry.getKey();
                        Integer tripCount = tripCountForZonesEntry.getValue();
                        safeMapAdd(outputTripCountForZone,zone,tripCount);

                    }
                }
                outputTripCountForZoneForServiceIds.put(serviceIds, outputTripCountForZone);
            }

            String date = simpleDateFormat.format(dateDay);
            String filePathBase = FOLDERNAME + "/";
            String filePathSufix = ".csv";
            for (Map.Entry<String, Integer> entry : outputTripCountForZone.entrySet()) {
                String zone = entry.getKey();
                String filePath = filePathBase + entry.getKey() + filePathSufix;
                if(!zonesWrittenTo.contains(zone)){
                    zonesWrittenTo.add(zone);
                    logger.header(filePath, "date,tripCount");
                }
                logger.logCSV(filePath, date + "," + entry.getValue());
            }
            dayCounter++;
            dateDay = removeTime(addDays(oldestDate, dayCounter));
        }
    }



    private Map<AgencyAndId,Map<String, Integer>> getTripCountForZonesForServiceId(GtfsMutableRelationalDao dao){
        Map<AgencyAndId,Map<String, Integer>> tripCountForZonesForServiceId = new HashMap<>();

        Map<AgencyAndId, String> zoneForRoutes = getZoneforRoutes();
        for(AgencyAndId serviceId : dao.getAllServiceIds()){
            Map<String,Integer> tripCountForZones = new HashMap<>();
            for(Trip trip : dao.getTripsForServiceId(serviceId)){
                Route route = trip.getRoute();
                String zone = zoneForRoutes.get(route.getId());
                safeMapAdd(tripCountForZones,zone,1);
                String[] depot = serviceId.getId().split("_")[0].split("-");
                String zone2 = depot.length>1 ? depot[1] : depot[0];

                safeMapAdd(tripCountForZones,zone2,1);
                safeMapAdd(tripCountForZones,ARG_TOTAL,1);
            }
            tripCountForZonesForServiceId.put(serviceId,tripCountForZones);
        }

        return tripCountForZonesForServiceId;
    }

    private Map<String, Integer> safeMapAdd(Map<String, Integer> map, String key, int n){
        if(map.get(key)==null){
            map.put(key,n);
        } else {
            map.put(key,map.get(key)+n);
        }
        return map;
    }



    private Map<Date,List<AgencyAndId>> getServiceIdsByDate (GtfsMutableRelationalDao dao){
        Map<Date,List<AgencyAndId>> serviceIdsByDate = new HashMap<>();

        for(AgencyAndId serviceId : dao.getAllServiceIds()){
            //if there are no entries in calendarDates, check serviceCalendar
            ServiceCalendar servCal = dao.getCalendarForServiceId(serviceId);
            if (servCal != null) {
                //check for service using calendar
                Date start = removeTime(servCal.getStartDate().getAsDate());
                Date end = removeTime(addDays(servCal.getEndDate().getAsDate(),1));
                int dayIndexCounter = 0;
                Date index = removeTime(addDays(start, dayIndexCounter));
                int[] activeDays = {0,
                        servCal.getSunday(),
                        servCal.getMonday(),
                        servCal.getTuesday(),
                        servCal.getWednesday(),
                        servCal.getThursday(),
                        servCal.getFriday(),
                        servCal.getSaturday(),
                };

                while(index.before(end)){
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(index);
                    int day = calendar.get(Calendar.DAY_OF_WEEK);
                    if(activeDays[day] == 1) {
                        if (serviceIdsByDate.get(index) == null) {
                            serviceIdsByDate.put(index, new ArrayList<>());
                        }
                        serviceIdsByDate.get(index).add(serviceId);
                    }
                    dayIndexCounter += 1;
                    index = removeTime(addDays(start, dayIndexCounter));
                }
            }

            for (ServiceCalendarDate calDate : dao.getCalendarDatesForServiceId(serviceId)) {
                Date date = constructDate(calDate.getDate());
                if(calDate.getExceptionType() == 1){
                    if (serviceIdsByDate.get(date) == null) {
                        serviceIdsByDate.put(date, new ArrayList<>());
                    }
                    serviceIdsByDate.get(date).add(serviceId);
                }
                if(calDate.getExceptionType() == 2){
                    if(serviceIdsByDate.get(date) != null) {
                        serviceIdsByDate.get(date).remove(serviceId);
                        if (serviceIdsByDate.get(date).size() == 0) {
                            serviceIdsByDate.remove(date);
                        }
                    }
                }

            }

        }
        for(Map.Entry<Date,List<AgencyAndId>> serviceIdsByDateEntry : serviceIdsByDate.entrySet()){
            Collections.sort(serviceIdsByDateEntry.getValue());
        }

        LinkedHashMap<Date,List<AgencyAndId>> sortedServiceIdsByDate = new LinkedHashMap<>();
        serviceIdsByDate.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEachOrdered(x -> sortedServiceIdsByDate.put(x.getKey(), x.getValue()));
        return sortedServiceIdsByDate;
    }



//    private LinkedHashMap<K, V> sortMap(LinkedHashMap<K, V> map){
//        return map;
//    }

    private Date getOldestDate(Set<Date> dateSet) {
        if (dateSet.size() < 1) {
            return null;
        }
        Date oldestDate = dateSet.iterator().next();
        for (Date date : dateSet) {
            if (date.before(oldestDate)) {
                oldestDate = date;
            }
        }
        return oldestDate;
    }

    private Date getNewestDate(Set<Date> dateSet) {
        if (dateSet.size() < 1) {
            return null;
        }
        Date newestDate = dateSet.iterator().next();
        for (Date date : dateSet) {
            if (date.after(newestDate)) {
                newestDate = date;
            }
        }
        return newestDate;
    }


    private Map<AgencyAndId, String> getZoneforRoutes() {
        Map<AgencyAndId, String> zoneforRoutes = new HashMap<>();
        try (BufferedReader br =
                     new BufferedReader(new FileReader(_mappingsFile))) {
            String line;
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] reportData = line.split(",");
                for (int i =1; i < reportData.length; i++){
                    String[] AgencyAndIdStrings = reportData[i].trim().split("\\*\\*\\*");
                    AgencyAndId agencyAndId = new AgencyAndId(AgencyAndIdStrings[0], AgencyAndIdStrings[1]);
                    zoneforRoutes.put(agencyAndId,reportData[0].trim());
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return zoneforRoutes;
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

    class BoroughTripCount {
        String borough;
        int tripCount;

        public int getTripCount() {
            return tripCount;
        }

        public String getBorough() {
            return borough;
        }

        public void setBorough(String borough) {
            this.borough = borough;
        }

        public void setTripCount(int tripCount) {
            this.tripCount = tripCount;
        }
    }


    private Date addDays(Date date, int daysToAdd) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, daysToAdd);
        return cal.getTime();
    }

    private Date constructDate(ServiceDate date) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, date.getYear());
        calendar.set(Calendar.MONTH, date.getMonth()-1);
        calendar.set(Calendar.DATE, date.getDay());
        Date date1 = calendar.getTime();
        date1 = removeTime(date1);
        return date1;
    }

    private Date removeTime(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        date = calendar.getTime();
        return date;
    }

}
