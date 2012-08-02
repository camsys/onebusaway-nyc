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
    vehicleDetail.setLocation(lastKnownRecord.getLatitude() + ", " + lastKnownRecord.getLongitude());
    vehicleDetail.setDirection(Math.abs(lastKnownRecord.getDirection()) % 360);
    vehicleDetail.setDepot(lastKnownRecord.getDepotId());
    vehicleDetail.setHeadSign(headSign);
    vehicleDetail.setInferredHeadSign(inferredHeadSign);
    vehicleDetail.setServiceDate(lastKnownRecord.getServiceDate());
    vehicleDetail.setOperatorId(lastKnownRecord.getOperatorIdDesignator());
    vehicleDetail.setAgency(lastKnownRecord.getAgencyId());
    vehicleDetail.setObservedRunId(lastKnownRecord.getRouteIdDesignator() + "-" + lastKnownRecord.getRunIdDesignator());
    if (pullout != null) {
      vehicleDetail.setUtsRunId(pullout.getRun());
    }
    vehicleDetail.setInferredRunId(lastKnownRecord.getInferredRunId());
    if (lastKnownRecord.getScheduleDeviation() != null) {
      vehicleDetail.setScheduleDeviation(lastKnownRecord.getScheduleDeviation());
    }
    vehicleDetail.setTripId(lastKnownRecord.getInferredTripId());
    return vehicleDetail;
  }

}
