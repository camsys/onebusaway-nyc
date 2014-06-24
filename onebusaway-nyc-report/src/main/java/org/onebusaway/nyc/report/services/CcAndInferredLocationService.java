package org.onebusaway.nyc.report.services;

import java.util.List;
import java.util.Map;

import org.onebusaway.nyc.report.impl.CcAndInferredLocationFilter;
import org.onebusaway.nyc.report.model.CcAndInferredLocationRecord;


public interface CcAndInferredLocationService {
	
  List<CcAndInferredLocationRecord> getAllLastKnownRecords(Map<CcAndInferredLocationFilter, String> filter) throws Exception;
	  
  CcAndInferredLocationRecord getLastKnownRecordForVehicle(Integer vehicleId) throws Exception;

}
