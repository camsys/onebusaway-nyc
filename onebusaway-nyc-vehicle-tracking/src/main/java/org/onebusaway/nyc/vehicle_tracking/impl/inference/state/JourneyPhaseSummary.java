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

import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;

public class JourneyPhaseSummary {

  private static DateFormat _timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);

  private static NumberFormat _ratioForamt = new DecimalFormat("0.00");

  private final EVehiclePhase phase;

  private final long timeFrom;

  private final long timeTo;

  private final BlockInstance blockInstance;

  private final double blockCompletionRatioFrom;

  private final double blockCompletionRatioTo;

  private JourneyPhaseSummary(Builder b) {
    this.phase = b.phase;
    this.timeFrom = b.timeFrom;
    this.timeTo = b.timeTo;
    this.blockInstance = b.blockInstance;
    this.blockCompletionRatioFrom = b.blockCompletionRatioFrom;
    this.blockCompletionRatioTo = b.blockCompletionRatioTo;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static Builder builder(JourneyPhaseSummary s) {
    return new Builder(s);
  }

  public EVehiclePhase getPhase() {
    return phase;
  }

  public long getTimeFrom() {
    return timeFrom;
  }

  public long getTimeTo() {
    return timeTo;
  }

  public BlockInstance getBlockInstance() {
    return blockInstance;
  }

  public double getBlockCompletionRatioFrom() {
    return blockCompletionRatioFrom;
  }

  public double getBlockCompletionRatioTo() {
    return blockCompletionRatioTo;
  }

  @Override
  public String toString() {
    return _timeFormat.format(new Date(timeFrom)) + "-"
        + _timeFormat.format(new Date(timeTo)) + " " + phase + " "
        + blockInstance + " " + _ratioForamt.format(blockCompletionRatioFrom)
        + "-" + _ratioForamt.format(blockCompletionRatioTo);
  }

  public static class Builder {

    private long timeFrom;

    private long timeTo;

    private EVehiclePhase phase;

    private BlockInstance blockInstance;

    private double blockCompletionRatioFrom;

    private double blockCompletionRatioTo;

    public Builder() {

    }

    public Builder(JourneyPhaseSummary s) {
      this.phase = s.phase;
      this.timeFrom = s.timeFrom;
      this.timeTo = s.timeTo;
      this.blockInstance = s.blockInstance;
      this.blockCompletionRatioFrom = s.blockCompletionRatioFrom;
      this.blockCompletionRatioTo = s.blockCompletionRatioTo;
    }

    public JourneyPhaseSummary create() {
      return new JourneyPhaseSummary(this);
    }

    public void setPhase(EVehiclePhase phase) {
      this.phase = phase;
    }

    public void setTimeFrom(long timeFrom) {
      this.timeFrom = timeFrom;
    }

    public void setTimeTo(long timeTo) {
      this.timeTo = timeTo;
    }

    public void setBlockInstance(BlockInstance blockInstance) {
      this.blockInstance = blockInstance;
    }

    public void setBlockCompletionRatioFrom(double blockCompletionRatioFrom) {
      this.blockCompletionRatioFrom = blockCompletionRatioFrom;
    }

    public void setBlockCompletionRatioTo(double blockCompletionRatioTo) {
      this.blockCompletionRatioTo = blockCompletionRatioTo;
    }
  }
}
