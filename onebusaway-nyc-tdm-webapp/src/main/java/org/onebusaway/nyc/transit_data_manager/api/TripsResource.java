package org.onebusaway.nyc.transit_data_manager.api;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.ReliefState;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.RunTripEntry;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.transit_data_federation.services.nyc.RunService;
import org.onebusaway.transit_data_federation.services.ExtendedCalendarService;
import org.onebusaway.transit_data_federation.services.transit_graph.ServiceIdActivation;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.jersey.api.spring.Autowire;

@Path("/trips")
@Component
@Scope("request")
@Autowire
public class TripsResource {

  public TripsResource() throws IOException {
  }

  private RunService _runService;

  private ExtendedCalendarService extCalendarService;

  private DestinationSignCodeService _destinationSignCodeService;

  public ExtendedCalendarService getRunService() {
    return extCalendarService;
  }

  @Autowired
  public void setRunService(RunService runService) {
    _runService = runService;
  }

  public ExtendedCalendarService getCalendarService() {
    return extCalendarService;
  }

  @Autowired
  public void setExtendedCalendarService(
      ExtendedCalendarService extCalendarService) {
    this.extCalendarService = extCalendarService;
  }

  @Autowired
  public void setDestinationSignCodeService(
      DestinationSignCodeService destinationSignCodeService) {
    _destinationSignCodeService = destinationSignCodeService;
  }

  private Map<String, Object> generateTripsMap(
      Collection<RunTripEntry> runTripEntries, Map<String, Object> output)
      throws ParseException {
    DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

    Long lTime;

    String time = "0";
    if (time.equals("0")) {
      lTime = new Date().getTime();
    } else {
      try {
        lTime = Long.valueOf(time);
      } catch (NumberFormatException e) {
        try {
          Date d = formatter.parse(time);
          lTime = d.getTime();
        } catch (ParseException e1) {
          throw e1;
        }
      }
    }

    Date serviceDate = this.getTimestampAsDate(lTime);

    List<Map<String, Object>> trips = new ArrayList<Map<String, Object>>();

    output.put("trips", trips);

    if (runTripEntries.size() > 0) {
      RunTripEntry rte = runTripEntries.toArray(new RunTripEntry[runTripEntries.size()])[0];
      output.put("runId", rte.getRunId());
      output.put("agencyId", rte.getTripEntry().getId().getAgencyId());
    }

    for (RunTripEntry entry : runTripEntries) {

      TripEntry tripEntry = entry.getTripEntry();
      ServiceIdActivation serviceIds = new ServiceIdActivation(
          tripEntry.getServiceId());

      if (extCalendarService.areServiceIdsActiveOnServiceDate(serviceIds,
          serviceDate)) {

        Map<String, Object> trip = new HashMap<String, Object>();

        trip.put("tripId", entry.getTripEntry().getId().toString());
        trip.put("initialRunId",
            _runService.getInitialRunForTrip(entry.getTripEntry().getId()));
        trip.put("startTime", serviceDate.getTime() / 1000
            + entry.getTripEntry().getStopTimes().get(0).getArrivalTime());
        trip.put(
            "startLocationStopId",
            entry.getTripEntry().getStopTimes().get(0).getStop().getId().toString());
        trip.put(
            "endTime",
            serviceDate.getTime()
                / 1000
                + entry.getTripEntry().getStopTimes().get(
                    entry.getTripEntry().getStopTimes().size() - 1).getArrivalTime());
        trip.put(
            "endLocationStopId",
            entry.getTripEntry().getStopTimes().get(
                entry.getTripEntry().getStopTimes().size() - 1).getStop().getId().toString());
        trip.put(
            "dsc",
            _destinationSignCodeService.getDestinationSignCodeForTripId(entry.getTripEntry().getId()));
        trip.put("routeId", entry.getTripEntry().getRoute().getId().toString());
        trip.put("directionId", entry.getTripEntry().getDirectionId());
        trip.put("blockId", entry.getTripEntry().getBlock().getId().toString());
        if (!entry.getRelief().equals(ReliefState.NO_RELIEF)) {
          trip.put("reliefRunId",
              _runService.getReliefRunForTrip(entry.getTripEntry().getId()));
          int reliefTime = _runService.getReliefTimeForTrip(entry.getTripEntry().getId());
          trip.put("reliefTime", serviceDate.getTime() / 1000 + reliefTime);

          StopEntry reliefStop = null;
          for (int i = 0; i < entry.getTripEntry().getStopTimes().size() - 1; i++) {
            StopEntry currentStop = entry.getTripEntry().getStopTimes().get(i).getStop();
            int currentStopDepartureTime = entry.getTripEntry().getStopTimes().get(
                i).getDepartureTime();

            StopEntry nextStop = entry.getTripEntry().getStopTimes().get(i + 1).getStop();
            int nextStopArrivalTime = entry.getTripEntry().getStopTimes().get(
                i + 1).getArrivalTime();
            int nextStopDepartureTime = entry.getTripEntry().getStopTimes().get(
                i + 1).getDepartureTime();

            if (reliefTime <= currentStopDepartureTime) {
              reliefStop = currentStop;
              break;
            }

            if (reliefTime > currentStopDepartureTime
                && reliefTime <= nextStopDepartureTime) {
              reliefStop = (reliefTime - currentStopDepartureTime <= nextStopArrivalTime
                  - reliefTime) ? currentStop : nextStop;
              break;
            }

            if (i == entry.getTripEntry().getStopTimes().size() - 2
                && reliefTime > nextStopArrivalTime) {
              reliefStop = nextStop;
            }
          }
          if (reliefStop != null)
            trip.put("reliefStopId", reliefStop.getId().toString());
        }

        trips.add(trip);
      }
    }
    return output;
  }

  @Path("/{runId}/run-route")
  @GET
  @Produces("application/json")
  public String getTripsByRunId(@PathParam("runId")
  String runId) throws ParseException {
    Collection<RunTripEntry> runTripEntries = _runService.getRunTripEntriesForRun(runId);
    Map<String, Object> output = new HashMap<String, Object>();
    Gson gson = new Gson();
    JsonObject ob = new JsonObject();
    ob.add("tripsForRun",
        gson.toJsonTree(generateTripsMap(runTripEntries, output)));
    return ob.toString();
  }

  @Path("/{blockId}/block-id")
  @GET
  @Produces("application/json")
  public String getTripsByBlockId(@PathParam("blockId")
  String blockId) throws ParseException {
    Collection<RunTripEntry> runTripEntries = _runService.getRunTripEntriesForBlock(blockId);
    Map<String, Object> output = new HashMap<String, Object>();
    Gson gson = new Gson();
    JsonObject ob = new JsonObject();
    ob.add("blocks", gson.toJsonTree(generateTripsMap(runTripEntries, output)));
    return ob.toString();
  }

  private Date getTimestampAsDate(long timestamp) {
    Calendar cd = Calendar.getInstance();
    cd.setTimeInMillis(timestamp);
    cd.set(Calendar.HOUR_OF_DAY, 0);
    cd.set(Calendar.MINUTE, 0);
    cd.set(Calendar.SECOND, 0);
    cd.set(Calendar.MILLISECOND, 0);
    return cd.getTime();
  }
}