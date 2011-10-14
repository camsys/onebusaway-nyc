package org.onebusaway.nyc.report_archive.services;

import org.onebusaway.nyc.report_archive.model.NycVehicleManagementStatusRecord;

public interface NycVehicleManagementStatusDao {
  void saveOrUpdateRecord(NycVehicleManagementStatusRecord record);
}
