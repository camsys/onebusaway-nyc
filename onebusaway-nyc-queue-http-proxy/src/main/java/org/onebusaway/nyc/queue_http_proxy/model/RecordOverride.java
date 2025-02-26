package org.onebusaway.nyc.queue_http_proxy.model;

import java.util.Objects;

public class RecordOverride {
    private double lat;
    private double lon;
    private String agency;

    public RecordOverride() {}

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public String getAgency() {
        return agency;
    }

    public void setAgency(String agency) {
        this.agency = agency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecordOverride that = (RecordOverride) o;
        return Double.compare(lat, that.lat) == 0 && Double.compare(lon, that.lon) == 0 && Objects.equals(agency, that.agency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lat, lon, agency);
    }

    @Override
    public String toString() {
        return "RecordOverride{" +
                "lat=" + lat +
                ", lon=" + lon +
                ", agency='" + agency + '\'' +
                '}';
    }
}