package org.onebusaway.nyc.transit_data_federation.model.nyc;

public enum SupplementalRouteType {
    FEEDER,
    GRID,
    EXPRESS,
    UNRECOGNIZED_TYPE,
    UNIDENTIFIED;

    public static SupplementalRouteType fromString(String value) {
        if (value == null || value.isBlank()) {
            return UNIDENTIFIED;
        }
        try {
            return SupplementalRouteType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNRECOGNIZED_TYPE;
        }
    }
}
