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

import java.util.Comparator;

import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;

import com.google.common.collect.ComparisonChain;

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
