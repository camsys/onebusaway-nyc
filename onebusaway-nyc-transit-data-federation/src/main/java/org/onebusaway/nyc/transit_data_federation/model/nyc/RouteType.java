package org.onebusaway.nyc.transit_data_federation.model.nyc;

public enum RouteType {
    FEEDER,
    GRID,
    EXPRESS,
    UNIDENTIFIED;

    public static RouteType fromString(String value) {
        if (value == null || value.isBlank()) {
            return UNIDENTIFIED;
        }
        try {
            return RouteType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNIDENTIFIED;
        }
    }
}