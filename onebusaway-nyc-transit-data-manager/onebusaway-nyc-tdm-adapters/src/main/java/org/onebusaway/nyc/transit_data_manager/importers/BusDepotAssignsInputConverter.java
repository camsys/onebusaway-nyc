package org.onebusaway.nyc.transit_data_manager.importers;

import java.util.List;

import org.onebusaway.nyc.transit_data_manager.model.MtaBusDepotAssignment;

public interface BusDepotAssignsInputConverter {
  List<MtaBusDepotAssignment> getBusDepotAssignments();
}
