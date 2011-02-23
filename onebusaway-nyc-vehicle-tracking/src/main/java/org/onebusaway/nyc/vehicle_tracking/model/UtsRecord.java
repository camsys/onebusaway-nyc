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
package org.onebusaway.nyc.vehicle_tracking.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "uts_raw")
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
public class UtsRecord {
	@Id
	@GeneratedValue
	private long id;

	private String route;	  
	private String depot;

	@Column(name = "run_number")
	private String runNumber;

	private Date date;	  

	@Column(name = "sched_po")
	private Date scheduledPullOut;

	@Column(name = "actual_po")
	private Date actualPullOut;
	
	@Column(name = "sched_pi")
	private Date scheduledPullIn;
	
	@Column(name = "actual_pi")
	private Date actualPullIn;

	@Column(name = "bus_number")
	private String busNumber;
	
	@Column(name = "bus_mileage")
	private Double busMileage;

	@Column(name = "empl_name")
	private String employeeLastName;
	
	@Column(name = "empl_name_first")
	private String employeeFirstName;
	
	@Column(name = "pass")
	private String employeePassNumber;
	
	@Column(name = "auth_id")
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

