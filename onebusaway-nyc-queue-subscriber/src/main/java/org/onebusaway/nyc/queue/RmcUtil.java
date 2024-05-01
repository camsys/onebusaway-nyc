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

package org.onebusaway.nyc.queue;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class RmcUtil {
    private static final int THIRTY_SEC_MILLIS = 30*1000;
    private static Logger _log = LoggerFactory.getLogger(RmcUtil.class);

    public static String replaceInvalidRmcDateTime(StringBuilder realtime, long timeReceived) throws ParseException {

        StringBuilder realtimeLocal = new StringBuilder(realtime);
        String[] rmcData = getRmcData(realtimeLocal);

        if(rmcData != null) {
            // Fix RMC Date (1024 Weeks)
            Date rmcDateTime = getRmcDateTime(rmcData);
            if (!isRmcDateValid(rmcDateTime, timeReceived)) {
                String timeReported = getTimeReported(realtimeLocal);
                Date timeReceivedDate = new Date(timeReceived);
                replaceRmcDate(rmcData, timeReceivedDate);

                // Fix Rmc Time
                if (!isRmcTimeValid(rmcDateTime, timeReceivedDate)) {
                    replaceRmcTime(rmcData, timeReceivedDate);
                }
                String rmcDataString = StringUtils.join(rmcData, ",");
                rmcDataString = processNewCRC(rmcDataString);
                replaceRmcData(realtime, rmcDataString);

                // Fix Time Reported
                rmcData = getRmcData(realtime);
                rmcDateTime = getRmcDateTime(rmcData);
                if(!isTimeReportedValid(timeReported, rmcDateTime)){
                    replaceTimeReported(realtime, rmcDateTime);
                }
            }
        }

        return realtime.toString();
    }


    static String [] getRmcData(StringBuilder realtime){
        int rmcIndex = realtime.lastIndexOf("$GPRMC");
        int endRmcIndex = realtime.indexOf("\"",rmcIndex);
        if(rmcIndex != -1 && endRmcIndex != -1){
            String[] rmcParts = realtime.substring(rmcIndex, endRmcIndex).split(",");
            if(rmcParts.length == 13 && rmcParts[1].length() >= 6 && rmcParts[9].length() >= 6)
                return rmcParts;
        }
        return null;
    }

    static boolean isRmcDateValid(Date rmcDate, long timeReceived){
        Calendar calPast = Calendar.getInstance();
        calPast.setTimeInMillis(timeReceived);
        calPast.add(Calendar.WEEK_OF_YEAR, -1024);

        Calendar calFuture = Calendar.getInstance();
        calFuture.setTimeInMillis(timeReceived);
        calFuture.add(Calendar.WEEK_OF_YEAR, 1023);

        Calendar rmcCal = Calendar.getInstance();
        rmcCal.setTime(rmcDate);

        int calPastWeek = calPast.get(calPast.WEEK_OF_YEAR);
        int rmcCalWeek = rmcCal.get(rmcCal.WEEK_OF_YEAR);

        int calPastWeekDiff = Math.abs(calPastWeek - rmcCalWeek);

        if(calPastWeekDiff <= 1){
            return false;
        } else if(rmcCal.getTimeInMillis() >= calFuture.getTimeInMillis()){
            return false;
        }
        return true;
    }

    static boolean isRmcTimeValid(Date rmcDate, Date timeReceived){
        int rmcTime;
        int timeReceivedTime;

        timeReceivedTime = (int) (timeReceived.getTime() % (24*60*60*1000L));
        rmcTime = (int) (rmcDate.getTime() % (24*60*60*1000L));
        return (timeReceivedTime - rmcTime <= (THIRTY_SEC_MILLIS));
    }

    static void replaceRmcData(StringBuilder realtime, String rmcDataString){
        int rmcIndex = realtime.lastIndexOf("$GPRMC");
        int endRmcIndex = realtime.indexOf("\"",rmcIndex);
        realtime.replace(rmcIndex, endRmcIndex, rmcDataString);
    }

    static Date getRmcDateTime(String[] rmcData) throws ParseException {
        String rmcDateTime = rmcData[9] + " " + rmcData[1];
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyy HHmmss.S");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date date = sdf.parse(rmcDateTime);
        return date;
    }

    static void replaceRmcDate(String[] rmcData, Date timeReceived){
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyy");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        rmcData[9] = sdf.format(timeReceived);
    }

    static void replaceRmcTime(String[] rmcData, Date timeReceived){
        SimpleDateFormat sdf = new SimpleDateFormat("HHmmss.S");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        rmcData[1] = sdf.format(timeReceived);
    }

    static String getTimeReported(StringBuilder realtime) {
        String timeReportedText = "\"time-reported\":\"";
        int timeReportedIndex = realtime.lastIndexOf(timeReportedText);
        int endTimeReportedIndex = realtime.indexOf(",", timeReportedIndex);
        if(timeReportedIndex != -1 && endTimeReportedIndex != -1) {
            int startIndex = timeReportedIndex + timeReportedText.length();
            int endIndex = endTimeReportedIndex - 1;
            return realtime.substring(startIndex, endIndex);
        }
        return null;
    }

    static boolean isTimeReportedValid(String timeReported, Date rmcDateTime){

        if(timeReported.length() < 10){
            return false;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SXXX");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            Date timeReportedDateTime = sdf.parse(timeReported);
            long diffInMillis = Math.abs(rmcDateTime.getTime() - timeReportedDateTime.getTime());
            if(TimeUnit.MILLISECONDS.toSeconds(diffInMillis) > 30){
                return false;
            }
        } catch (ParseException e){
            return false;
        }
        return true;
    }

    static void replaceTimeReported(StringBuilder realtime, Date rmcDate) {
        String timeReportedText = "\"time-reported\":\"";
        int timeReportedIndex = realtime.lastIndexOf(timeReportedText) + timeReportedText.length();
        int endTimeReportedIndex = realtime.indexOf(",", timeReportedIndex) - 1;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SXXX");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String rmcDateString = sdf.format(rmcDate);
        rmcDateString = StringUtils.replace(rmcDateString, "Z", "-00:00");

        realtime.replace(timeReportedIndex, endTimeReportedIndex,rmcDateString);
    }

    public static String processNewCRC(String RMCRaw)
    {
        String checkSum = calculateCRC(RMCRaw);
        String RMCEdited = RMCRaw.substring(0, RMCRaw.indexOf("*"));
        RMCEdited = RMCEdited +"*"+checkSum;
        return RMCEdited;
    }

    public static String calculateCRC(String dataLine)
    {
        //XOR all characters between $ and *
        char crc = 0;
        int indexStar = dataLine.indexOf('*');

        for (int i = 1; i < indexStar; i++) {
            crc ^= dataLine.charAt(i);
        }

        // Make a 2 digit hex string
        String crcCalculated = ("00" + Integer.toHexString(crc & 0xFF));
        crcCalculated = crcCalculated.substring(crcCalculated.length() - 2);
        return crcCalculated;
    }
}
