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

package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.gtfs.model.AgencyAndId;

import org.apache.commons.lang.StringUtils;

import org.apache.commons.lang.builder.CompareToBuilder;

import java.util.Set;

public class RunResults implements Comparable<RunResults> {

  final private String assignedRunId;
  final private Set<String> fuzzyMatches;
  final private Set<AgencyAndId> routeIds;
  final private Integer bestFuzzyDist;

  private int _hash = 0;

  public RunResults(String assignedRunId, Set<String> fuzzyMatches,
      Integer bestFuzzyDist, Set<AgencyAndId> routeIds) {
    this.assignedRunId = assignedRunId;
    this.fuzzyMatches = fuzzyMatches;
    this.bestFuzzyDist = bestFuzzyDist;
    this.routeIds = routeIds;
  }

  public boolean hasRunResults() {
    return !(StringUtils.isEmpty(assignedRunId) && fuzzyMatches.isEmpty() && bestFuzzyDist == null);
  }

  public String getAssignedRunId() {
    return assignedRunId;
  }

  public Set<String> getFuzzyMatches() {
    return fuzzyMatches;
  }

  public Integer getBestFuzzyDist() {
    return bestFuzzyDist;
  }

  @Override
  public int compareTo(RunResults o2) {
    if (this == o2)
      return 0;

    final int res = new CompareToBuilder().append(assignedRunId,
        o2.assignedRunId).append(fuzzyMatches, o2.fuzzyMatches).append(
        bestFuzzyDist, o2.bestFuzzyDist).toComparison();

    return res;
  }

  @Override
  public int hashCode() {
    if (_hash != 0)
      return _hash;

    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((assignedRunId == null) ? 0 : assignedRunId.hashCode());
    result = prime * result
        + ((bestFuzzyDist == null) ? 0 : bestFuzzyDist.hashCode());
    result = prime * result
        + ((fuzzyMatches == null) ? 0 : fuzzyMatches.hashCode());
    _hash = result;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof RunResults)) {
      return false;
    }
    final RunResults other = (RunResults) obj;
    if (assignedRunId == null) {
      if (other.assignedRunId != null) {
        return false;
      }
    } else if (!assignedRunId.equals(other.assignedRunId)) {
      return false;
    }
    if (bestFuzzyDist == null) {
      if (other.bestFuzzyDist != null) {
        return false;
      }
    } else if (!bestFuzzyDist.equals(other.bestFuzzyDist)) {
      return false;
    }
    if (fuzzyMatches == null) {
      if (other.fuzzyMatches != null) {
        return false;
      }
    } else if (!fuzzyMatches.equals(other.fuzzyMatches)) {
      return false;
    }
    return true;
  }

  public Set<AgencyAndId> getRouteIds() {
    return routeIds;
  }
}