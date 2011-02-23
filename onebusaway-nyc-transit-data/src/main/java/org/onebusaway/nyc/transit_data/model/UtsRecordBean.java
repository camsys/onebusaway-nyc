/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.transit_data.model;

import java.io.Serializable;
import java.util.Date;

public final class UtsRecordBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private long id;
	private String route;	  
	private String depot;
	private String runNumber;	  
	private Date date;	  
	private Date scheduledPullOut;
	private Date actualPullOut;
	private Date scheduledPullIn;
	private Date actualPullIn;
	private String busNumber;
	private double busMileage;
	private String employeeLastName;
	private String employeeFirstName;
	private String employeePassNumber;
	private String employeeAuthId;

	public void setId(long id) {
		this.id = id;
	}

	public long getId() {
		return id;
	}

	public String getRoute() {
		return route;
	}

	public void setRoute(String r) {
		this.route = r;
	}

	public String getDepot() {
		return depot;
	}

	public void setDepot(String d) {
		this.depot = d;
	}

	public String getRunNumber() {
		return runNumber;
	}

	public void setRunNumber(String n) {
		this.runNumber = n;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date d) {
		this.date = d;
	}

	public Date getScheduledPullOut() {
		return scheduledPullOut;
	}

	public void setScheduledPullOut(Date d) {
		this.scheduledPullOut = d;
	}

	public Date getScheduledPullIn() {
		return scheduledPullIn;
	}

	public void setScheduledPullIn(Date d) {
		this.scheduledPullIn = d;
	}

	public Date getActualPullOut() {
		return actualPullOut;
	}

	public void setActualPullOut(Date d) {
		this.actualPullOut = d;
	}

	public Date getActualPullIn() {
		return actualPullIn;
	}

	public void setActualPullIn(Date d) {
		this.actualPullIn = d;
	}

	public String getBusNumber() {
		return busNumber;
	}

	public void setBusNumber(String n) {
		this.busNumber = n;
	}

	public double getBusMileage() {
		return busMileage;
	}

	public void setBusMileage(double m) {
		this.busMileage = m;
	}

	public String getEmployeeFirstName() {
		return employeeFirstName;
	}

	public void setEmployeeFirstName(String n) {
		this.employeeFirstName = n;
	}

	public String getEmployeeLastName() {
		return employeeLastName;
	}

	public void setEmployeeLastName(String n) {
		this.employeeLastName = n;
	}

	public String getEmployeePassNumber() {
		return employeePassNumber;
	}

	public void setEmployeePassNumber(String n) {
		this.employeePassNumber = n;
	}

	public String getEmployeeAuthId() {
		return employeeAuthId;
	}

	public void setEmployeeAuthId(String n) {
		this.employeeAuthId = n;
	}
}

