package org.onebusaway.nyc.vehicle_tracking.impl.sort;

import java.util.Comparator;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestLocationRecord;

public class NycTestLocationRecordVehicleComparator implements
    Comparator<NycTestLocationRecord> {

  @Override
  public int compare(NycTestLocationRecord o1, NycTestLocationRecord o2) {
    return new CompareToBuilder().append(o1.getVehicleId(), o2.getVehicleId()).toComparison();
  }
}
