/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.vehicle_tracking.impl.inference.state;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.realtime.api.EVehiclePhase;

public final class JourneyState {

  private final EVehiclePhase phase;

  private final Object data;

  private JourneyState(EVehiclePhase phase, Object data) {
    this.phase = phase;
    this.data = data;
  }

  public EVehiclePhase getPhase() {
    return phase;
  }

  @SuppressWarnings("unchecked")
  public <T> T getData() {
    return (T) data;
  }

  @Override
  public String toString() {
    return phase.toString();
  }

  public static JourneyState atBase() {
    return new JourneyState(EVehiclePhase.AT_BASE, null);
  }

  public static JourneyState deadheadBefore(CoordinatePoint journeyStart) {
    JourneyStartState jss = new JourneyStartState(journeyStart);
    return new JourneyState(EVehiclePhase.DEADHEAD_BEFORE, jss);
  }

  public static JourneyState layoverBefore() {
    return new JourneyState(EVehiclePhase.LAYOVER_BEFORE, null);
  }

  public static JourneyState inProgress() {
    return new JourneyState(EVehiclePhase.IN_PROGRESS, null);
  }

  public static JourneyState layoverDuring() {
    return new JourneyState(EVehiclePhase.LAYOVER_DURING, null);
  }

  public static JourneyState deadheadDuring(CoordinatePoint journeyStart) {
    JourneyStartState jss = new JourneyStartState(journeyStart);
    return new JourneyState(EVehiclePhase.DEADHEAD_DURING, jss);
  }

  public static JourneyState deadheadAfter() {
    return new JourneyState(EVehiclePhase.DEADHEAD_AFTER, null);
  }
  
  public static JourneyState layoverAfter() {
    return new JourneyState(EVehiclePhase.LAYOVER_AFTER, null);
  }
}
