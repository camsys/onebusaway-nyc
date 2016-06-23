package org.onebusaway.nyc.transit_data_manager.api.service;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;
import org.onebusaway.nyc.transit_data_manager.api.sourceData.VehiclePipoUploadsFilePicker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tcip_final_4_0_0.ObaSchPullOutList;
import tcip_final_4_0_0.SCHPullInOutInfo;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RealtimeVehiclePipoServiceImpl implements RealtimeVehiclePipoService {
	
	private static Logger log = LoggerFactory.getLogger(RealtimeVehiclePipoServiceImpl.class);
	private static String DATASOURCE_SYSTEM = "UTS";
	private DepotIdTranslator depotIdTranslator = null;
	
	@Override
	public ObaSchPullOutList readRealtimePulloutList(ObjectMapper m, DepotIdTranslator depotIdTranslator){
	//  throws IOException, JsonParseException, JsonMappingException
		
		this.depotIdTranslator = depotIdTranslator;
	
		// Read the current pullout list
		VehiclePipoUploadsFilePicker picker;
		try {
		  picker = new VehiclePipoUploadsFilePicker(
		      "tdm.vehiclepipoUploadDir", "RtSchPullinPulloutList_", ".json");
		} catch (IOException e) {
		  String message = "instantiating VehiclePipoUploadsFilePicker returned " + e.getMessage();
		  log.warn(message);
		  return createObaSchPullOutListWithError(message);
		}
		File file = picker.getMostRecentSourceFile();
		if (file == null) {
		  String message = "picker.getMostRecentSourceFile() returned null";
		  log.warn(message);
		  return createObaSchPullOutListWithError(message);
		}
		ObaSchPullOutList pulloutList;
		try {
		  pulloutList = (ObaSchPullOutList) m.readValue(file,
		      ObaSchPullOutList.class);
		  transformData(pulloutList);
		} catch (Exception e) {
		  String message = "parsing pullout list failed, exception: " + e.getMessage();
		  log.error(message);
		  return createObaSchPullOutListWithError(message);
		}
		return pulloutList;
	}
	
	private void transformData(ObaSchPullOutList pulloutList){
		for (SCHPullInOutInfo pullOut : pulloutList.getPullOuts().getPullOut()) {
			translateDepot(pullOut);
		}
	}
	
	private void translateDepot(SCHPullInOutInfo pullOut){
		if (depotIdTranslator != null) {
			pullOut.getGarage().setId(depotIdTranslator.getMappedId(DATASOURCE_SYSTEM, pullOut.getGarage().getId()));
		}
    }

	private ObaSchPullOutList createObaSchPullOutListWithError(String message) {
		ObaSchPullOutList o = new ObaSchPullOutList();
		o.setErrorCode("128");
		o.setErrorDescription(message);
		return o;
	}

}
