package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.MultiCSVLogger;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.StifTrip;
import org.springframework.beans.factory.annotation.Autowired;

public class AbnormalStifDataLoggerImpl {
	
	  private MultiCSVLogger csvLogger = null;
	  
	  @Autowired
	  public void setLogger(MultiCSVLogger logger) {
	    this.csvLogger = logger;
	  }
	  
	  public MultiCSVLogger getLogger(){
		  return this.csvLogger;
	  }
	  
	  public void header(String file, String header){
		  csvLogger.header(file, header);
	  }
	  
	  public void log(String file, Object... args){
		  csvLogger.log(file, args);
	  }
	  
	  public void createHeaders(){
		  csvLogger.header("non_pullin_without_next_movement.csv", "stif_trip,stif_filename,stif_trip_record_line_num");
		  csvLogger.header(
		        "stif_trips_without_pullout.csv",
		        "stif_trip,stif_filename,stif_trip_record_line_num,gtfs_trip_id,synthesized_block_id");
		  csvLogger.header("matched_trips_gtfs_stif.csv", "agency_id,gtfs_service_id,service_id,blockId,tripId,dsc,firstStop,"+
		        "firstStopTime,lastStop,lastStopTime,runId,reliefRunId,recoveryTime,firstInSeq,lastInSeq,signCodeRoute,routeId,busType");
	  }
	
	  // package private for unit testing
	  public void logDSCStatistics(Map<String, List<AgencyAndId>> dscToTripMap,
	      Map<AgencyAndId, String> tripToDscMap, HashMap<String, Set<AgencyAndId>> routeIdsByDsc) {
	    csvLogger.header("dsc_statistics.csv", "dsc,agency_id,number_of_trips_in_stif,number_of_distinct_route_ids_in_gtfs");
	    for (Map.Entry<String, List<AgencyAndId>> entry : dscToTripMap.entrySet()) {
	      String destinationSignCode = entry.getKey();
	      List<AgencyAndId> tripIds = entry.getValue();

	      Set<AgencyAndId> routeIds = routeIdsByDsc.get(destinationSignCode);
	      HashSet<String> set = new HashSet<String>();
	      for (AgencyAndId aaid : tripIds){
	        if (aaid != null){
	          set.add(aaid.getAgencyId());
	        }
	      }
	      for (String agencyId : set){
	        csvLogger.log("dsc_statistics.csv", destinationSignCode,agencyId, tripIds.size(),(routeIds != null ? routeIds.size() : 0));
	      }
	    }
	  }
	  
	  /**
	   * Dump some raw block matching data to a CSV file from stif trips
	   */
	  public void dumpBlockDataForTrip(StifTrip trip, String gtfsServiceId,
	      String tripId, String blockId, String routeId) {

	    csvLogger.log("matched_trips_gtfs_stif.csv", trip.agencyId,
	        gtfsServiceId, trip.serviceCode, blockId, tripId, trip.getDsc(), trip.firstStop,
	        trip.firstStopTime, trip.lastStop, trip.lastStopTime, trip.runId,
	        trip.reliefRunId, trip.recoveryTime, trip.firstTripInSequence,
	        trip.lastTripInSequence, trip.getSignCodeRoute(), routeId, trip.busType);
	  }
	  
	
}
