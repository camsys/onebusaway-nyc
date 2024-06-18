package org.onebusaway.nyc.webapp.actions.api;

import org.onebusaway.geospatial.model.EncodedPolylineBean;
import org.onebusaway.nyc.presentation.model.SearchResult;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.presentation.service.search.SearchService;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.webapp.actions.api.model.RouteDirection;
import org.onebusaway.nyc.webapp.actions.api.model.RouteResult;
import org.onebusaway.nyc.webapp.actions.api.model.StopOnRoute;
import org.onebusaway.transit_data.model.*;
import org.onebusaway.util.AgencyAndIdLibrary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchResultFactoryV2Impl extends SearchResultFactoryImpl{



    private SearchService _searchService;

    private NycTransitDataService _nycTransitDataService;

    private RealtimeService _realtimeService;

    public SearchResultFactoryV2Impl(SearchService searchService, NycTransitDataService nycTransitDataService, RealtimeService realtimeService) {
        super(searchService,nycTransitDataService,realtimeService);
        _searchService = searchService;
        _nycTransitDataService = nycTransitDataService;
        _realtimeService = realtimeService;
    }

    @Override
    public SearchResult getRouteResult(RouteBean routeBean) {
        List<RouteDirection> directions = new ArrayList<RouteDirection>();

        StopsForRouteBean stopsForRoute = _nycTransitDataService.getStopsForRoute(routeBean.getId());

        // create stop ID->stop bean map
        Map<String, StopBean> stopIdToStopBeanMap = new HashMap<String, StopBean>();
        for(StopBean stopBean : stopsForRoute.getStops()) {
            stopIdToStopBeanMap.put(stopBean.getId(), stopBean);
        }

        List<StopGroupingBean> stopGroupings = stopsForRoute.getStopGroupings();
        for (StopGroupingBean stopGroupingBean : stopGroupings) {
            for (StopGroupBean stopGroupBean : stopGroupingBean.getStopGroups()) {
                NameBean name = stopGroupBean.getName();
                String type = name.getType();

                if (!type.equals("destination"))
                    continue;

                List<String> polylines = new ArrayList<String>();
                for(EncodedPolylineBean polyline : stopGroupBean.getPolylines()) {
                    polylines.add(polyline.getPoints());
                }

                Boolean hasUpcomingScheduledService =
                        _nycTransitDataService.routeHasUpcomingScheduledService((routeBean.getAgency()!=null?routeBean.getAgency().getId():null), System.currentTimeMillis(), routeBean.getId(), stopGroupBean.getId());

                // if there are buses on route, always have "scheduled service"
                Boolean routeHasVehiclesInService =
                        _realtimeService.getVehiclesInServiceForRoute(routeBean.getId(), stopGroupBean.getId(), System.currentTimeMillis());

                if(routeHasVehiclesInService) {
                    hasUpcomingScheduledService = true;
                }

                List<StopOnRoute> _stops = new ArrayList<StopOnRoute>();
                if(!stopGroupBean.getStopIds().isEmpty()) {
                    for(String stopId : stopGroupBean.getStopIds()) {
                        String agencyId = AgencyAndIdLibrary.convertFromString(routeBean.getId()).getAgencyId();
                        if (_nycTransitDataService.stopHasRevenueServiceOnRoute(agencyId, stopId,
                                stopsForRoute.getRoute().getId(), stopGroupBean.getId())) {
                            _stops.add(new StopOnRoute(stopIdToStopBeanMap.get(stopId)));
                        }
                    }
                }


                directions.add(new RouteDirection(stopGroupBean, polylines, _stops, hasUpcomingScheduledService));
            }
        }

        return new RouteResult(routeBean, directions);
    }
}
