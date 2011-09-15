package org.onebusaway.nyc.transit_data_manager.importers;

import java.util.List;

import org.onebusaway.nyc.transit_data_manager.model.MtaBusDepotAssignment;
import org.onebusaway.nyc.transit_data_manager.model.xml.busAssignment.NewDataSet.Table;

public interface BusDepotAssignsInputConverter {
	List<MtaBusDepotAssignment> getBusDepotAssignments();
}
