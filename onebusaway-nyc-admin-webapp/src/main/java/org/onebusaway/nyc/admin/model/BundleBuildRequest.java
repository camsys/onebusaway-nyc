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

package org.onebusaway.nyc.admin.model;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.ArrayList;
import java.util.List;

public class BundleBuildRequest {
	private String _id;
	private String _bundleDirectory;
	private String _bundleName;
	private String _tmpDirectory;
	private String _emailAddress;
	private String _bundleStartDate;
	private String _bundleEndDate;
	private String _routeMappings = "";
	private boolean _predate = false;

	public String getBundleDirectory() {
		return _bundleDirectory;
	}

	public void setBundleDirectory(String bundleDirectory) {
		_bundleDirectory = bundleDirectory;
	}

	public String getTmpDirectory() {
		return _tmpDirectory;
	}

	public void setTmpDirectory(String tmpDirectory) {
		_tmpDirectory = tmpDirectory;
	}

	public boolean getPredate(){return _predate;}

	public void setPredate(boolean predate){_predate = predate;}

	// TODO this should come from config service
	public List<String> getNotInServiceDSCList() {
		ArrayList<String> dscs = new ArrayList<String>();
		dscs.add("10");
		dscs.add("11");
		dscs.add("12");
		dscs.add("13");
		dscs.add("22");
		dscs.add("6");
		return dscs;
	}

	public String getBundleName() {
		return _bundleName;
	}

	public void setBundleName(String bundleName) {
		_bundleName = bundleName;
	}

	public LocalDate getBundleStartDate() {
		DateTimeFormatter dtf = ISODateTimeFormat.date();
		return new LocalDate(dtf.parseLocalDate(_bundleStartDate));
	}

	public String getBundleStartDateString() {
		return _bundleStartDate;
	}

	public void setBundleStartDate(String bundleStartDate) {
		_bundleStartDate = bundleStartDate;
	}

	public LocalDate getBundleEndDate() {
		DateTimeFormatter dtf = ISODateTimeFormat.date();
		return new LocalDate(dtf.parseLocalDate(_bundleEndDate));
	}

	public void setBundleEndDate(String bundleEndDate) {
		_bundleEndDate = bundleEndDate;
	}

	public String getBundleEndDateString() {
		return _bundleEndDate;
	}

	public String getEmailAddress() {
		return _emailAddress;
	}

	public void setEmailAddress(String emailTo) {
		_emailAddress = emailTo;
	}

	public String getId() {
		return _id;
	}

	public void setId(String id) {
		_id = id;
	}

	public void setRouteMappings(String routeMappings){
		_routeMappings = routeMappings;
	}

	public String getRouteMappings() {
		return _routeMappings;
	}
}
