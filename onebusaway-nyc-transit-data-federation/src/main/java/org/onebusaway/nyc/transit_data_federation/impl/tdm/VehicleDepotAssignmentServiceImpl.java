package org.onebusaway.nyc.transit_data_federation.impl.tdm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.impl.tdm.model.VehicleDepotAssignmentItem;
import org.onebusaway.nyc.transit_data_federation.services.tdm.VehicleDepotAssignmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class VehicleDepotAssignmentServiceImpl implements VehicleDepotAssignmentService {

	private static Logger _log = LoggerFactory.getLogger(VehicleDepotAssignmentServiceImpl.class);

	private Timer _updateTimer;

	private volatile HashMap<String, ArrayList<AgencyAndId>> _depotToVehicleListMap = 
			new HashMap<String, ArrayList<AgencyAndId>>();

	private class UpdateThread extends TimerTask {
		@Override
		public void run() {
			try {
				synchronized(_depotToVehicleListMap) {
					for(String depotId : _depotToVehicleListMap.keySet()) {
						@SuppressWarnings("unchecked")
						ArrayList<VehicleDepotAssignmentItem> vehicleAssignments = 
								(ArrayList<VehicleDepotAssignmentItem>)DataFetcherLibrary.getItemsForRequest("depot", depotId, "vehicles", "list");

						ArrayList<AgencyAndId> vehiclesForThisDepot = new ArrayList<AgencyAndId>();
						for(VehicleDepotAssignmentItem depotVehicleAssignment : vehicleAssignments) {
							vehiclesForThisDepot.add(new AgencyAndId(
									depotVehicleAssignment.agencyId, 
									depotVehicleAssignment.vehicleId));
						}
						
						_depotToVehicleListMap.put(depotId, vehiclesForThisDepot);
					}
					
				}
			} catch(Exception e) {
				_log.error("Error updating vehicle list");
				e.printStackTrace();
			}
		}		
	}

	public VehicleDepotAssignmentServiceImpl() {
		_updateTimer = new Timer();
		_updateTimer.schedule(new UpdateThread(), 0, 5 * 1000);
	}

	@Override
	public ArrayList<AgencyAndId> getVehiclesForDepot(String depotIdentifier) {
		return _depotToVehicleListMap.get(depotIdentifier);
	}
	
}
