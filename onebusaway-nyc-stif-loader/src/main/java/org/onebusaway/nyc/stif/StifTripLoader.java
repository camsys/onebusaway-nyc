/*
 * Copyright 2010, OpenPlans
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.     
 */

package org.onebusaway.nyc.stif;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.onebusaway.nyc.stif.model.StifRecord;
import org.onebusaway.nyc.stif.model.TripRecord;


public class StifTripLoader {
	class TripIdentifier {
		public int startTime;
		public String routeName;
		public TripIdentifier(Trip trip) {
			this.routeName = trip.getRoute().getId().getId();
			this.startTime = gtfsDao.getStopTimesForTrip(trip).get(0).getDepartureTime();
		}
		public TripIdentifier(int startTime, String routeName) {
			this.startTime = startTime;
			this.routeName = routeName;
		}
		public TripIdentifier(TripRecord tripRecord) {
			routeName = tripRecord.getRoute();
			startTime = tripRecord.getOriginTime();
		}
		public boolean equals(Object other) {
			if (other instanceof TripIdentifier) {
				TripIdentifier otherTrip = (TripIdentifier) other; 
				return otherTrip.startTime == startTime && otherTrip.routeName.equals(routeName);
			}
			return false;
		}
		public int hashCode() {
			return startTime ^ routeName.hashCode();
		}
		
		public String toString() {
			return "TripIdentifier(" + startTime + ", " + routeName + ")"; 
		}
	}
	

	private GtfsRelationalDao gtfsDao;
	private Map<String, List<Trip>> tripsBySignCode;
	private Map<TripIdentifier, List<Trip>> tripsByIdentifier;

	public void setGtfsDao(GtfsRelationalDao dao) {
		this.gtfsDao = dao;
	}
	
	public Map<String, List<Trip>>  getTripMapping() {
		return tripsBySignCode;
	}
	
	@PostConstruct
	public void init() {
		tripsByIdentifier = new HashMap<TripIdentifier, List<Trip>>();
		for (Trip trip : gtfsDao.getAllTrips()) {
			TripIdentifier id = new TripIdentifier(trip);
			List<Trip> trips = tripsByIdentifier.get(id);
			if (trips == null) {
				trips = new ArrayList<Trip>();
				tripsByIdentifier.put(id, trips);
			}
			trips.add(trip);
		}
		
		tripsBySignCode = new HashMap<String, List<Trip>>();
	}
	
	public void run(File path) {
		try {
			run(new FileInputStream(path));
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Error loading " + path , e);
		}
	}
	
	public void run (InputStream stream) {
		StifRecordReader reader;

		try {
			reader = new StifRecordReader(stream);
			while(true) {
				StifRecord record = reader.read();
				if (record == null) {
					break;
				}
				if (record instanceof TripRecord) {
					TripRecord tripRecord = (TripRecord) record;
					int tripType = tripRecord.getTripType();
					if (tripType == 2 || tripType == 3 || tripType == 4) {
						continue; //deadhead or to/from depot
					}
					String code = tripRecord.getSignCode();
					TripIdentifier id = new TripIdentifier(tripRecord);
					List<Trip> trips = tripsByIdentifier.get(id);
					if (trips == null) {
						//trip in stif but not in gtfs
						throw new RuntimeException("trip not found for " + id);
					}
					tripsBySignCode.put(code, trips);
				}
				
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
