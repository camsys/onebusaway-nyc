package org.onebusaway.nyc.vehicle_tracking.impl.sort;

import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;

import com.google.common.collect.ComparisonChain;

import java.util.Comparator;

public final class ScheduledBlockLocationComparator implements
    Comparator<ScheduledBlockLocation> {

  private ScheduledBlockLocationComparator() {

  }

  public final static ScheduledBlockLocationComparator INSTANCE = new ScheduledBlockLocationComparator();

  private static class TripIdComparator implements Comparator<TripEntry> {
    @Override
    public int compare(TripEntry o1, TripEntry o2) {
      return o1.getId().compareTo(o2.getId());
    }
  }

  static final private TripIdComparator _tripIdComparator = new TripIdComparator();

  @Override
  public int compare(ScheduledBlockLocation arg0, ScheduledBlockLocation arg1) {
    if (arg0 == arg1)
      return 0;

    return ComparisonChain.start().compare(
        arg0.getActiveTrip().getTrip().getId(),
        arg1.getActiveTrip().getTrip().getId()).compare(
        arg0.getScheduledTime(), arg1.getScheduledTime()).result();
  }

}
