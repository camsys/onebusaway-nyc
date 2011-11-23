package org.onebusaway.nyc.transit_data_manager.adapters.input.readers;

import java.io.FileNotFoundException;
import java.util.List;

import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaUtsCrewAssignment;

public interface CrewAssignsInputConverter {
  List<MtaUtsCrewAssignment> getCrewAssignments() throws FileNotFoundException;
}
