package org.onebusaway.nyc.transit_data_manager.importers;

import java.util.List;

import org.onebusaway.nyc.transit_data_manager.model.MtaUtsCrewAssignment;

public interface CrewAssignsInputConverter 
{
    List<MtaUtsCrewAssignment> getCrewAssignments() ;
}
