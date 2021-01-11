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

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.onebusaway.gtfs.model.*;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.GtfsMutableRelationalDao;
import org.onebusaway.nyc.admin.model.BundleRequestResponse;
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

public class DailyDataValidationTask implements Runnable {
    private static final String FOLDERNAME = "DailyDataValidation";
    private Logger _log = LoggerFactory.getLogger(DailyDataValidationTask.class);
    private MultiCSVLogger logger;
    private GtfsMutableRelationalDao _dao;
    private FederatedTransitDataBundle _bundle;
    private static final int MAX_STOP_CT = 150;
    private File _mappingsFile;
    int maxStops = 0;
    final private String internalDelimeter = "&%&%&%&%";
    BundleRequestResponse requestResponse;

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
        _log.info("starting DailyDataValidationTask");
        setMappingsFile();
        if (! _mappingsFile.isFile()) {
            _log.info("missing mapping file,{} exiting", _mappingsFile.getAbsolutePath());
            return;
        }
        try {
            process();
            _log.info("done");
        } catch (Exception e) {
            _log.error("exception with dailyVa:", e);
            requestResponse.getResponse().setException(e);
        }

    }

    private void setMappingsFile(){
        _mappingsFile = new File(requestResponse.getRequest().getRouteMappings());
    }

    private void process(){
        _log.info("Creating daily route data validation from this file" + _mappingsFile);

        Map<AgencyAndId,Set<String>> RouteInfoByServiceId = getRouteInfoByServiceId(_dao);
        _log.info("Gotten TripCountsByRouteInfoByServiceId organized");
        Map<Date,List<AgencyAndId>> serviceIdsByDate = getServiceIdsByDate(_dao);
        _log.info("Determined Service Ids for dates");
        Map<List<AgencyAndId>,List<String>> orderedOutputForServiceIds = new HashMap<>();

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
            _log.info("Printing route summaries for " + simpleDateFormat.format(dateDay));
            String filePath = FOLDERNAME + "/" + simpleDateFormat.format(dateDay) + ".csv";
            new File(logger.getBasePath() + "/" + FOLDERNAME).mkdir();
            logger.header(filePath, "Zone,Route,Headsign,Direction,# of stops,# of trips");
            String[] oldRouteInfoArray = null;
            List<AgencyAndId> serviceIds = serviceIdsByDate.get(dateDay);
            List<String> output = orderedOutputForServiceIds.get(serviceIds);
            if (output == null) {
                output = new ArrayList<>();
                for (AgencyAndId agencyAndId : serviceIdsByDate.get(dateDay)) {
                    Set<String> routeInfoByServiceId = RouteInfoByServiceId.get(agencyAndId);
                    for (String routeInfo : routeInfoByServiceId) {
                        output.add( routeInfo);
                    }
                }
                Collections.sort(output);

                for( int n = 0; n < output.size(); n++){
                    String line = output.get(n);
                    String[] routeInfoArray = line.split(internalDelimeter);
                    String routeInfoOutput = "";
                    if (oldRouteInfoArray == null) {
                        oldRouteInfoArray = routeInfoArray;
                        routeInfoOutput = Arrays.stream(routeInfoArray).reduce((a, b) -> a + "," + b).get();
                    } else {
                        boolean match = true;
                        for (int i = 0; i < routeInfoArray.length; i++) {
                            if (routeInfoArray[i].equals(oldRouteInfoArray[i]) & match) {
                                routeInfoOutput += ",";
                            } else {
                                routeInfoOutput += routeInfoArray[i].replace(",", "") + ",";
                                oldRouteInfoArray[i] = routeInfoArray[i];
                                match = false;
                            }
                        }
                    }
                    output.set(n, routeInfoOutput);
                }

                orderedOutputForServiceIds.put(serviceIds,output);
            }
            for(String line : output){
                logger.logCSV(filePath, line);
            }
            dayCounter ++;
            dateDay = removeTime(addDays(oldestDate, dayCounter));
            logger.logCSV(filePath,",,,,,,,,");
        }

        _log.info("finished Day-by-Day route data validation report");
    }



    private Map<AgencyAndId,Set<String>> getRouteInfoByServiceId(GtfsMutableRelationalDao dao){

        Map<AgencyAndId,Set<String>> RouteInfoByServiceId = new HashMap<>();
        Map<AgencyAndId, String> zoneForRoutes = getZoneforRoutes();
        for(AgencyAndId serviceId : dao.getAllServiceIds()){
            Set<String> routeInfoSet = new HashSet<>();
            Map<String,Integer> routeInfoByServiceId = new HashMap<>();
            for(Trip trip : dao.getTripsForServiceId(serviceId)){
                Route route = trip.getRoute();
                String zone = zoneForRoutes.get(route.getId());
                String routeName = route.getShortName() + "-" + route.getDesc();
                String headSign = trip.getTripHeadsign();
                String dir;
                if (trip.getDirectionId() == null || trip.getDirectionId().equals("0")) {
                    dir = "0";
                } else {
                    dir = "1";
                }
                List<StopTime> stopTimes =  _dao.getStopTimesForTrip(trip);
                int stopCt = stopTimes.size();

                String routeInfo = zone + internalDelimeter
                        + routeName + internalDelimeter
                        + headSign + internalDelimeter
                        + dir + internalDelimeter
                        + stopCt;
                if(routeInfoByServiceId.get(routeInfo)==null){
                    routeInfoByServiceId.put(routeInfo,1);
                } else {
                    routeInfoByServiceId.put(routeInfo,routeInfoByServiceId.get(routeInfo)+1);
                }
            }
            for(Map.Entry<String, Integer> entry : routeInfoByServiceId.entrySet()){
                routeInfoSet.add(entry.getKey() + internalDelimeter + entry.getValue() + internalDelimeter + serviceId);
            }


            RouteInfoByServiceId.put(serviceId,routeInfoSet);
        }

        return RouteInfoByServiceId;
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
