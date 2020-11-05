/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onebusaway.nyc.vehicle_tracking.impl.sort;

import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;

import com.google.common.collect.ComparisonChain;

import java.util.Comparator;

public final class BlockInstanceComparator implements Comparator<BlockInstance> {

  public static final BlockInstanceComparator INSTANCE = new BlockInstanceComparator();

  @Override
  public int compare(BlockInstance o1, BlockInstance o2) {
    if (o1 == o2)
      return 0;

    return ComparisonChain.start().compare(o1.getBlock().getBlock().getId(),
        o2.getBlock().getBlock().getId()).compare(
        o1.getBlock().getServiceIds(), o2.getBlock().getServiceIds()).compare(
        o1.getServiceDate(), o2.getServiceDate()).result();
  }

}
