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
package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model;

import java.util.HashMap;

public enum ServiceCode {
	WEEKDAY_SCHOOL_OPEN, 
	WEEKDAY_SCHOOL_CLOSED, 
	SATURDAY, 
	SUNDAY, 
	MLK, 
	PRESIDENTS_DAY, 
	MEMORIAL_DAY, 
	GOOD_FRIDAY, 
	LABOR_DAY, 
	JULY_FOURTH, 
	COLUMBUS_DAY, 
	THANKSGIVING, 
	DAY_AFTER_THANKSGIVING, 
	CHRISTMAS_EVE, 
	CHRISTMAS_DAY, 
	CHRISTMAS_DAY_OBSERVED, 
	CHRISTMAS_WEEK, 
	NEW_YEARS_EVE, 
	NEW_YEARS_DAY, 
	NEW_YEARS_DAY_OBSERVED;

	static HashMap<String, ServiceCode> serviceCodeForId = new HashMap<String, ServiceCode>();
	static HashMap<ServiceCode, String> idForServiceCode = new HashMap<ServiceCode, String>();

	static {
		mapServiceCode("0", WEEKDAY_SCHOOL_OPEN);
		mapServiceCode("11", WEEKDAY_SCHOOL_CLOSED);
		mapServiceCode("1", SATURDAY);
		mapServiceCode("2", SUNDAY);
		mapServiceCode("H", MLK);
		mapServiceCode("I", PRESIDENTS_DAY);
		mapServiceCode("J", GOOD_FRIDAY);
		mapServiceCode("K", MEMORIAL_DAY);
		mapServiceCode("M", JULY_FOURTH);
		mapServiceCode("N", LABOR_DAY);
		mapServiceCode("O", COLUMBUS_DAY);
		mapServiceCode("R", THANKSGIVING);
		mapServiceCode("S", DAY_AFTER_THANKSGIVING);
		mapServiceCode("T", CHRISTMAS_EVE);
		mapServiceCode("U", CHRISTMAS_DAY);
		mapServiceCode("V", CHRISTMAS_DAY_OBSERVED);
		mapServiceCode("W", CHRISTMAS_WEEK);
		mapServiceCode("X", NEW_YEARS_EVE);
		mapServiceCode("Y", NEW_YEARS_DAY);
		mapServiceCode("Z", NEW_YEARS_DAY_OBSERVED);
	}
	

	public static ServiceCode getServiceCodeForId(String id) {
		return serviceCodeForId.get(id);
	}

	public static String getServiceCodeForId(ServiceCode code) {
		return idForServiceCode.get(code);
	}

	private static void mapServiceCode(String string,
			ServiceCode serviceCode) {
		serviceCodeForId.put(string, serviceCode);
		idForServiceCode.put(serviceCode, string);
	}

}
