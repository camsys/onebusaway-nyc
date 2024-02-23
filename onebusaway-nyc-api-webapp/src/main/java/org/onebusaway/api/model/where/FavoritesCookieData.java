package org.onebusaway.api.model.where;

import org.onebusaway.api.web.actions.api.where.FavoritesController;
import org.onebusaway.api.web.mapping.formatting.CookieDataFormatter;

import java.util.ArrayList;
import java.util.List;


public class FavoritesCookieData extends CookieData {
    public List<String> routes = new ArrayList<>();
    public List<String> stops = new ArrayList<>();

    public List<String> getRoutes() {
        return routes;
    }

    public List<String> getStops() {
        return stops;
    }

    public void setRoutes(List<String> routes) {
        this.routes = routes;
    }

    public void setStops(List<String> stops) {
        this.stops = stops;
    }

    @Override
    public String toString() {
        return CookieDataFormatter.toString(this);
    }
}
