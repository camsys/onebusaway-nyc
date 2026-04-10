/**
 * Copyright (C) 2024 Metropolitan Transportation Authority
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
package org.onebusaway.nyc.webapp.actions.api;

import org.onebusaway.nyc.geocoder.service.NycGeocoderResult;
import org.onebusaway.nyc.presentation.model.SearchResult;
import org.onebusaway.nyc.presentation.model.SearchResultCollection;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.presentation.service.search.SearchService;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.webapp.actions.api.model.*;
import org.onebusaway.transit_data.model.*;
import org.onebusaway.transit_data.model.trip_mods.ShapeModificationDiff;
import org.onebusaway.transit_data.model.trip_mods.StopChangeDiff;
import org.onebusaway.transit_data.model.trip_mods.TripModificationDiff;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.util.AgencyAndIdLibrary;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class SearchResultFactoryV2Impl extends SearchResultFactoryImpl {

    private SearchService _searchService;

    private NycTransitDataService _nycTransitDataService;

    private RealtimeService _realtimeService;

    public SearchResultFactoryV2Impl(SearchService searchService, NycTransitDataService nycTransitDataService, RealtimeService realtimeService) {
        super(searchService, nycTransitDataService, realtimeService);
        _searchService = searchService;
        _nycTransitDataService = nycTransitDataService;
        _realtimeService = realtimeService;
    }

    @Override
    public SearchResult getRouteResult(RouteBean routeBean) {
        List<RouteDirection> directions = new ArrayList<>();

        StopsForRouteBean stopsForRoute = _nycTransitDataService.getStopsForRoute(routeBean.getId());

        Map<String, StopBean> stopIdToStopBeanMap = new HashMap<>();
        for (StopBean stopBean : stopsForRoute.getStops()) {
            stopIdToStopBeanMap.put(stopBean.getId(), stopBean);
        }

        // Fetch and filter diffs for this route once, then group by direction ID
        Collection<TripModificationDiff> routeDiffs = filterDiffsByRoute(
                _nycTransitDataService.getAllTripModificationDiffs(
                        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))),
                routeBean.getId());
        Map<String, List<TripModificationDiff>> diffsByDirection = groupDiffsByDirection(routeDiffs);

        List<StopGroupingBean> stopGroupings = stopsForRoute.getStopGroupings();
        for (StopGroupingBean stopGroupingBean : stopGroupings) {
            for (StopGroupBean stopGroupBean : stopGroupingBean.getStopGroups()) {
                NameBean name = stopGroupBean.getName();
                if (!name.getType().equals("destination"))
                    continue;

                String directionId = stopGroupBean.getId();
                List<TripModificationDiff> directionDiffs = diffsByDirection.getOrDefault(directionId, Collections.emptyList());

                List<PolylineWithStatus> polylines = buildPolylinesForDirection(stopGroupBean, directionDiffs);
                Map<String, String> stopDetourStatusMap = buildStopDetourStatusMap(directionDiffs);

                Boolean hasUpcomingScheduledService =
                        _nycTransitDataService.routeHasUpcomingScheduledService(
                                (routeBean.getAgency() != null ? routeBean.getAgency().getId() : null),
                                System.currentTimeMillis(), routeBean.getId(), directionId);

                Boolean routeHasVehiclesInService =
                        _realtimeService.getVehiclesInServiceForRoute(routeBean.getId(), directionId, System.currentTimeMillis());

                if (routeHasVehiclesInService) {
                    hasUpcomingScheduledService = true;
                }

                List<StopOnRoute> stops = new ArrayList<>();
                if (!stopGroupBean.getStopIds().isEmpty()) {
                    String agencyId = AgencyAndIdLibrary.convertFromString(routeBean.getId()).getAgencyId();
                    for (String stopId : stopGroupBean.getStopIds()) {
                        if (_nycTransitDataService.stopHasRevenueServiceOnRoute(agencyId, stopId,
                                stopsForRoute.getRoute().getId(), directionId)) {
                            String detourStatus = stopDetourStatusMap.getOrDefault(stopId, "canonical");
                            stops.add(new StopOnRouteV2(stopIdToStopBeanMap.get(stopId), detourStatus));
                        }
                    }
                }

                directions.add(new RouteDirection(stopGroupBean, polylines, stops, hasUpcomingScheduledService));
            }
        }

        return new RouteResult(routeBean, directions);
    }

    @Override
    public SearchResult getStopResult(StopBean stopBean, Set<RouteBean> routeFilter) {
        List<RouteAtStop> routesAtStop = new ArrayList<>();
        Map<String, StopBean> stopIdToStopBeanMap = new HashMap<>();

        for (RouteBean routeBean : stopBean.getRoutes()) {
            StopsForRouteBean stopsForRoute = _nycTransitDataService.getStopsForRoute(routeBean.getId());
            for (StopBean stopBeanForRoute : stopsForRoute.getStops()) {
                stopIdToStopBeanMap.put(stopBeanForRoute.getId(), stopBeanForRoute);
            }

            // Fetch and filter diffs for this route once, then group by direction ID
            Collection<TripModificationDiff> routeDiffs = filterDiffsByRoute(
                    _nycTransitDataService.getAllTripModificationDiffs(
                            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))),
                    routeBean.getId());
            Map<String, List<TripModificationDiff>> diffsByDirection = groupDiffsByDirection(routeDiffs);

            List<RouteDirection> directions = new ArrayList<>();
            List<StopGroupingBean> stopGroupings = stopsForRoute.getStopGroupings();
            for (StopGroupingBean stopGroupingBean : stopGroupings) {
                for (StopGroupBean stopGroupBean : stopGroupingBean.getStopGroups()) {
                    NameBean name = stopGroupBean.getName();
                    if (!name.getType().equals("destination"))
                        continue;

                    String directionId = stopGroupBean.getId();
                    List<TripModificationDiff> directionDiffs = diffsByDirection.getOrDefault(directionId, Collections.emptyList());

                    List<PolylineWithStatus> polylines = buildPolylinesForDirection(stopGroupBean, directionDiffs);
                    Map<String, String> stopDetourStatusMap = buildStopDetourStatusMap(directionDiffs);

                    Boolean hasUpcomingScheduledService = null;

                    // Only check service if this direction actually serves this stop
                    if (stopGroupBean.getStopIds().contains(stopBean.getId())) {
                        hasUpcomingScheduledService =
                                _nycTransitDataService.stopHasUpcomingScheduledService(
                                        (routeBean.getAgency() != null ? routeBean.getAgency().getId() : null),
                                        System.currentTimeMillis(), stopBean.getId(),
                                        routeBean.getId(), directionId);

                        Boolean routeHasVehiclesInService =
                                _realtimeService.getVehiclesInServiceForStopAndRoute(stopBean.getId(), routeBean.getId(), System.currentTimeMillis());

                        if (routeHasVehiclesInService) {
                            hasUpcomingScheduledService = true;
                        }
                    }

                    List<StopOnRoute> stops = new ArrayList<>();
                    if (!stopGroupBean.getStopIds().isEmpty()) {
                        String agencyId = AgencyAndIdLibrary.convertFromString(routeBean.getId()).getAgencyId();
                        for (String stopId : stopGroupBean.getStopIds()) {
                            if (_nycTransitDataService.stopHasRevenueServiceOnRoute(agencyId, stopId,
                                    stopsForRoute.getRoute().getId(), directionId)) {
                                String detourStatus = stopDetourStatusMap.getOrDefault(stopId, "canonical");
                                stops.add(new StopOnRouteV2(stopIdToStopBeanMap.get(stopId), detourStatus));
                            }
                        }
                    }

                    directions.add(new RouteDirection(stopGroupBean, polylines, stops, hasUpcomingScheduledService));
                }
            }

            RouteAtStop routeAtStop = new RouteAtStop(routeBean, directions);
            routesAtStop.add(routeAtStop);
        }

        return new StopResult(stopBean, routesAtStop);
    }

    /**
     * Groups route-filtered diffs by the direction ID of their trip.
     */
    private Map<String, List<TripModificationDiff>> groupDiffsByDirection(Collection<TripModificationDiff> diffs) {
        Map<String, List<TripModificationDiff>> map = new HashMap<>();
        for (TripModificationDiff diff : diffs) {
            TripBean trip = _nycTransitDataService.getTrip(diff.getTripId());
            if (trip != null && trip.getDirectionId() != null) {
                map.computeIfAbsent(trip.getDirectionId(), k -> new ArrayList<>()).add(diff);
            }
        }
        return map;
    }

    /**
     * Builds the polyline list for a direction. If any diff for this direction
     * has a shape modification, returns the four annotated segments
     * (prefix/canonical, original/removed, replacement/detour, suffix/canonical).
     * Otherwise wraps the stop-group polylines as canonical.
     */
    private List<PolylineWithStatus> buildPolylinesForDirection(StopGroupBean stopGroupBean,
                                                                List<TripModificationDiff> directionDiffs) {
        Optional<ShapeModificationDiff> shapeDiff = directionDiffs.stream()
                .map(TripModificationDiff::getShapeDiff)
                .filter(Objects::nonNull)
                .findFirst();

        if (shapeDiff.isPresent()) {
            ShapeModificationDiff sd = shapeDiff.get();
            List<PolylineWithStatus> result = new ArrayList<>();
            if (sd.getPrefixSegmentPolyline() != null)
                result.add(new PolylineWithStatus(sd.getPrefixSegmentPolyline(), "canonical"));
            if (sd.getOriginalSegmentPolyline() != null)
                result.add(new PolylineWithStatus(sd.getOriginalSegmentPolyline(), "removed"));
            if (sd.getReplacementSegmentPolyline() != null)
                result.add(new PolylineWithStatus(sd.getReplacementSegmentPolyline(), "detour"));
            if (sd.getSuffixSegmentPolyline() != null)
                result.add(new PolylineWithStatus(sd.getSuffixSegmentPolyline(), "canonical"));
            return result;
        }

        return stopGroupBean.getPolylines().stream()
                .map(p -> new PolylineWithStatus(p.getPoints(), "canonical"))
                .collect(Collectors.toList());
    }

    /**
     * Builds a stopId -> detourStatus map from the stop change diffs for a direction.
     * Priority: removed > detour > canonical.
     */
    private Map<String, String> buildStopDetourStatusMap(List<TripModificationDiff> directionDiffs) {
        Map<String, String> statusMap = new HashMap<>();
        for (TripModificationDiff diff : directionDiffs) {
            if (diff.getChanges() == null) continue;
            for (StopChangeDiff change : diff.getChanges()) {
                String stopId = change.getStopId();
                String newStatus;
                switch (change.getChangeType()) {
                    case REMOVED: newStatus = "removed"; break;
                    case ADDED:   newStatus = "detour";  break;
                    default:      newStatus = "canonical"; break;
                }
                String existing = statusMap.get(stopId);
                if (existing == null || outranks(newStatus, existing)) {
                    statusMap.put(stopId, newStatus);
                }
            }
        }
        return statusMap;
    }

    /** Returns true if newStatus should take priority over existing. */
    private boolean outranks(String newStatus, String existing) {
        if ("removed".equals(newStatus)) return true;
        if ("detour".equals(newStatus) && "canonical".equals(existing)) return true;
        return false;
    }

    private Collection<TripModificationDiff> filterDiffsByRoute(Collection<TripModificationDiff> diffs, String routeId) {
        if (diffs == null || routeId == null) return Collections.emptyList();
        return diffs.stream()
                .filter(diff -> {
                    TripBean trip = _nycTransitDataService.getTrip(diff.getTripId());
                    return trip != null && trip.getRoute() != null
                            && routeId.equals(trip.getRoute().getId());
                })
                .collect(Collectors.toList());
    }

    @Override
    public SearchResult getGeocoderResult(NycGeocoderResult geocodeResult, Set<RouteBean> routeBean) {

        // get routes data
        List<SearchResult> nearbySearchResults = null;

        if (geocodeResult.isRegion()) {
            nearbySearchResults = _searchService.findRoutesStoppingWithinRegion(geocodeResult.getBounds(), this).getMatches();
        } else {
            nearbySearchResults = _searchService.findRoutesStoppingNearPoint(geocodeResult.getLatitude(),
                    geocodeResult.getLongitude(), this).getMatches();
        }

        // get stops data
        SearchResultCollection searchResultCollection;

        if (geocodeResult.isRegion()) {
            searchResultCollection = _searchService.findRoutesStoppingWithinRegion(
                    geocodeResult.getBounds(), this);
        } else {
            searchResultCollection = _searchService.findStopsNearPoint(geocodeResult.getLatitude(),
                    geocodeResult.getLongitude(), this, routeBean);
        }

        nearbySearchResults.addAll(searchResultCollection.getMatches());

        return new GeocodeResult(geocodeResult, nearbySearchResults);
    }
}
