package org.onebusaway.nyc.webapp.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.onebusaway.geocoder.model.GeocoderResult;
import org.onebusaway.geocoder.services.GeocoderService;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.webapp.model.AvailableRoute;
import org.onebusaway.nyc.webapp.model.DistanceAway;
import org.onebusaway.nyc.webapp.model.StopItem;
import org.onebusaway.nyc.webapp.model.search.RouteSearchResult;
import org.onebusaway.nyc.webapp.model.search.SearchResult;
import org.onebusaway.nyc.webapp.model.search.StopSearchResult;
import org.onebusaway.nyc.webapp.service.NycSearchService;
import org.onebusaway.presentation.services.ServiceAreaService;
import org.onebusaway.transit_data.model.ArrivalAndDepartureBean;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.NameBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.RoutesBean;
import org.onebusaway.transit_data.model.SearchQueryBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopGroupBean;
import org.onebusaway.transit_data.model.StopGroupingBean;
import org.onebusaway.transit_data.model.StopWithArrivalsAndDeparturesBean;
import org.onebusaway.transit_data.model.StopsBean;
import org.onebusaway.transit_data.model.StopsForRouteBean;
import org.onebusaway.transit_data.model.TripStopTimeBean;
import org.onebusaway.transit_data.model.TripStopTimesBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsInclusionBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import org.onebusaway.transit_data.model.trips.TripsForRouteQueryBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NycSearchServiceImpl implements NycSearchService {

  private final static Pattern routePattern = Pattern.compile("(?:[BMQS]|BX)[0-9]+", Pattern.CASE_INSENSITIVE);

  // when querying for stops from a lat/lng, use this distance in meters
  private double distanceToStops = 200;

  private WebappIdParser idParser = new WebappIdParser();

  @Autowired
  private TransitDataService transitService;

  @Autowired
  private GeocoderService geocoderService;

  @Autowired
  private ServiceAreaService serviceArea;

  private static final SearchResultComparator searchResultComparator = new SearchResultComparator();

  @Override
  public List<SearchResult> search(String q) {
    List<SearchResult> results = new ArrayList<SearchResult>();

    if (q == null)
      return results;
    q = q.trim();
    if (q.isEmpty())
      return results;

    String[] fields = q.split(" ");
    if (fields.length == 1) {
      // we are probably a query for a route or a stop
      if (isStop(q)) {
        SearchQueryBean queryBean = makeSearchQuery(q);
        StopsBean stops = transitService.getStops(queryBean);

        for (StopBean stopBean : stops.getStops()) {
          StopSearchResult stopSearchResult = makeStopSearchResult(stopBean);
          results.add(stopSearchResult);
        }
      } else if (isRoute(q)) {
        SearchQueryBean queryBean = makeSearchQuery(q);
        RoutesBean routes = transitService.getRoutes(queryBean);

        for (RouteBean routeBean : routes.getRoutes()) {
          String routeId = routeBean.getId();
          String routeShortName = routeBean.getShortName();
          String routeLongName = routeBean.getLongName();
          // FIXME last update time
          String lastUpdate = "one minute ago";

          // create lookups keyed by directionId to the different bits of
          // information we need for the response
          Map<String, List<StopBean>> directionIdToStopBeans = createDirectionToStopBeansMap(routeId);
          Map<String, String> directionIdToHeadsign = new HashMap<String, String>();
          Map<String, String> directionIdToTripId = new HashMap<String, String>();
          // go from trip id to a mapping of stop id -> distance along trip
          Map<String, Map<String, Double>> tripIdToStopDistancesMap = new HashMap<String, Map<String, Double>>();
          Map<String, List<Double>> stopIdToDistanceAways = new HashMap<String, List<Double>>();

          TripsForRouteQueryBean tripQueryBean = makeTripQueryBean(routeId);
          ListBean<TripDetailsBean> tripsForRoute = transitService.getTripsForRoute(tripQueryBean);
          List<TripDetailsBean> tripsList = tripsForRoute.getList();

          for (TripDetailsBean tripDetailsBean : tripsList) {
            TripBean trip = tripDetailsBean.getTrip();
            String tripId = trip.getId();
            String tripDirectionId = trip.getDirectionId();
            String tripHeadsign = trip.getTripHeadsign();
            directionIdToHeadsign.put(tripDirectionId, tripHeadsign);
            directionIdToTripId.put(tripDirectionId, tripId);

            // determine distances along trip for each stop if we haven't gotten
            // this trip yet
            if (!tripIdToStopDistancesMap.containsKey(tripId)) {
              TripStopTimesBean schedule = tripDetailsBean.getSchedule();
              List<TripStopTimeBean> stopTimes = schedule.getStopTimes();
              for (TripStopTimeBean tripStopTimeBean : stopTimes) {
                StopBean stopBean = tripStopTimeBean.getStop();
                String stopId = stopBean.getId();
                double distanceAlongTrip = tripStopTimeBean.getDistanceAlongTrip();

                Map<String, Double> stopIdToDistance = tripIdToStopDistancesMap.get(tripId);
                if (stopIdToDistance == null) {
                  stopIdToDistance = new HashMap<String, Double>();
                  tripIdToStopDistancesMap.put(tripId, stopIdToDistance);
                }
                stopIdToDistance.put(stopId, distanceAlongTrip);
              }
            }

            TripStatusBean tripStatusBean = tripDetailsBean.getStatus();
            if (tripStatusBean == null || !tripStatusBean.isPredicted())
              continue;

            StopBean closestStop = tripStatusBean.getNextStop();
            if (closestStop != null) {
              String closestStopId = closestStop.getId();
              Map<String, Double> stopIdToDistance = tripIdToStopDistancesMap.get(tripId);
              double distanceAlongTrip = tripStatusBean.getDistanceAlongTrip();
              double stopDistanceAlongroute = stopIdToDistance.get(closestStopId);
              double distanceAway = stopDistanceAlongroute - distanceAlongTrip;
              List<Double> stopDistanceAways = stopIdToDistanceAways.get(closestStopId);
              if (stopDistanceAways == null) {
                stopDistanceAways = new ArrayList<Double>();
                stopIdToDistanceAways.put(closestStopId, stopDistanceAways);
              }
              stopDistanceAways.add(distanceAway);
            }
          }

          for (Map.Entry<String, List<StopBean>> directionStopBeansEntry : directionIdToStopBeans.entrySet()) {
            String directionId = directionStopBeansEntry.getKey();
            String tripId = directionIdToTripId.get(directionId);
            String tripHeadsign = directionIdToHeadsign.get(directionId);
            // on the off chance that this happens degrade more gracefully
            if (tripHeadsign == null)
              tripHeadsign = routeLongName;

            List<StopBean> stopBeansList = directionStopBeansEntry.getValue();
            Map<String, Double> stopIdToDistances = tripIdToStopDistancesMap.get(tripId);
            if (stopIdToDistances != null) {
              Comparator<StopBean> stopBeanComparator = new NycSearchServiceImpl.StopBeanComparator(
                  stopIdToDistances);
              Collections.sort(stopBeansList, stopBeanComparator);
            }
            List<StopItem> stopItemsList = new ArrayList<StopItem>();
            for (StopBean stopBean : stopBeansList) {
              String stopId = stopBean.getId();
              List<Double> distances = stopIdToDistanceAways.get(stopId);
              List<Double> meterDistances = null;
              if (distances != null && distances.size() > 0) {
                meterDistances = new ArrayList<Double>(distances.size());
                for (Double feetDistance : distances) {
                  meterDistances.add(metersToFeet(feetDistance));
                }
                Collections.sort(meterDistances);
              }
              StopItem stopItem = new StopItem(stopBean, meterDistances);
              stopItemsList.add(stopItem);
            }
            RouteSearchResult routeSearchResult = new RouteSearchResult(
                routeId, routeShortName, routeLongName, lastUpdate,
                tripHeadsign, directionId, stopItemsList);
            results.add(routeSearchResult);
          }
        }
      }
    } else {
      // we should be either an intersection and route or an intersection

      // if we're a route, set route and the geocoder query appropriately
      String route = null;
      String queryNoRoute = null;
      if (isRoute(fields[0])) {
        route = fields[0];
        StringBuilder sb = new StringBuilder(32);
        for (int i = 1; i < fields.length; i++) {
          sb.append(fields[i]);
          sb.append(" ");
        }
        queryNoRoute = sb.toString().trim();
      } else if (isRoute(fields[fields.length - 1])) {
        route = fields[fields.length - 1];
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < fields.length - 1; i++) {
          sb.append(fields[i]);
          sb.append(" ");
        }
        queryNoRoute = sb.toString().trim();
      }

      if (route != null) {
        // we are a route and intersection
        List<StopsBean> stopsList = fetchStopsFromGeocoder(queryNoRoute);
        for (StopsBean stopsBean : stopsList) {
          for (StopBean stopBean : stopsBean.getStops()) {
            boolean useStop = false;
            List<RouteBean> routes = stopBean.getRoutes();
            for (RouteBean routeBean : routes) {
              String idNoAgency = idParser.parseIdWithoutAgency(routeBean.getId());
              if (idNoAgency.equalsIgnoreCase(route)) {
                useStop = true;
                break;
              }
            }
            if (useStop) {
              StopSearchResult stopSearchResult = makeStopSearchResult(stopBean);
              results.add(stopSearchResult);
            }
          }
        }
      } else {
        // try an intersection search
        List<StopsBean> stopsBeanList = fetchStopsFromGeocoder(q);
        for (StopsBean stopsBean : stopsBeanList) {
          List<StopBean> stopsList = stopsBean.getStops();
          for (StopBean stopBean : stopsList) {
            StopSearchResult stopSearchResult = makeStopSearchResult(stopBean);
            results.add(stopSearchResult);
          }
        }
      }
    }

    return sortSearchResults(results);
  }

  private Map<String, List<StopBean>> createDirectionToStopBeansMap(
      String routeId) {
    Map<String, List<StopBean>> directionIdToStopBeans = new HashMap<String, List<StopBean>>();

    StopsForRouteBean stopsForRoute = transitService.getStopsForRoute(routeId);
    List<StopBean> stopBeansList = stopsForRoute.getStops();

    List<StopGroupingBean> stopGroupings = stopsForRoute.getStopGroupings();
    for (StopGroupingBean stopGroupingBean : stopGroupings) {
      List<StopGroupBean> stopGroups = stopGroupingBean.getStopGroups();
      for (StopGroupBean stopGroupBean : stopGroups) {
        NameBean name = stopGroupBean.getName();
        String type = name.getType();
        if (!type.equals("destination"))
          continue;

        List<StopBean> stopsForDirection = new ArrayList<StopBean>();
        String directionId = stopGroupBean.getId();
        Set<String> stopIds = new HashSet<String>(stopGroupBean.getStopIds());

        for (StopBean stopBean : stopBeansList) {
          String stopBeanId = stopBean.getId();
          if (stopIds.contains(stopBeanId))
            stopsForDirection.add(stopBean);
        }
        directionIdToStopBeans.put(directionId, stopsForDirection);
      }
    }
    return directionIdToStopBeans;
  }

  private TripsForRouteQueryBean makeTripQueryBean(String routeId) {
    TripsForRouteQueryBean tripRouteQueryBean = new TripsForRouteQueryBean();
    tripRouteQueryBean.setRouteId(routeId);
    tripRouteQueryBean.setMaxCount(100);
    tripRouteQueryBean.setTime(new Date().getTime());
    TripDetailsInclusionBean inclusionBean = new TripDetailsInclusionBean();
    inclusionBean.setIncludeTripBean(true);
    inclusionBean.setIncludeTripSchedule(true);
    inclusionBean.setIncludeTripStatus(true);
    tripRouteQueryBean.setInclusion(inclusionBean);
    return tripRouteQueryBean;
  }

  private boolean isStop(String s) {
    // stops are 6 digits
    int n = s.length();
    if (n != 6)
      return false;
    for (int i = 0; i < n; i++) {
      if (!Character.isDigit(s.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  private boolean isRoute(String s) {
    Matcher matcher = routePattern.matcher(s);
    return matcher.matches();
  }

  private List<StopsBean> fetchStopsFromGeocoder(String q) {
    List<StopsBean> result = new ArrayList<StopsBean>();
    List<GeocoderResult> geocoderResults = geocoderService.geocode(q).getResults();
    for (GeocoderResult geocoderResult : geocoderResults) {
      double lat = geocoderResult.getLatitude();
      double lng = geocoderResult.getLongitude();
      CoordinateBounds bounds = SphericalGeometryLibrary.bounds(lat, lng,
          distanceToStops);

      // and add any stops for it
      SearchQueryBean searchQueryBean = makeSearchQuery(bounds);
      searchQueryBean.setMaxCount(100);
      StopsBean stops = transitService.getStops(searchQueryBean);
      result.add(stops);
    }
    return result;
  }

  private StopSearchResult makeStopSearchResult(StopBean stopBean) {
    String stopId = stopBean.getId();
    List<Double> latLng = Arrays.asList(new Double[] {
        stopBean.getLat(), stopBean.getLon()});
    String stopName = stopBean.getName();

    Date now = new Date();
    int minutesBefore = 5;
    int minutesAfter = 35;
    Map<String, List<DistanceAway>> routeIdToDistanceAways = new HashMap<String, List<DistanceAway>>();

    StopWithArrivalsAndDeparturesBean stopWithArrivalsAndDepartures = transitService.getStopWithArrivalsAndDepartures(
        stopId, now, minutesBefore, minutesAfter);
    List<ArrivalAndDepartureBean> arrivalsAndDepartures = stopWithArrivalsAndDepartures.getArrivalsAndDepartures();

    for (ArrivalAndDepartureBean arrivalAndDepartureBean : arrivalsAndDepartures) {
      if (arrivalAndDepartureBean.isPredicted()) {
        double distanceFromStopInMeters = arrivalAndDepartureBean.getDistanceFromStop();
        if (distanceFromStopInMeters < 0)
          continue;
        int distanceFromStopInFeet = (int) this.metersToFeet(distanceFromStopInMeters);
        int numberOfStopsAway = arrivalAndDepartureBean.getNumberOfStopsAway();
        String routeId = arrivalAndDepartureBean.getTrip().getRoute().getId();
        List<DistanceAway> distances = routeIdToDistanceAways.get(routeId);
        if (distances == null) {
          distances = new ArrayList<DistanceAway>();
          routeIdToDistanceAways.put(routeId, distances);
        }
        distances.add(new DistanceAway(numberOfStopsAway, distanceFromStopInFeet));
      }
    }

    List<AvailableRoute> availableRoutes = new ArrayList<AvailableRoute>();
    List<RouteBean> stopRouteBeans = stopBean.getRoutes();
    for (RouteBean routeBean : stopRouteBeans) {
      String shortName = routeBean.getShortName();
      String longName = routeBean.getLongName();
      String routeId = routeBean.getId();
      List<DistanceAway> distances = routeIdToDistanceAways.get(routeId);
      if (distances == null)
        distances = Collections.emptyList();

      Collections.sort(distances);

      AvailableRoute availableRoute = new AvailableRoute(shortName, longName,
          distances);
      availableRoutes.add(availableRoute);
    }
    StopSearchResult stopSearchResult = new StopSearchResult(stopId, stopName,
        latLng, availableRoutes);
    return stopSearchResult;
  }

  private double metersToFeet(double meters) {
    double feetInMeters = 3.28083989501312;
    return meters * feetInMeters;
  }

  private SearchQueryBean makeSearchQuery(String q) {
    return makeSearchQuery(q, serviceArea.getServiceArea());
  }

  private SearchQueryBean makeSearchQuery(CoordinateBounds bounds) {
    return makeSearchQuery(null, bounds);
  }

  private SearchQueryBean makeSearchQuery(String q, CoordinateBounds bounds) {
    SearchQueryBean queryBean = new SearchQueryBean();
    queryBean.setType(SearchQueryBean.EQueryType.BOUNDS_OR_CLOSEST);
    queryBean.setBounds(bounds);
    queryBean.setMaxCount(5);
    if (q != null)
      queryBean.setQuery(q);
    return queryBean;
  }

  private List<SearchResult> sortSearchResults(List<SearchResult> searchResults) {
    Collections.sort(searchResults, NycSearchServiceImpl.searchResultComparator);
    return searchResults;
  }

  public static class StopBeanComparator implements Comparator<StopBean> {

    private final Map<String, Double> stopIdToDistances;

    public StopBeanComparator(Map<String, Double> stopIdToDistances) {
      this.stopIdToDistances = stopIdToDistances;
    }

    @Override
    public int compare(StopBean o1, StopBean o2) {
      Double d1 = stopIdToDistances.get(o1.getId());
      Double d2 = stopIdToDistances.get(o2.getId());
      if (d1 == null)
        d1 = Double.valueOf(0);
      if (d2 == null)
        d2 = Double.valueOf(0);
      return d1.compareTo(d2);
    }

  }

  public static class SearchResultComparator implements
      Comparator<SearchResult> {

    @Override
    public int compare(SearchResult o1, SearchResult o2) {
      if ((o1 instanceof RouteSearchResult) && (o2 instanceof StopSearchResult))
        return -1;
      else if ((o1 instanceof StopSearchResult)
          && (o2 instanceof RouteSearchResult))
        return 1;
      else if ((o1 instanceof RouteSearchResult)
          && (o2 instanceof RouteSearchResult))
        return ((RouteSearchResult) o1).getName().compareTo(
            ((RouteSearchResult) o2).getName());
      else if ((o1 instanceof StopSearchResult)
          && (o2 instanceof StopSearchResult))
        return ((StopSearchResult) o1).getName().compareTo(
            ((StopSearchResult) o2).getName());
      else
        throw new IllegalStateException("Unknown search result types");
    }
  }
}
