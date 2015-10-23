/**
 * 
 */
package org.onebusaway.nyc.report_archive.impl;

import java.math.BigDecimal;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.queue.model.RealtimeEnvelope;
import org.onebusaway.nyc.report_archive.model.CcAndInferredLocationRecord;
import org.onebusaway.nyc.report_archive.services.RecordValidationService;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import tcip_final_3_0_5_1.CcLocationReport;

/**
 * Default implementation of {@link RecordValidationService}
 * @author abelsare
 *
 */
@Component
public class RecordValidationServiceImpl implements RecordValidationService {

	private static final Logger log = LoggerFactory.getLogger(RecordValidationServiceImpl.class);
	
	@Override
	public boolean validateInferenceRecord(NycQueuedInferredLocationBean inferredRecord) {
		boolean isValid = true;
		  
		  String id = inferredRecord.getVehicleId();
		  int index = id.indexOf('_');
		  
		  if(index > -1) {
			  String agency = id.substring(0, index);
			  String vehicleId = id.substring(index + 1);
			  
			  //Check vehicle and agency id
			  if(StringUtils.isBlank(vehicleId)) {
				  log.error("Missing vehicle id for inference record : {}", id);
				  isValid =  false;
			  }
			  if(StringUtils.isBlank(agency)) {
				  log.error("Missing agency id for inference record : {}", id);
				  isValid = false;
			  }
		  } else {
			  log.error("Cannot parse vehicle id for inference record : {}", id);
			  isValid =  false;
		  }
		  
		  //Check time reported and service date
		  if(inferredRecord.getRecordTimestamp() == null) {
			  log.error("Missing time reported for inference record : {}", id);
			  isValid =  false;
		  }
		  
		  if(inferredRecord.getServiceDate() == null) {
			  log.error("Missing service date for inference record : {}", id);
			  isValid = false;
		  }
		  
		  //Check UUID
		  if(StringUtils.isBlank(inferredRecord.getManagementRecord().getUUID())) {
			  log.error("Missing UUID for inference record : {}", id);
			  isValid =  false;
		  }
		  
		  //Check inferred latitude and inferred longitude
		  Double inferredLatitude = inferredRecord.getInferredLatitude();
		  Double inferredLongitude = inferredRecord.getInferredLongitude(); 
		  if((!isValueWithinRange(inferredLatitude, -999.999999, 999.999999)) ||
				  (!isValueWithinRange(inferredLongitude, -999.999999, 999.999999))) {
			  isValid =  false;
		  }

		  //Check inferred trip id
		  if(StringUtils.isNotBlank(inferredRecord.getTripId())) {
		    if (inferredRecord.getTripId().length() >= 64) {
		      log.error("Inferred trip id too long : {}", id);
		      isValid = false;
		    }
		  }

      //Check inferred block id
      if(StringUtils.isNotBlank(inferredRecord.getBlockId())) {
        if (inferredRecord.getBlockId().length() >= 64) {
          log.error("Inferred block id too long : {}", id);
          isValid = false;
        }
      }
		  
		  
		  return isValid;
	}

	
	@Override
	public boolean validateRealTimeRecord(RealtimeEnvelope realTimeRecord) {
		boolean isValid = true;
		CcLocationReport ccLocationReport = realTimeRecord.getCcLocationReport();
		long vehicleId = ccLocationReport.getVehicle().getVehicleId();
		
		if(ccLocationReport.getVehicle().getAgencyId() == null) {
			log.error("Missing agency id for real time record : {}", vehicleId);
			isValid = false;
		}

		//Check time reported
		if(StringUtils.isBlank(ccLocationReport.getTimeReported())) {
			log.error("Missing time reported for real time record : {}", vehicleId);
			isValid =  false;
		}

		//Check UUID
		if(StringUtils.isBlank(realTimeRecord.getUUID())) {
			log.error("Missing UUID for real time record : {}", vehicleId);
			isValid =  false;
		}

		//Check latitude and longitude
		BigDecimal latitude = convertMicrodegreesToDegrees(ccLocationReport.getLatitude());
		BigDecimal longitude = convertMicrodegreesToDegrees(ccLocationReport.getLongitude());
		
		if((!isValueWithinRange(latitude.doubleValue(), -999.999999, 999.999999)) ||
			(!isValueWithinRange(longitude.doubleValue(), -999.999999, 999.999999))) {
			isValid =  false;
		}
		
		BigDecimal speed = this.convertSpeed(ccLocationReport.getSpeed());
		if (!isValueWithinRange(speed.doubleValue(), -999.9, 999.9)) {
			isValid = false;
			log.error("Invalid speed for real time record : {} with value {}", vehicleId, ccLocationReport.getSpeed());
		}

		//Check direction degree
		BigDecimal directionDegree = ccLocationReport.getDirection().getDeg();
		if((!isValueWithinRange(directionDegree.doubleValue(), -999.99, 999.99)) ||
				(!isValueWithinRange(directionDegree.doubleValue(), -999.99, 999.99))) {
			log.error("Direction degree is either missing or out of range for real time record : {}", vehicleId);
			isValid =  false;
		}
		
		return isValid;
	}

	
	@Override
	public boolean validateLastKnownRecord(CcAndInferredLocationRecord record) {
	  boolean isValid = true;
	  
    //Check inferred latitude and inferred longitude
    BigDecimal inferredLatitude = record.getInferredLatitude();
    BigDecimal inferredLongitude = record.getInferredLongitude(); 
    if((!isValueWithinRange(inferredLatitude, -999.999999, 999.999999)) ||
        (!isValueWithinRange(inferredLongitude, -999.999999, 999.999999))) {
      isValid =  false;
    }

    //Check inferred trip id
    if(StringUtils.isNotBlank(record.getInferredTripId())) {
      if (record.getInferredTripId().length() >= 64) {
        log.error("Inferred trip id too long : {}", record.getVehicleId());
        isValid = false;
      }
    }

    //Check inferred block id
    if(StringUtils.isNotBlank(record.getInferredBlockId())) {
      if (record.getInferredBlockId().length() >= 64) {
        log.error("Inferred block id too long : {}", record.getVehicleId());
        isValid = false;
      }
    }
	  
	  return isValid;
	}
	
	private boolean isValueWithinRange(BigDecimal value, double lowerBound, double upperBound) {
    //Check for null and the valid range
    if(value == null) {
      return false;
    }
	
	  return isValueWithinRange(value.doubleValue(), lowerBound, upperBound);
  }


  @Override
	public boolean isValueWithinRange(Double value, double lowerBound, double upperBound) {
		//Check for null and the valid range
		if(value == null || Double.isNaN(value) || value < lowerBound || value > upperBound) {
			return false;
		}
		return true;
	}
	
	private BigDecimal convertMicrodegreesToDegrees(int latlong) {
		return new BigDecimal(latlong * Math.pow(10.0, -6));
	}
	
	// this comes from CcLocationReportRecord
	private BigDecimal convertSpeed(short saeSpeed) {
	    BigDecimal noOffsetSaeSpeed = new BigDecimal(saeSpeed - 30);

	    return noOffsetSaeSpeed.divide(new BigDecimal(2));
	  }
}
