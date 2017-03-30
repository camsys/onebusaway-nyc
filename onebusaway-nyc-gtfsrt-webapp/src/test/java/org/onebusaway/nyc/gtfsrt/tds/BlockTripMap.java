package org.onebusaway.nyc.gtfsrt.tds;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;

import java.util.Collection;
import java.util.Comparator;
import java.util.SortedSet;

public class BlockTripMap {
    private GtfsRelationalDao _dao;
    private SortedSetMultimap<String, TripEntryImplStub> _blockTripMap;

    private static final Comparator<String> KEY_CMP = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            return o1.compareTo(o2);
        }
    };

    private final Comparator<TripEntryImplStub> VAL_CMP = new Comparator<TripEntryImplStub> () {
        @Override
        public int compare(TripEntryImplStub o1, TripEntryImplStub o2) {
            StopTime st1 = _dao.getStopTimesForTrip(o1.getTrip()).get(0);
            StopTime st2 = _dao.getStopTimesForTrip(o2.getTrip()).get(0);
            return st1.getDepartureTime() - st2.getDepartureTime();
        }
    };

    public BlockTripMap(GtfsRelationalDao dao) {
        _dao = dao;
        _blockTripMap = TreeMultimap.create(KEY_CMP, VAL_CMP);
    }

    public SortedSet<? extends TripEntry> getOrderedBlockTripsForTrip(Trip trip) {
        String block = trip.getBlockId();
        if (block == null)
            return null;
        if (!_blockTripMap.containsKey(block)) {
            for (Trip t : getTripsForBlockId(block)) {
                if (tripsShareBlock(trip, t)) {
                    _blockTripMap.put(block, toTestTripEntryImpl(t));
                }
            }
        }
        return _blockTripMap.get(block);
    }

    // TODO- is this the right way to treat service ID?
    private static boolean tripsShareBlock(Trip s, Trip t) {
        return (s.getBlockId().equals(t.getBlockId()))
                && s.getServiceId().equals(t.getServiceId());
    }

    private TripEntryImplStub toTestTripEntryImpl (Trip trip) {
        TripEntryImplStub entry = new TripEntryImplStub(trip);
        double distance = 0;
        for (StopTime st : _dao.getStopTimesForTrip(trip)) {
            distance += st.getShapeDistTraveled(); // ft or meters
        }
        entry.setTotalTripDistance(distance);
        return entry;
    }

    // need our own getTripsForBlockId because we add the blocks after GTFS is read in
    private Multimap<String, Trip> _tripsForBlockId;
    private Collection<Trip> getTripsForBlockId(String id) {
        if (_tripsForBlockId == null) {
            _tripsForBlockId = ArrayListMultimap.create();
            for (Trip trip : _dao.getAllTrips()) {
                if (id.equals(trip.getBlockId())) {
                    _tripsForBlockId.put(id, trip);
                }
            }
        }
        return _tripsForBlockId.get(id);
    }
}
