package org.onebusaway.nyc.vehicle_tracking.impl.sort;

import org.onebusaway.transit_data_federation.bundle.tasks.transit_graph.FrequencyComparator;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

import java.util.Comparator;

public final class BlockInstanceComparator implements Comparator<BlockInstance> {

  private BlockInstanceComparator() {

  }

  static final private FrequencyComparator _frequencyComparator = new FrequencyComparator();
  public static final BlockInstanceComparator INSTANCE = new BlockInstanceComparator();

  @Override
  public int compare(BlockInstance o1, BlockInstance o2) {
    if (o1 == o2)
      return 0;

    return ComparisonChain.start().compare(o1.getBlock().getBlock().getId(),
        o2.getBlock().getBlock().getId()).compare(
        o1.getBlock().getServiceIds(), o2.getBlock().getServiceIds()).compare(
        o1.getServiceDate(), o2.getServiceDate()).compare(o1.getFrequency(),
        o2.getFrequency(), Ordering.from(_frequencyComparator).nullsLast()).result();
  }

}
