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
package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifImport;
import java.io.Serializable;

public class StifStopTime implements Serializable {
    private static final long serialVersionUID = 2L;
    public String id;
    public int time;
    public boolean revenue;
    public int boardingAlightingFlag;

    public StifStopTime(String id, int time, int boardingAlightingFlag, boolean revenue){
        this.id = id;
        this.time = time;
        this.boardingAlightingFlag = boardingAlightingFlag;
        this.revenue = revenue;
    }

    public void setBoardingAlightingFlag(int boardingAlightingFlag) {
        this.boardingAlightingFlag = boardingAlightingFlag;
    }

    public int getBoardingAlightingFlag(){
        return boardingAlightingFlag;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setRevenue(boolean revenue) {
        this.revenue = revenue;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getTime() {
        return time;
    }

    public String getId() {
        return id;
    }

    public boolean isRevenue() {
        return revenue;
    }

    public String toString(){
        return id + "," + time + "," + revenue + "," + boardingAlightingFlag;
    }
}
