package org.onebusaway.nyc.admin.util;

import org.onebusaway.nyc.admin.model.json.VehicleLastKnownRecord;
import org.onebusaway.nyc.admin.model.json.VehiclePullout;
import org.onebusaway.nyc.admin.model.ui.VehicleDetail;

public class VehicleDetailBuilder {

  public VehicleDetail buildVehicleDetail(VehiclePullout pullout,
      VehicleLastKnownRecord lastKnownRecord,
      String headSign,
      String inferredHeadSign) {
    VehicleDetail vehicleDetail = new VehicleDetail();
    vehicleDetail.setVehicleId(lastKnownRecord.getVehicleId());
    vehicleDetail.setLocation(lastKnownRecord.getLatitude() +", " + lastKnownRecord.getLongitude());
    vehicleDetail.setDirection(Math.abs(lastKnownRecord.getDirection()) % 360);
    vehicleDetail.setDepot(lastKnownRecord.getDepotId());
    vehicleDetail.setHeadSign(headSign);
    vehicleDetail.setInferredHeadSign(inferredHeadSign);
    return vehicleDetail;
  }

}
