package org.onebusaway.nyc.gtfsrt.tds;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.LocalizedServiceId;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.FrequencyEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.RouteCollectionEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.RouteEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;

import java.util.ArrayList;
import java.util.List;

public class TripEntryImplStub implements TripEntry {

    private Trip trip;
    private double tripDistance;
    private List<StopTimeEntry> stopTimeEntries;

    public TripEntryImplStub(Trip trip) {
        this.trip = trip;
    }

    @Override
    public AgencyAndId getId() {
        return trip.getId();
    }

    @Override
    public RouteEntry getRoute() {
        throw new UnsupportedOperationException();
    }

    @Override
    public RouteCollectionEntry getRouteCollection() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDirectionId() {
        return trip.getDirectionId();
    }

    @Override
    public BlockEntry getBlock() {
        throw new UnsupportedOperationException();
    }

    @Override
    public LocalizedServiceId getServiceId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public AgencyAndId getShapeId() {
        return trip.getShapeId();
    }

    @Override
    public List<StopTimeEntry> getStopTimes() {
        if (stopTimeEntries != null)
            return stopTimeEntries;
        throw new UnsupportedOperationException();
    }

    @Override
    public double getTotalTripDistance() {
        return tripDistance;
    }

    public void setTotalTripDistance(double tripDistance) {
        this.tripDistance = tripDistance;
    }

    @Override
    public FrequencyEntry getFrequencyLabel() {
        throw new UnsupportedOperationException();
    }

    public Trip getTrip() {
        return trip;
    }
}
