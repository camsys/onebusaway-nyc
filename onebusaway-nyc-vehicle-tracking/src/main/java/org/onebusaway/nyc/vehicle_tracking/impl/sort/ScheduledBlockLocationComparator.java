package org.onebusaway.nyc.vehicle_tracking.impl.sort;

import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;

import com.google.common.collect.ComparisonChain;

import java.util.Comparator;

public final class ScheduledBlockLocationComparator implements
    Comparator<ScheduledBlockLocation> {

  public final static ScheduledBlockLocationComparator INSTANCE = new ScheduledBlockLocationComparator();

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
