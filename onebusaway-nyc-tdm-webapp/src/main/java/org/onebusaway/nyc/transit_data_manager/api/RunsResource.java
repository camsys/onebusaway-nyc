package org.onebusaway.nyc.transit_data_manager.api;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.RunTripEntry;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.transit_data_federation.services.nyc.RunService;
import org.onebusaway.transit_data.model.ArrivalAndDepartureBean;
import org.onebusaway.transit_data.model.ArrivalsAndDeparturesQueryBean;
import org.onebusaway.transit_data.model.StopWithArrivalsAndDeparturesBean;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.ExtendedCalendarService;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.jersey.api.spring.Autowire;

@Path("/runs")
@Component
@Scope("request")
@Autowire
public class RunsResource {

  public RunsResource() throws IOException {
  }

  private NycTransitDataService _nycTransitDataService;
  
  private DestinationSignCodeService _destinationSignCodeService;

  private RunService _runService;

  private TransitGraphDao _transitGraphDao;

  @Autowired
  public void setNycTransitDataService(
      NycTransitDataService nycTransitDataService) {
    _nycTransitDataService = nycTransitDataService;
  }
  
  @Autowired
  public void setDestinationSignCodeService(
      DestinationSignCodeService destinationSignCodeService) {
    _destinationSignCodeService = destinationSignCodeService;
  }
  
  @Autowired
  public void setRunService(RunService runService) {
    _runService = runService;
  }

  @Autowired
  public void setTransitGraphDao(TransitGraphDao transitGraphDao) {
    _transitGraphDao = transitGraphDao;
  }

  @Path("/runsForStop")
  @GET
  @Produces("application/json")
  public String getRunsForStop(@QueryParam("id") String id,
      @DefaultValue("60") @QueryParam("minutesBefore") int minutesBefore,
      @DefaultValue("60") @QueryParam("minutesAfter") int minutesAfter,
      @DefaultValue("0") @QueryParam("time") String time) throws ParseException {

    Long lTime;
    StopWithArrivalsAndDeparturesBean stopWithArrivalsAndDepartures;

    lTime = getTimeParamAsLong(time);
    stopWithArrivalsAndDepartures = getStopsWithArrivalsAndDepartures(id, lTime,
        minutesBefore, minutesAfter);

    Gson gson = new Gson();
    JsonObject ob = new JsonObject();
    ob.add("runsForStop",
        gson.toJsonTree(generateRunsForStopMap(id, stopWithArrivalsAndDepartures)));
    return ob.toString();
  }

  private Map<String, Object> generateRunsForStopMap(String stopId, StopWithArrivalsAndDeparturesBean stopWithArrivalsAndDepartures) {
    
    // Used to keep track of runs when adding trips
    Map<String, Map<String, Object>> runMap = new HashMap<String, Map<String, Object>>();
    Map<String, Object> model = new HashMap<String, Object>();
    List<Map<String, Object>> runList = new ArrayList<Map<String, Object>>();
    
    model.put("id", stopId);
    model.put("runs", runList);

    for (ArrivalAndDepartureBean adBean : stopWithArrivalsAndDepartures.getArrivalsAndDepartures()) {
      
      TripEntry tripEntry = _transitGraphDao.getTripEntryForId(AgencyAndIdLibrary.convertFromString(adBean.getTrip().getId()));
      RunTripEntry runTripEntry = _runService.getRunTripEntryForTripAndTime(
          tripEntry,
          (int) ((adBean.getScheduledArrivalTime() - adBean.getServiceDate()) / 1000));

      String runId = runTripEntry.getRunId();
      String agencyId = runTripEntry.getTripEntry().getId().getAgencyId();

      if (!runMap.containsKey(runId)) {
        Map<String, Object> run = generateRun(runId, agencyId);
        runList.add(run);
        runMap.put(runId, run);
      }

      // Get the list of trips we've been tracking for the current runId. It's
      // already part of our runs which are already part of our result
      @SuppressWarnings("unchecked")
      List<Map<String, String>> trips = (List<Map<String, String>>) (runMap.get(runId).get("trips"));
      Map<String, String> trip = generateTripForRun(adBean);
      trips.add(trip);
    }
    
    return model;
  }

  private Long getTimeParamAsLong(String time) throws ParseException {
    if (time.equals("0")) {
      return new Date().getTime();
    } else {
      try {
        return Long.valueOf(time);
      } catch (NumberFormatException e) {
        try {
          DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
          formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
          Date d = formatter.parse(time);
          return d.getTime();
        } catch (ParseException e1) {
          throw e1;
        }
      }
    }
  }

  private StopWithArrivalsAndDeparturesBean getStopsWithArrivalsAndDepartures(
      String stopId, Long lTime, int minutesBefore, int minutesAfter) {
    ArrivalsAndDeparturesQueryBean queryBean = new ArrivalsAndDeparturesQueryBean();
    queryBean.setMinutesBefore(minutesBefore);
    queryBean.setMinutesAfter(minutesAfter);
    queryBean.setTime(lTime);

    return _nycTransitDataService.getStopWithArrivalsAndDepartures(
        stopId, queryBean);
  }
  
  private Map<String, Object> generateRun(String runId, String agencyId){
    Map<String, Object> run = new HashMap<String, Object>();
    run.put("id", runId);
    run.put("agencyId", agencyId);
    run.put("trips", new ArrayList<HashMap<String, Object>>());
    return run;
  }
  
  private Map<String, String> generateTripForRun(ArrivalAndDepartureBean adBean){
    Map<String, String> trip = new HashMap<String, String>();

    trip.put("tripId", adBean.getTrip().getId());
    AgencyAndId tripId = AgencyAndIdLibrary.convertFromString(adBean.getTrip().getId());
    trip.put("destinationSignCode",
        _destinationSignCodeService.getDestinationSignCodeForTripId(tripId));
    trip.put("runId", _runService.getInitialRunForTrip(tripId));
    trip.put("routeId", adBean.getTrip().getRoute().getId());
    trip.put("directionId", adBean.getTrip().getDirectionId());
    trip.put("blockId", adBean.getTrip().getBlockId());
    trip.put("time", String.valueOf(adBean.getScheduledArrivalTime() / 1000)); // Predicted?
                                                                               // Scheduled?
    return trip;
  }
}