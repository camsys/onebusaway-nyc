package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onebusaway.csv_entities.CSVLibrary;
import org.onebusaway.csv_entities.CSVListener;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;

public class DSCOverrideHandler {

	  private String _tripToDSCOverridePath;
	  public void setTripToDSCOverridePath(String path) {
	    _tripToDSCOverridePath = path;
	  }
	
	public Map<String, List<AgencyAndId>> handleOverride(StifLoaderImpl stifLoader, Map<String, List<AgencyAndId>> dscToTripMap){
		if(_tripToDSCOverridePath == null)
			return dscToTripMap;
		
		Map<AgencyAndId, String> tripToDSCOverrides;
	      try {
	        tripToDSCOverrides = loadTripToDSCOverrides(_tripToDSCOverridePath);
	      } catch (Exception e) {
	    	  e.printStackTrace();
	        throw new IllegalStateException(e);
	      }

	      // Add tripToDSCOverrides to dscToTripMap
	      for (Map.Entry<AgencyAndId, String> entry : tripToDSCOverrides.entrySet()) {

	        if (stifLoader.getGtfsMutableRelationalDao().getTripForId(entry.getKey()) == null) {
	          throw new IllegalStateException("Trip id " + entry.getKey() + " from trip ID to DSC overrides does not exist in bundle GTFS.");
	        }

	        List<AgencyAndId> agencyAndIds;

	        // See if trips for this dsc are already in the map
	        agencyAndIds = dscToTripMap.get(entry.getValue());

	        // If not, care a new array of trip ids and add it to the map for this dsc
	        if (agencyAndIds == null) {
	          agencyAndIds = new ArrayList<AgencyAndId>();
	          dscToTripMap.put(entry.getValue(), agencyAndIds);
	        }

	        // Add the trip id to our list of trip ids already associated with a dsc
	        agencyAndIds.add(entry.getKey());
	      }
	      return dscToTripMap;
	}
	
	public Map<AgencyAndId, String> loadTripToDSCOverrides(String path) throws Exception {

		final Map<AgencyAndId, String> results = new HashMap<AgencyAndId, String>();

		CSVListener listener = new CSVListener() {

			int count = 0;
			int tripIdIndex;
			int dscIndex;

			@Override
			public void handleLine(List<String> line) throws Exception {

				if (line.size() != 2)
					throw new Exception("Each Trip ID to DSC CSV line must contain two columns.");

				if (count == 0) {
					count++;

					tripIdIndex = line.indexOf("trip_id");
					dscIndex = line.indexOf("dsc");

					if(tripIdIndex == -1 || dscIndex == -1) {
						throw new Exception("Trip ID to DSC CSV must contain a header with column names 'trip_id' and 'dsc'.");
					}

					return;
				}

				results.put(AgencyAndIdLibrary.convertFromString(line.get(tripIdIndex)), line.get(dscIndex));
			}
		};

		File source = new File(path);

		new CSVLibrary().parse(source, listener);

		return results;
	}
	
}
