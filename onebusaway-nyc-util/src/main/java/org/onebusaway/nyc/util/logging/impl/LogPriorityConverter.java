/*
 * Copyright (C)  2011 Metropolitan Transportation Authority
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onebusaway.nyc.util.logging.impl;

public class LogPriorityConverter {
    public static final int TRACE_INT = 5000;
    public static final int OFF_INT = 2147483647;
    public static final int FATAL_INT = 50000;
    public static final int ERROR_INT = 40000;
    public static final int WARN_INT = 30000;
    public static final int INFO_INT = 20000;
    public static final int DEBUG_INT = 10000;
    public static final int ALL_INT = -2147483648;

    public static int priorityToInt(String sArg) {
        if (sArg == null) {
            return INFO_INT;
        } else {
            String s = sArg.toUpperCase();
            if (s.equals("ALL")) {
                return ALL_INT;
            } else if (s.equals("DEBUG")) {
                return DEBUG_INT;
            } else if (s.equals("INFO")) {
                return INFO_INT;
            } else if (s.equals("WARN")) {
                return WARN_INT;
            } else if (s.equals("ERROR")) {
                return ERROR_INT;
            } else if (s.equals("FATAL")) {
                return FATAL_INT;
            } else if (s.equals("OFF")) {
                return OFF_INT;
            } else if (s.equals("TRACE")) {
                return TRACE_INT;
            } else {
                return INFO_INT;
            }
        }
    }



}
