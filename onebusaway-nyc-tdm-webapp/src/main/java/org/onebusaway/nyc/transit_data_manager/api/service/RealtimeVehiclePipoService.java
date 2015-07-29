package org.onebusaway.nyc.transit_data_manager.api.service;

import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;

import tcip_final_4_0_0.ObaSchPullOutList;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface RealtimeVehiclePipoService {
	
	ObaSchPullOutList readRealtimePulloutList(ObjectMapper m, DepotIdTranslator depotIdTranslator);
}
